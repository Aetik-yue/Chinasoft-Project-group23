/**
 * 鹦鹉实时视觉识别 composable：连接后端 /ws/parrot，定时抓取"源画面"帧发送，
 * 接收检测框/行为/种类/异常并维护响应式状态，在独立 overlay canvas 上画框。
 *
 * 设计要点（为将来 3D 建模预留）：
 * - 帧来源解耦：通过 setFrameSource(fn) 注入"获取当前源 canvas"的函数。
 *   现在返回 videoCanvas（模拟/摄像头都画进它）；未来 3D 时改返回 3D canvas 即可，本 composable 不改。
 * - 叠加层 overlay canvas 独立于源 canvas，只画检测框，绝不污染源画面（3D canvas 不被改写）。
 * - 坐标 1:1：capture 用源 canvas 原生尺寸 toDataURL，后端返回的 box 即源坐标，overlay 同尺寸直配。
 */
import { onBeforeUnmount, ref } from 'vue'

const WS_URL = `ws://${import.meta.env.VITE_BACKEND_HOST || 'localhost'}:8080/ws/parrot`
const CAPTURE_INTERVAL = 300
const HEARTBEAT_INTERVAL = 30000
const RECONNECT_DELAY = 5000

export function useParrotVision() {
  const connected = ref(false)
  const detecting = ref(false)
  const boxes = ref([])
  const behavior = ref('')
  const behaviorConfidence = ref(0)
  const species = ref('')
  const speciesConfidence = ref(0)
  const abnormal = ref(null)
  const error = ref('')
  const fps = ref(0)

  let ws = null
  let captureTimer = 0
  let heartbeatTimer = 0
  let reconnectTimer = 0
  let shouldReconnect = false
  let frameSourceFn = null
  let overlayCanvas = null
  let overlayCtx = null
  let deviceId = 'default'
  let petId = ''
  let lastFpsTime = 0
  let fpsCount = 0

  /** 设置叠加层 canvas（独立于源画面，仅画检测框）。 */
  function setOverlay(canvasEl) {
    overlayCanvas = canvasEl || null
    overlayCtx = overlayCanvas ? overlayCanvas.getContext('2d') : null
  }

  /** 注入帧来源获取函数：返回当前用于识别的 canvas（模拟/摄像头/未来3D）。 */
  function setFrameSource(fn) {
    frameSourceFn = typeof fn === 'function' ? fn : null
  }

  function setDeviceId(id) {
    deviceId = id || 'default'
  }

  function setPetId(id) {
    petId = id || ''
  }

  /** 启动识别：连 WS + 开始定时抓帧。 */
  function start() {
    if (detecting.value) return
    detecting.value = true
    shouldReconnect = true
    error.value = ''
    connect()
    captureTimer = window.setInterval(captureAndSend, CAPTURE_INTERVAL)
    lastFpsTime = performance.now()
  }

  /** 停止识别：关 WS、清定时器、清状态与叠加层。 */
  function stop() {
    detecting.value = false
    shouldReconnect = false
    window.clearInterval(captureTimer)
    captureTimer = 0
    window.clearTimeout(reconnectTimer)
    reconnectTimer = 0
    window.clearInterval(heartbeatTimer)
    heartbeatTimer = 0
    if (ws && ws.readyState <= WebSocket.OPEN) {
      ws.close()
    }
    ws = null
    connected.value = false
    boxes.value = []
    behavior.value = ''
    behaviorConfidence.value = 0
    species.value = ''
    speciesConfidence.value = 0
    abnormal.value = null
    clearOverlay()
  }

  function connect() {
    if (!shouldReconnect || typeof WebSocket === 'undefined') return
    window.clearTimeout(reconnectTimer)
    try {
      ws = new WebSocket(WS_URL)
    } catch {
      reconnectTimer = window.setTimeout(connect, RECONNECT_DELAY)
      return
    }
    ws.addEventListener('open', () => {
      connected.value = true
      error.value = ''
      window.clearInterval(heartbeatTimer)
      heartbeatTimer = window.setInterval(() => {
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send('{"type":"ping"}')
        }
      }, HEARTBEAT_INTERVAL)
    })
    ws.addEventListener('message', (event) => {
      try {
        handleMessage(JSON.parse(event.data))
      } catch {
        // 忽略无法解析的帧（如心跳/调试）
      }
    })
    ws.addEventListener('close', () => {
      connected.value = false
      window.clearInterval(heartbeatTimer)
      if (shouldReconnect) {
        reconnectTimer = window.setTimeout(connect, RECONNECT_DELAY)
      }
    })
    ws.addEventListener('error', () => {
      error.value = '视觉识别服务连接失败'
      // close 回调会负责重连
    })
  }

  function captureAndSend() {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    const src = frameSourceFn && frameSourceFn()
    if (!src) return
    let dataUrl = null
    try {
      dataUrl = src.toDataURL ? src.toDataURL('image/jpeg', 0.5) : null
    } catch {
      // 源 canvas 被污染或不可用时跳过本帧
      return
    }
    if (!dataUrl) return
    const token = localStorage.getItem('parrotAuthToken') || ''
    ws.send(JSON.stringify({ image: dataUrl, deviceId, petId, token }))
    fpsCount++
    const now = performance.now()
    if (now - lastFpsTime >= 1000) {
      fps.value = fpsCount
      fpsCount = 0
      lastFpsTime = now
    }
  }

  function handleMessage(data) {
    if (!data) return
    if (data.error) {
      error.value = data.error
      return
    }
    error.value = ''
    boxes.value = Array.isArray(data.boxes) ? data.boxes : []
    behavior.value = data.behavior || ''
    behaviorConfidence.value = Number(data.behaviorConfidence) || 0
    species.value = data.species || ''
    speciesConfidence.value = Number(data.speciesConfidence) || 0
    abnormal.value = Array.isArray(data.abnormal) && data.abnormal[0] ? data.abnormal[0] : null
    drawOverlay()
  }

  function drawOverlay() {
    if (!overlayCtx || !overlayCanvas) return
    const w = overlayCanvas.width
    const h = overlayCanvas.height
    overlayCtx.clearRect(0, 0, w, h)
    // 种类/行为由后端 CLIP 给出（响应顶层，约每 2.5s 更新）；未出结果时回退“鹦鹉”
    const sp = species.value || '鹦鹉'
    const beh = behavior.value || ''
    boxes.value.forEach((b) => {
      const conf = Number(b.confidence) || 0
      const color = conf >= 0.8 ? '#2ed573' : conf >= 0.5 ? '#ffa502' : '#ff4757'
      overlayCtx.save()
      overlayCtx.strokeStyle = color
      overlayCtx.lineWidth = 3
      overlayCtx.strokeRect(b.x, b.y, b.width, b.height)
      overlayCtx.fillStyle = color
      overlayCtx.font = 'bold 13px sans-serif'
      const pct = `${Math.round(conf * 100)}%`
      const label = beh ? `${sp} · ${beh} ${pct}` : `${sp} ${pct}`
      overlayCtx.fillText(label, b.x, Math.max(12, b.y - 6))
      overlayCtx.restore()
    })
  }

  function clearOverlay() {
    if (overlayCtx && overlayCanvas) {
      overlayCtx.clearRect(0, 0, overlayCanvas.width, overlayCanvas.height)
    }
  }

  onBeforeUnmount(stop)

  return {
    connected,
    detecting,
    boxes,
    behavior,
    behaviorConfidence,
    species,
    speciesConfidence,
    abnormal,
    error,
    fps,
    start,
    stop,
    setOverlay,
    setFrameSource,
    setDeviceId,
    setPetId,
  }
}
