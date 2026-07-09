<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import ParrotVisual from './ParrotVisual.vue'
import { getRealtimeSmoke } from '../api/smoke'
import { getAlarmLogs } from '../api/alarm'

const props = defineProps({
  card: {
    type: Object,
    required: true,
  },
  // 当前鹦鹉关联的烟感设备 ID，用于拉取该设备的实时数据与告警（pet ↔ device 概念闭环）
  deviceId: {
    type: String,
    default: '',
  },
  parrotId: {
    type: String,
    default: '',
  },
  locale: {
    type: String,
    default: 'zh',
  },
})

const emit = defineEmits(['open', 'dust-detail', 'metric-update', 'snapshot-captured', 'fullscreen-change', 'alarm-notify'])

const isLiveMode = ref(true)
const isFullscreen = ref(false)
const micEnabled = ref(true)
const volume = ref(62)
const currentTime = ref('')
const videoCanvas = ref(null)
const monitorCard = ref(null)
const savedShots = ref([])
const captureFlash = ref(false)
const saveToastVisible = ref(false)
// 实时状态由 /api/smoke/realtime 驱动，覆盖 card 上的 mock 值
const online = ref(!!props.card.online)
const statusLabel = ref(props.card.statusLabel || '当前状态：--')
const realtimeError = ref('')
const sensorSnapshot = ref({
  humidity: 58,
  temperature: 26.3,
  dustValue: 18,
  dustUnit: 'μg/m³',
  dustLevel: '低',
  riskScore: 18,
  connected: false,
  updateTime: '',
})
const alarmState = ref({
  humidity: false,
  temperature: false,
  dust: false,
})
const alarmNotified = ref({
  humidity: false,
  temperature: false,
  dust: false,
})

let timeTimer = 0
let animationFrame = 0
let animationStarted = false
let realtimeTimer = 0
let alarmSocket = null
let alarmHeartbeatTimer = 0
let alarmReconnectTimer = 0
let shouldReconnectAlarmSocket = true
let captureFlashTimer = 0
let saveToastTimer = 0

const MONITOR_COPY = {
  zh: {
    humidity: '湿度', temperature: '温度', dust: '粉尘浓度',
    low: '低', mid: '中', high: '高', suitable: '适宜', lowState: '偏低', highState: '偏高', pending: '待接入',
    alarm: '告警中', currentStatus: '当前状态', failed: '获取失败', saved: '图像已保存', weeklyRecords: '近一周记录',
    risk: { normal: '正常', low: '低风险', medium: '中风险', high: '高风险' },
  },
  en: {
    humidity: 'Humidity', temperature: 'Temperature', dust: 'Dust',
    low: 'Low', mid: 'Medium', high: 'High', suitable: 'Good', lowState: 'Low', highState: 'High', pending: 'Pending',
    alarm: 'Alarm', currentStatus: 'Status', failed: 'Failed', saved: 'Image saved', weeklyRecords: 'Last 7 days',
    risk: { normal: 'Normal', low: 'Low risk', medium: 'Medium risk', high: 'High risk' },
  },
  es: {
    humidity: 'Humedad', temperature: 'Temperatura', dust: 'Polvo',
    low: 'Bajo', mid: 'Medio', high: 'Alto', suitable: 'Adecuado', lowState: 'Bajo', highState: 'Alto', pending: 'Pendiente',
    alarm: 'Alarma', currentStatus: 'Estado', failed: 'Error', saved: 'Imagen guardada', weeklyRecords: 'Últimos 7 días',
    risk: { normal: 'Normal', low: 'Riesgo bajo', medium: 'Riesgo medio', high: 'Riesgo alto' },
  },
  ja: {
    humidity: '湿度', temperature: '温度', dust: '粉じん濃度',
    low: '低', mid: '中', high: '高', suitable: '適切', lowState: '低め', highState: '高め', pending: '未接続',
    alarm: '警報中', currentStatus: '現在状態', failed: '取得失敗', saved: '画像を保存しました', weeklyRecords: '直近7日',
    risk: { normal: '正常', low: '低リスク', medium: '中リスク', high: '高リスク' },
  },
}
const monitorText = computed(() => MONITOR_COPY[props.locale] || MONITOR_COPY.zh)
const ALARM_SOCKET_URL = 'ws://localhost:8080/ws/alarm'
const ALARM_THRESHOLDS = {
  humidityHigh: 70,
  temperatureHigh: 30,
  dustMedium: 35,
}

const environment = computed(() => [
  {
    key: 'humidity',
    label: monitorText.value.humidity,
    value: sensorSnapshot.value.humidity,
    displayValue: `${formatNumber(sensorSnapshot.value.humidity, 0)}%`,
    unit: '%',
    level: getHumidityLevel(sensorSnapshot.value.humidity),
    alarming: alarmState.value.humidity,
    gaugeMax: 100,
    route: '/api/smoke/realtime#humidity',
  },
  {
    key: 'temperature',
    label: monitorText.value.temperature,
    value: sensorSnapshot.value.temperature,
    displayValue: `${formatNumber(sensorSnapshot.value.temperature, 1)}℃`,
    unit: '℃',
    level: getTemperatureLevel(sensorSnapshot.value.temperature),
    alarming: alarmState.value.temperature,
    gaugeMax: 45,
    route: '/api/smoke/realtime#temperature',
  },
  {
    key: 'dust',
    label: monitorText.value.dust,
    value: sensorSnapshot.value.dustValue,
    displayValue: `${sensorSnapshot.value.dustValue}${sensorSnapshot.value.dustUnit}`,
    unit: sensorSnapshot.value.dustUnit,
    level: sensorSnapshot.value.dustLevel,
    alarming: alarmState.value.dust,
    gaugeMax: 120,
    route: '/api/smoke/realtime#smokeValue',
  },
])

// 每 0.5 秒拉取一次实时烟雾数据，驱动环境指标与风险状态。
async function refreshRealtime() {
  try {
    const data = await getRealtimeSmoke(props.deviceId)
    if (realtimeError.value || !sensorSnapshot.value.updateTime) {
      console.info('[MonitorCard] 实时数据已恢复，deviceId=', props.deviceId, 'temperature=', data?.temperature, 'humidity=', data?.humidity)
    }
    realtimeError.value = ''
    online.value = !!data?.connected
    sensorSnapshot.value = normalizeSensorPayload(data)
    syncThresholdAlarms(data)
    emitMetricUpdates()
    const riskText = monitorText.value.risk[data?.riskLevel] || data?.riskLevel || '--'
    const alarmText = data?.alarmStatus === 'alarm' ? monitorText.value.alarm : ''
    statusLabel.value = `${monitorText.value.currentStatus}：${alarmText || riskText}`
  } catch (e) {
    realtimeError.value = e.message
    online.value = false
    statusLabel.value = `${monitorText.value.currentStatus}：${monitorText.value.failed}`
    console.warn('[MonitorCard] 实时数据拉取失败：', e.message, 'deviceId=', props.deviceId)
  }
}

const volumeStyle = computed(() => ({
  background: `linear-gradient(90deg, #f2b66e 0 ${volume.value}%, rgba(255,255,255,.5) ${volume.value}% 100%)`,
}))

function formatTime(date = new Date()) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

function updateTime() {
  currentTime.value = formatTime()
}

function formatNumber(value, digits = 0) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '--'
  return number.toFixed(digits).replace(/\.0$/, '')
}

function getDustLevel(value, fallback = '') {
  const normalized = String(fallback || '').toLowerCase()
  if (normalized.includes('high') || fallback === '高') return monitorText.value.high
  if (normalized.includes('medium') || normalized.includes('middle') || fallback === '中') return monitorText.value.mid
  const number = Number(value)
  if (!Number.isFinite(number)) return monitorText.value.low
  if (number >= 80) return monitorText.value.high
  if (number >= 35) return monitorText.value.mid
  return monitorText.value.low
}

function getTemperatureLevel(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return monitorText.value.pending
  if (number < 18) return monitorText.value.lowState
  if (number > 30) return monitorText.value.highState
  return monitorText.value.suitable
}

function getHumidityLevel(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return monitorText.value.pending
  if (number < 40) return monitorText.value.lowState
  if (number > 70) return monitorText.value.highState
  return monitorText.value.suitable
}

function normalizeSensorPayload(payload) {
  const data = payload?.data || payload || {}
  const dustValue = Number(data.smokeValue ?? data.dustValue ?? sensorSnapshot.value.dustValue)
  const temperature = Number(data.temperature ?? sensorSnapshot.value.temperature)
  const humidity = Number(data.humidity ?? sensorSnapshot.value.humidity)
  return {
    humidity: Number.isFinite(humidity) ? humidity : sensorSnapshot.value.humidity,
    temperature: Number.isFinite(temperature) ? temperature : sensorSnapshot.value.temperature,
    dustValue: Number.isFinite(dustValue) ? dustValue : sensorSnapshot.value.dustValue,
    dustUnit: data.unit || sensorSnapshot.value.dustUnit,
    dustLevel: getDustLevel(dustValue, data.riskLevel),
    riskScore: Number(data.riskScore ?? dustValue ?? sensorSnapshot.value.riskScore),
    connected: Boolean(data.connected),
    updateTime: data.updateTime || new Date().toISOString(),
  }
}

function alarmMessage(metric) {
  const labels = {
    humidity: monitorText.value.humidity,
    temperature: monitorText.value.temperature,
    dust: monitorText.value.dust,
  }
  const suffix = { zh: '异常', en: ' abnormal', es: ' anormal', ja: '異常' }[props.locale] || '异常'
  return `${labels[metric] || '环境'}${suffix}`
}

function emitAlarmNotice(metric, value, raw = {}) {
  emit('alarm-notify', {
    metric,
    label: alarmMessage(metric).replace('异常', ''),
    message: alarmMessage(metric),
    value,
    alarmId: raw.alarmId || '',
    deviceId: raw.deviceId || props.deviceId,
    level: raw.level || '',
    alarmTime: raw.alarmTime || new Date().toISOString(),
  })
}

function isHighDustLevel(value, level = '') {
  const normalized = String(level || '').toLowerCase()
  return (
    normalized.includes('medium')
    || normalized.includes('high')
    || level === '中'
    || level === '高'
    || level === monitorText.value.mid
    || level === monitorText.value.high
    || Number(value) >= ALARM_THRESHOLDS.dustMedium
  )
}

function syncThresholdAlarms(raw = {}) {
  const data = raw?.data || raw || {}
  const nextState = {
    humidity: Number(sensorSnapshot.value.humidity) > ALARM_THRESHOLDS.humidityHigh,
    temperature: Number(sensorSnapshot.value.temperature) > ALARM_THRESHOLDS.temperatureHigh,
    dust: data.alarmStatus === 'alarm' || isHighDustLevel(sensorSnapshot.value.dustValue, sensorSnapshot.value.dustLevel),
  }

  Object.entries(nextState).forEach(([metric, alarming]) => {
    if (alarming && !alarmState.value[metric] && !alarmNotified.value[metric]) {
      emitAlarmNotice(metric, sensorSnapshot.value[metric === 'dust' ? 'dustValue' : metric], data)
      alarmNotified.value = { ...alarmNotified.value, [metric]: true }
    }
    if (!alarming && alarmState.value[metric]) {
      alarmNotified.value = { ...alarmNotified.value, [metric]: false }
    }
  })

  alarmState.value = nextState
}

function handleSocketAlarm(payload) {
  if (payload?.type !== 'alarm') return
  const value = Number(payload.smokeValue ?? payload.dustValue)
  sensorSnapshot.value = {
    ...sensorSnapshot.value,
    dustValue: Number.isFinite(value) ? value : sensorSnapshot.value.dustValue,
    dustUnit: payload.unit || 'ppm',
    dustLevel: payload.level === 'high' ? monitorText.value.high : monitorText.value.mid,
    connected: true,
    updateTime: payload.alarmTime || new Date().toISOString(),
  }
  alarmState.value = { ...alarmState.value, dust: true }
  alarmNotified.value = { ...alarmNotified.value, dust: true }
  emitAlarmNotice('dust', sensorSnapshot.value.dustValue, payload)
  emitMetricUpdates()
}

function connectAlarmSocket() {
  if (!shouldReconnectAlarmSocket || typeof WebSocket === 'undefined') return
  window.clearTimeout(alarmReconnectTimer)
  window.clearInterval(alarmHeartbeatTimer)

  try {
    alarmSocket = new WebSocket(ALARM_SOCKET_URL)
  } catch {
    alarmReconnectTimer = window.setTimeout(connectAlarmSocket, 5000)
    return
  }

  alarmSocket.addEventListener('open', () => {
    alarmHeartbeatTimer = window.setInterval(() => {
      if (alarmSocket?.readyState === WebSocket.OPEN) {
        alarmSocket.send('{"type":"ping"}')
      }
    }, 30000)
  })

  alarmSocket.addEventListener('message', (event) => {
    try {
      handleSocketAlarm(JSON.parse(event.data))
    } catch {
      // Ignore malformed heartbeat or debug frames.
    }
  })

  alarmSocket.addEventListener('close', () => {
    window.clearInterval(alarmHeartbeatTimer)
    if (shouldReconnectAlarmSocket) {
      alarmReconnectTimer = window.setTimeout(connectAlarmSocket, 5000)
    }
  })
}

function closeAlarmSocket() {
  shouldReconnectAlarmSocket = false
  window.clearTimeout(alarmReconnectTimer)
  window.clearInterval(alarmHeartbeatTimer)
  if (alarmSocket && alarmSocket.readyState <= WebSocket.OPEN) {
    alarmSocket.close()
  }
  alarmSocket = null
}

async function refreshSensorSnapshot() {
  try {
    sensorSnapshot.value = normalizeSensorPayload(await getRealtimeSmoke(props.deviceId))
    syncThresholdAlarms()
  } catch {
    const drift = Math.round(Math.sin(Date.now() / 9000) * 4)
    const fallbackValue = Math.max(8, sensorSnapshot.value.dustValue + drift)
    sensorSnapshot.value = {
      ...sensorSnapshot.value,
      dustValue: fallbackValue,
      dustLevel: getDustLevel(fallbackValue),
      connected: false,
      updateTime: new Date().toISOString(),
    }
    syncThresholdAlarms()
  }
}

function metricNeedleRotation(item) {
  const number = Number(item.value)
  const max = Number(item.gaugeMax || 100)
  const ratio = Number.isFinite(number) && max > 0 ? Math.min(1, Math.max(0, number / max)) : 0
  return `${-90 + ratio * 180}deg`
}

function metricPayload(item) {
  return {
    metric: item.key,
    label: item.label,
    value: item.value,
    displayValue: item.displayValue,
    unit: item.unit,
    level: item.level,
    gaugeMax: item.gaugeMax,
    connected: sensorSnapshot.value.connected,
    updateTime: sensorSnapshot.value.updateTime,
    dustValue: item.key === 'dust' ? item.value : sensorSnapshot.value.dustValue,
    dustUnit: item.key === 'dust' ? item.unit : sensorSnapshot.value.dustUnit,
    dustLevel: item.key === 'dust' ? item.level : sensorSnapshot.value.dustLevel,
  }
}

function emitMetricUpdates() {
  emit('metric-update', environment.value.map((item) => metricPayload(item)))
}

function openMetricDetail(item) {
  emit('dust-detail', metricPayload(item))
}

function enterLiveMode() {
  if (isLiveMode.value) return
  isLiveMode.value = true
  emit('open', props.card)
  nextTick(() => {
    startMockVideoStream()
  })
}

function exitLiveMode() {
  isLiveMode.value = false
  stopMockVideoStream()
}

function toggleMic() {
  micEnabled.value = !micEnabled.value
  // TODO: connect to backend/WebRTC audio track mute API.
}

function changeVolume(delta) {
  volume.value = Math.min(100, Math.max(0, volume.value + delta))
  // TODO: connect to live stream audio output volume API.
}

function showCaptureFeedback() {
  window.clearTimeout(captureFlashTimer)
  window.clearTimeout(saveToastTimer)
  captureFlash.value = false
  saveToastVisible.value = false

  requestAnimationFrame(() => {
    captureFlash.value = true
    saveToastVisible.value = true
    captureFlashTimer = window.setTimeout(() => {
      captureFlash.value = false
    }, 260)
    saveToastTimer = window.setTimeout(() => {
      saveToastVisible.value = false
    }, 1000)
  })
}

function captureCurrentFrame() {
  const canvas = videoCanvas.value
  if (!canvas) return

  // JPEG 压缩 + 缩小尺寸，控制 base64 体积（单张约 20~30KB，避免 localStorage 爆容量）
  const snapshot = {
    id: `shot-${Date.now()}`,
    parrotId: props.parrotId || 'sun-001',
    source: props.card.route,
    savedAt: new Date().toISOString(),
    image: compressCanvasToJpeg(canvas, 0.6, 480),
  }

  // 本地仅缓存少量截图供离线查看；持久化由父应用上传后端
  const nextShots = [snapshot, ...savedShots.value].slice(0, 12)
  savedShots.value = nextShots
  try {
    localStorage.setItem('parrotArchiveSnapshots', JSON.stringify(nextShots))
  } catch {
    // 本地缓存写不下了就跳过，主数据在后端
  }
  emit('snapshot-captured', snapshot)
  showCaptureFeedback()
  // The parent app persists the snapshot after local capture succeeds.
}

// 把 canvas 压缩成 JPEG base64：等比缩小到 maxWidth 以内，quality 为 0~1
function compressCanvasToJpeg(sourceCanvas, quality = 0.6, maxWidth = 480) {
  const scale = Math.min(1, maxWidth / sourceCanvas.width)
  const target = document.createElement('canvas')
  target.width = Math.max(1, Math.round(sourceCanvas.width * scale))
  target.height = Math.max(1, Math.round(sourceCanvas.height * scale))
  target.getContext('2d').drawImage(sourceCanvas, 0, 0, target.width, target.height)
  return target.toDataURL('image/jpeg', quality)
}

async function openWeeklyRecords() {
  // 先完成浏览器全屏退出，再切换到周报页面，避免组件卸载后丢失 fullscreenchange 事件。
  if (document.fullscreenElement === monitorCard.value) {
    try {
      await document.exitFullscreen?.()
    } catch (error) {
      console.warn('[monitor] 退出全屏失败', error)
    }
  }
  isFullscreen.value = false
  emit('fullscreen-change', false)

  emit('open', { ...props.card, route: '/monitor/records?range=7d' })
  try {
    const data = await getAlarmLogs({ limit: 50, deviceId: props.deviceId })
    // 后端可能返回分页对象 { list, total } 或直接数组，兼容两种
    const list = Array.isArray(data) ? data : data?.list || []
    console.info('[alarm-logs] 近期告警', list)
    // TODO: 渲染告警记录列表 UI（当前先打日志，后续可弹层展示）
  } catch (e) {
    console.warn('[alarm-logs] 获取失败', e.message)
  }
}

async function toggleFullscreen() {
  const target = monitorCard.value
  if (!target) return

  if (!document.fullscreenElement) {
    await target.requestFullscreen?.()
    isFullscreen.value = true
    return
  }

  await document.exitFullscreen?.()
  isFullscreen.value = false
}

function drawMockVideoFrame(time = 0) {
  const canvas = videoCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height
  const sway = Math.sin(time / 650)
  const blink = Math.sin(time / 900) > .92

  ctx.clearRect(0, 0, width, height)

  const bg = ctx.createLinearGradient(0, 0, width, height)
  bg.addColorStop(0, '#fff2df')
  bg.addColorStop(.56, '#f9dcc2')
  bg.addColorStop(1, '#d8ebf2')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, width, height)

  ctx.fillStyle = 'rgba(255,255,255,.38)'
  for (let x = 0; x < width; x += 52) {
    ctx.fillRect(x, 0, 2, height)
  }

  ctx.strokeStyle = '#ba814d'
  ctx.lineWidth = 18
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(54, height * .7)
  ctx.lineTo(width - 38, height * .66)
  ctx.stroke()

  ctx.strokeStyle = '#8d6038'
  ctx.lineWidth = 10
  ctx.beginPath()
  ctx.moveTo(width - 125, 70)
  ctx.lineTo(width - 78, height * .66)
  ctx.stroke()

  ctx.fillStyle = '#74a64a'
  ctx.save()
  ctx.translate(width - 150, 98)
  ctx.rotate(-.65)
  ctx.ellipse(0, 0, 16, 32, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()

  ctx.save()
  ctx.translate(width * .48 + sway * 10, height * .51)
  ctx.rotate(sway * .05)

  ctx.fillStyle = '#f4782c'
  ctx.beginPath()
  ctx.ellipse(0, 0, 82, 54, -.12, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#f8f6ef'
  ctx.beginPath()
  ctx.ellipse(-45, -21, 52, 39, -.18, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#222222'
  ctx.beginPath()
  ctx.ellipse(-78, 8, 36, 52, .58, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#ef6f25'
  ctx.beginPath()
  ctx.ellipse(96, 18, 91, 28, .25, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#ffffff'
  ctx.beginPath()
  ctx.arc(-29, -37, 28, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#242424'
  ctx.beginPath()
  ctx.arc(-16, -43, 6, 0, Math.PI * 2)
  ctx.fill()
  if (!blink) {
    ctx.fillStyle = '#ffffff'
    ctx.beginPath()
    ctx.arc(-14, -45, 2, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.strokeStyle = '#85562f'
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.moveTo(-28, 52)
  ctx.lineTo(-34, 86)
  ctx.moveTo(16, 51)
  ctx.lineTo(28, 84)
  ctx.stroke()

  ctx.restore()

  ctx.fillStyle = 'rgba(54, 35, 21, .38)'
  ctx.beginPath()
  ctx.ellipse(width * .5, height - 38, 122, 18, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = 'rgba(255,255,255,.72)'
  ctx.font = '700 18px Microsoft YaHei, sans-serif'
  ctx.fillText('Mock Live Stream', 22, height - 24)

  animationFrame = requestAnimationFrame(drawMockVideoFrame)
}

function startMockVideoStream() {
  if (animationStarted) return
  animationStarted = true
  drawMockVideoFrame()
  // TODO: replace mock canvas with WebRTC/HLS/MJPEG live stream.
}

function stopMockVideoStream() {
  if (animationFrame) cancelAnimationFrame(animationFrame)
  animationStarted = false
}

function handleFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === monitorCard.value
  emit('fullscreen-change', isFullscreen.value)
}

onMounted(() => {
  updateTime()
  timeTimer = window.setInterval(updateTime, 1000)
  document.addEventListener('fullscreenchange', handleFullscreenChange)

  // 实时烟雾数据：启动即拉一次，之后每 0.5 秒轮询（与 API 文档轮询节奏一致）
  refreshRealtime()
  realtimeTimer = window.setInterval(refreshRealtime, 500)
  connectAlarmSocket()
  nextTick(() => {
    startMockVideoStream()
  })

  const storedShots = localStorage.getItem('parrotArchiveSnapshots')
  if (storedShots) {
    try {
      savedShots.value = JSON.parse(storedShots)
    } catch {
      savedShots.value = []
    }
  }
})

onBeforeUnmount(() => {
  window.clearInterval(timeTimer)
  window.clearInterval(realtimeTimer)
  window.clearTimeout(captureFlashTimer)
  window.clearTimeout(saveToastTimer)
  closeAlarmSocket()
  // 页面切换可能先卸载组件、后触发浏览器的 fullscreenchange，因此在卸载时主动清理父级状态。
  emit('fullscreen-change', false)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  stopMockVideoStream()
})
</script>

<template>
  <section
    ref="monitorCard"
    class="monitor-card"
    :class="{ 'live-mode': isLiveMode, 'is-fullscreen': isFullscreen }"
    :data-route="card.route"
  >
    <header v-if="!isLiveMode" class="monitor-header">
      <div class="monitor-title-wrap">
        <h2>{{ card.title }}</h2>
        <span
          class="connection-status"
          :class="online ? 'connection-online' : 'connection-offline'"
          :aria-label="online ? '在线' : '离线'"
          :title="online ? '已连接后端实时数据' : '与后端断开，显示缓存/模拟值'"
        >
          <svg class="wifi-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path class="wifi-arc-outer" d="M2.7 8.7a15.3 15.3 0 0 1 18.6 0" />
            <path class="wifi-arc-middle" d="M6.1 12.1a10.5 10.5 0 0 1 11.8 0" />
            <path class="wifi-arc-inner" d="M9.5 15.5a5.7 5.7 0 0 1 5 0" />
            <circle class="wifi-dot" cx="12" cy="19" r="1.6" />
            <path v-if="!online" class="wifi-slash" d="M2.5 3l19 19" />
          </svg>
        </span>
      </div>
      <button class="expand-button" type="button" aria-label="打开实时监控" @click="enterLiveMode">
        <span></span>
      </button>
    </header>

    <button
      v-if="!isLiveMode"
      class="monitor-scene"
      type="button"
      aria-label="查看实时监控与状态"
      @click="enterLiveMode"
    >
      <span class="wood-house" aria-hidden="true">
        <span></span>
      </span>
      <span class="hanging-bell" aria-hidden="true"></span>
      <span class="branch branch-main" aria-hidden="true"></span>
      <span class="branch branch-leaf-1" aria-hidden="true"></span>
      <span class="branch branch-leaf-2" aria-hidden="true"></span>
      <span class="perch" aria-hidden="true"></span>
      <span class="food-bowl" aria-hidden="true"></span>
      <ParrotVisual type="main-orange" />
      <span class="toy-ball" aria-hidden="true"></span>
      <span class="status-pill">
        <span class="mini-bird" aria-hidden="true"></span>
        {{ statusLabel }}
      </span>
      <span class="status-rays" aria-hidden="true"></span>
    </button>

    <div v-else class="live-monitor-panel" aria-label="实时视频监控模式">
      <transition name="capture-save-toast">
        <div v-if="saveToastVisible" class="capture-save-toast" role="status">
          {{ monitorText.saved }}
        </div>
      </transition>
      <div class="live-topbar">
        <button class="live-back-button" type="button" aria-label="返回实时通话卡片" @click="exitLiveMode">
          <span aria-hidden="true"></span>
        </button>
        <time class="live-clock">{{ currentTime }}</time>
      </div>

      <div class="live-content">
        <aside class="environment-stack" aria-label="环境指标">
          <button
            v-for="item in environment"
            :key="item.key"
            class="environment-item"
            :class="{ 'environment-button': item.interactive, 'is-alarming': item.alarming }"
            type="button"
            :data-api="item.route"
            @click="openMetricDetail(item)"
          >
            <span class="inline-gauge-arc" aria-hidden="true">
              <i :style="{ transform: metricNeedleRotation(item) }"></i>
            </span>
            <span>{{ item.label }}</span>
            <strong>{{ item.displayValue }}</strong>
            <em>{{ item.alarming ? monitorText.alarm : item.level }}</em>
          </button>
        </aside>

        <div class="video-frame" :class="{ 'is-capturing': captureFlash }">
          <canvas ref="videoCanvas" width="720" height="430" aria-label="实时视频画面"></canvas>
          <div class="video-live-badge">
            <span></span>
            LIVE
          </div>
        </div>

        <aside class="right-live-tools" aria-label="监控工具">
          <button class="capture-button" type="button" aria-label="截图并保存到宠物档案" @click="captureCurrentFrame">
            <span class="camera-icon" aria-hidden="true"></span>
          </button>
          <button class="records-link" type="button" @click="openWeeklyRecords">
            {{ monitorText.weeklyRecords }}
          </button>
        </aside>
      </div>

      <div class="live-controls">
        <button
          class="round-control mic-control"
          type="button"
          :class="{ muted: !micEnabled }"
          :aria-label="micEnabled ? '关闭麦克风' : '打开麦克风'"
          @click="toggleMic"
        >
          <span aria-hidden="true"></span>
        </button>
        <button class="round-control volume-down" type="button" aria-label="音量减小" @click="changeVolume(-8)">
          <span aria-hidden="true"></span>
        </button>
        <div class="volume-group" aria-label="音量大小">
          <span>{{ volume }}</span>
          <input v-model.number="volume" type="range" min="0" max="100" :style="volumeStyle" />
        </div>
        <button class="round-control volume-up" type="button" aria-label="音量增大" @click="changeVolume(8)">
          <span aria-hidden="true"></span>
        </button>
        <button class="fullscreen-control" type="button" :aria-label="isFullscreen ? '缩小监控画面' : '放大监控画面'" @click="toggleFullscreen">
          <span aria-hidden="true"></span>
        </button>
      </div>
    </div>

    <!-- 浏览器全屏只显示全屏元素及其后代，指标弹窗会 Teleport 到此宿主。 -->
    <div id="monitor-modal-host" class="monitor-modal-host"></div>
  </section>
</template>
