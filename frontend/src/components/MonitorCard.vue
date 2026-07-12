<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ParrotVisual from './ParrotVisual.vue'
import { getRealtimeSmoke } from '../api/smoke'
import { getAlarmLogs } from '../api/alarm'
import { useParrotVision } from '../composables/useParrotVision'
import { analyzeWithVlm, chatWithParrot } from '../api/parrot'
import { createRecording } from '../api/care'

const ParrotCage3D = defineAsyncComponent(() => import('./ParrotCage3D.vue'))

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
  environmentThresholds: {
    type: Object,
    default: () => ({ temperatureLower: 18, temperatureUpper: 30, humidityLower: 40, humidityUpper: 70, dustLower: 0, dustUpper: 35 }),
  },
})

const emit = defineEmits(['open', 'dust-detail', 'metric-update', 'snapshot-captured', 'fullscreen-change', 'alarm-notify'])

// 鹦鹉实时视觉识别（连后端 /ws/parrot；帧来源解耦，为未来 3D canvas 预留）
const {
  connected: visionConnected,
  detecting: visionDetecting,
  boxes: visionBoxes,
  behavior: visionBehavior,
  behaviorConfidence: visionBehaviorConfidence,
  species: visionSpecies,
  speciesConfidence: visionSpeciesConfidence,
  abnormal: visionAbnormal,
  error: visionError,
  start: startVision,
  stop: stopVision,
  setOverlay: setVisionOverlay,
  setFrameSource: setVisionFrameSource,
  setDeviceId: setVisionDeviceId,
} = useParrotVision()
const overlayCanvas = ref(null)
const cameraVideo = ref(null)
const videoMode = ref('mock')
const parrotScene = ref(null)
const parrotBehaviorLabel = ref('站立观察')
const interactionBusy = ref(false)
const interactionAction = ref('')

watch(() => props.deviceId, (id) => setVisionDeviceId(id || 'default'))

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
const toastText = ref('')
const dialogueActive = ref(false)
const dialogueText = ref('')
const parrotResponseText = ref('')
const showParrotBubble = ref(false)
const isListening = ref(false)
const parrotSpeaking = ref(false)

// 录音相关状态
const isRecording = ref(false)
const showSaveRecordModal = ref(false)
const recordTime = ref(0)
const recordTitleDraft = ref('')
const recordBase64 = ref('')
const recordPreviewUrl = ref('')

const parrotReplies = [
  "大吉大利，今晚吃鸡！啾~",
  "恭喜发财，红包拿来！啾啾~",
  "帅哥！美女！咔咔！",
  "吃瓜子！吃瓜子！啾~",
  "谁在叫我？是主人吗？啾！",
  "你在干嘛呀？咔咔！",
  "拜拜！下班啦！下班啦！",
  "好想出去玩呀，啾！",
  "主人最好了，比心！啾啾~",
  "你真好看！啾！",
  "早上好！中午好！晚上好！",
  "我是大明星！啾啾~"
]
// 实时状态由 /api/smoke/realtime 驱动，覆盖 card 上的 mock 值
const online = ref(!!props.card.online)
const statusLabel = ref(props.card.statusLabel || '当前状态：--')
const realtimeError = ref('')
const sensorSnapshot = ref({
  humidity: 58,
  temperature: 26.3,
  dustValue: 18,
  dustUnit: 'ppm',
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
let realtimeTimer = 0
let alarmSocket = null
let alarmHeartbeatTimer = 0
let alarmReconnectTimer = 0
let shouldReconnectAlarmSocket = true
let captureFlashTimer = 0
let saveToastTimer = 0
// 摄像头模式：getUserMedia 流与 rAF 渲染句柄
let cameraStream = null
let cameraFrame = 0
let cameraStarted = false
// 3D 模式 VLM 定时复核
let vlmTimer = 0
let vlmAvailable = false   // true = Qwen-VL 已配置并启用；false = 降级到 WS YOLO
let vlmGeneration = 0      // 世代号：start/stop 递增，作废在飞的旧 probe，防其 resolve 后复活状态
const vlmPending = ref(false)
const vlmLastResult = ref(null)

const MONITOR_COPY = {
  zh: {
    humidity: '湿度', temperature: '温度', dust: '粉尘浓度',
    low: '低', mid: '中', high: '高', suitable: '适宜', lowState: '偏低', highState: '偏高', pending: '待接入',
    alarm: '告警中', currentStatus: '当前状态', failed: '获取失败', saved: '图像已保存', weeklyRecords: '近一周记录',
    feed: '喂食', water: '加水', play: '逗玩', interacting: '互动中',
    risk: { normal: '正常', low: '低风险', medium: '中风险', high: '高风险' },
  },
  en: {
    humidity: 'Humidity', temperature: 'Temperature', dust: 'Dust',
    low: 'Low', mid: 'Medium', high: 'High', suitable: 'Good', lowState: 'Low', highState: 'High', pending: 'Pending',
    alarm: 'Alarm', currentStatus: 'Status', failed: 'Failed', saved: 'Image saved', weeklyRecords: 'Last 7 days',
    feed: 'Feed', water: 'Water', play: 'Play', interacting: 'Interacting',
    risk: { normal: 'Normal', low: 'Low risk', medium: 'Medium risk', high: 'High risk' },
  },
  es: {
    humidity: 'Humedad', temperature: 'Temperatura', dust: 'Polvo',
    low: 'Bajo', mid: 'Medio', high: 'Alto', suitable: 'Adecuado', lowState: 'Bajo', highState: 'Alto', pending: 'Pendiente',
    alarm: 'Alarma', currentStatus: 'Estado', failed: 'Error', saved: 'Imagen guardada', weeklyRecords: 'Últimos 7 días',
    feed: 'Alimentar', water: 'Agua', play: 'Jugar', interacting: 'Interactuando',
    risk: { normal: 'Normal', low: 'Riesgo bajo', medium: 'Riesgo medio', high: 'Riesgo alto' },
  },
  ja: {
    humidity: '湿度', temperature: '温度', dust: '粉じん濃度',
    low: '低', mid: '中', high: '高', suitable: '適切', lowState: '低め', highState: '高め', pending: '未接続',
    alarm: '警報中', currentStatus: '現在状態', failed: '取得失敗', saved: '画像を保存しました', weeklyRecords: '直近7日',
    feed: '餌やり', water: '給水', play: '遊ぶ', interacting: 'ふれあい中',
    risk: { normal: '正常', low: '低リスク', medium: '中リスク', high: '高リスク' },
  },
}
const monitorText = computed(() => MONITOR_COPY[props.locale] || MONITOR_COPY.zh)

// 视觉识别 UI 文案
const VISION_COPY = {
  zh: { modeLabel: '画面源', mockMode: '3D 模拟', cameraMode: '摄像头', behaviorLabel: '行为', speciesLabel: '种类', confidenceLabel: '置信度' },
  en: { modeLabel: 'Source', mockMode: '3D Sim', cameraMode: 'Camera', behaviorLabel: 'Behavior', speciesLabel: 'Species', confidenceLabel: 'Confidence' },
  es: { modeLabel: 'Fuente', mockMode: 'Sim. 3D', cameraMode: 'Cámara', behaviorLabel: 'Conducta', speciesLabel: 'Especie', confidenceLabel: 'Confianza' },
  ja: { modeLabel: '映像元', mockMode: '3D模擬', cameraMode: 'カメラ', behaviorLabel: '行動', speciesLabel: '種類', confidenceLabel: '信頼度' },
}
const visionText = computed(() => VISION_COPY[props.locale] || VISION_COPY.zh)

// 3D 模拟模式：模型为绿颊锥尾鹦鹉（与 parrotModel.js root.name 一致）
const SPECIES_3D = {
  zh: '绿颊锥尾鹦鹉', en: 'Green-cheeked Conure',
  es: 'Inseparables de Mejillas Verdes', ja: 'ホオミドリウロコインコ',
}
const species3D = computed(() => SPECIES_3D[props.locale] || SPECIES_3D.zh)
function resolve3DBehaviorLabel(key, locale) {
  const copy = BEHAVIOR_COPY[locale] || BEHAVIOR_COPY.zh
  return copy[key] || key
}
const BEHAVIOR_COPY = {
  zh: { idle: '站立观察', hop: '跳跃', eating: '进食', drinking: '饮水', preening: '梳理羽毛', flying: '飞翔', climbing: '攀爬', sleeping: '睡觉', playing: '玩耍', defecating: '排泄', calling: '鸣叫' },
  en: { idle: 'Watching', hop: 'Hopping', eating: 'Eating', drinking: 'Drinking', preening: 'Preening', flying: 'Flying', climbing: 'Climbing', sleeping: 'Sleeping', playing: 'Playing', defecating: 'Defecating', calling: 'Calling' },
  es: { idle: 'Observando', hop: 'Saltando', eating: 'Comiendo', drinking: 'Bebiendo', preening: 'Acicalándose', flying: 'Volando', climbing: 'Trepando', sleeping: 'Durmiendo', playing: 'Jugando', defecating: 'Defecando', calling: 'Vocalizando' },
  ja: { idle: '観察中', hop: 'ジャンプ', eating: '食事中', drinking: '水飲み', preening: '羽づくろい', flying: '飛行中', climbing: 'よじ登り', sleeping: '睡眠中', playing: '遊び中', defecating: '排泄中', calling: '鳴いている' },
}

const ALARM_SOCKET_URL = `ws://${import.meta.env.VITE_BACKEND_HOST || 'localhost'}:8080/ws/alarm`
const ALARM_THRESHOLDS = computed(() => props.environmentThresholds)

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
    statusLabel.value = `${monitorText.value.currentStatus}：${parrotBehaviorLabel.value} · ${alarmText || riskText}`
  } catch (e) {
    realtimeError.value = e.message
    online.value = false
    statusLabel.value = `${monitorText.value.currentStatus}：${parrotBehaviorLabel.value} · ${monitorText.value.failed}`
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
  if (number < ALARM_THRESHOLDS.value.temperatureLower) return monitorText.value.lowState
  if (number > ALARM_THRESHOLDS.value.temperatureUpper) return monitorText.value.highState
  return monitorText.value.suitable
}

function getHumidityLevel(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return monitorText.value.pending
  if (number < ALARM_THRESHOLDS.value.humidityLower) return monitorText.value.lowState
  if (number > ALARM_THRESHOLDS.value.humidityUpper) return monitorText.value.highState
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
  // 个人环境告警由后端定时任务判定并定向推送；前端只计算视觉状态，不重复创建通知。
  const nextState = {
    humidity: Number(sensorSnapshot.value.humidity) < ALARM_THRESHOLDS.value.humidityLower || Number(sensorSnapshot.value.humidity) > ALARM_THRESHOLDS.value.humidityUpper,
    temperature: Number(sensorSnapshot.value.temperature) < ALARM_THRESHOLDS.value.temperatureLower || Number(sensorSnapshot.value.temperature) > ALARM_THRESHOLDS.value.temperatureUpper,
    dust: Number(sensorSnapshot.value.dustValue) < ALARM_THRESHOLDS.value.dustLower || Number(sensorSnapshot.value.dustValue) > ALARM_THRESHOLDS.value.dustUpper,
  }

  Object.entries(nextState).forEach(([metric, alarming]) => {
    if (!alarming && alarmState.value[metric]) {
      alarmNotified.value = { ...alarmNotified.value, [metric]: false }
    }
  })

  alarmState.value = nextState
}

function handleSocketAlarm(payload) {
  if (payload?.type === 'environment_alarm') {
    const metric = payload.metric
    if (!['humidity', 'temperature', 'dust'].includes(metric)) return
    const key = metric === 'dust' ? 'dustValue' : metric
    sensorSnapshot.value = { ...sensorSnapshot.value, [key]: Number(payload.metricValue), updateTime: payload.alarmTime || new Date().toISOString() }
    alarmState.value = { ...alarmState.value, [metric]: true }
    emitAlarmNotice(metric, Number(payload.metricValue), payload)
    emitMetricUpdates()
    return
  }
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
    const token = localStorage.getItem('parrotAuthToken') || ''
    alarmSocket = new WebSocket(`${ALARM_SOCKET_URL}?token=${encodeURIComponent(token)}`)
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
  return `rotate(${-90 + ratio * 180}deg)`
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

function currentFrameCanvas() {
  if (videoMode.value === 'mock') return parrotScene.value?.getCanvas?.() || null
  return videoCanvas.value
}

function syncVisionCanvasSize(source = currentFrameCanvas()) {
  if (!source || !overlayCanvas.value) return
  if (overlayCanvas.value.width !== source.width) overlayCanvas.value.width = source.width
  if (overlayCanvas.value.height !== source.height) overlayCanvas.value.height = source.height
  setVisionOverlay(overlayCanvas.value)
}

function handle3DReady() {
  syncVisionCanvasSize()
  setVisionFrameSource(currentFrameCanvas)
  // 3D canvas 就绪（晚于 enterLiveMode 的 nextTick）后，若仍卡在 YOLO 兜底（probe 因 canvas 未就绪被跳过），重跑一次探测
  if (videoMode.value === 'mock' && !vlmAvailable && !vlmPending.value) {
    start3DVision()
  }
}

function handle3DResize(size) {
  if (videoMode.value !== 'mock' || !overlayCanvas.value) return
  if (size?.width) overlayCanvas.value.width = size.width
  if (size?.height) overlayCanvas.value.height = size.height
  setVisionOverlay(overlayCanvas.value)
}

function handle3DError(error) {
  console.warn('[MonitorCard] 3D 鸟笼不可用，已显示静态降级画面：', error?.message || error)
}

function handleParrotBehavior(payload) {
  parrotBehaviorLabel.value = BEHAVIOR_COPY[props.locale]?.[payload?.key] || payload?.label || '--'
  statusLabel.value = `${monitorText.value.currentStatus}：${parrotBehaviorLabel.value}`
  if (payload?.source !== 'user') {
    interactionBusy.value = false
    interactionAction.value = ''
  }
  // 桥接行为机 → vision 状态：仅在 Qwen-VL 模式下生效；WS YOLO 模式时由 WS 回写，不覆盖
  if (videoMode.value === 'mock' && vlmAvailable) {
    const behLabel = resolve3DBehaviorLabel(payload.key, props.locale)
    // 当状态改变时，主动重置/使上次 VLM 结果失效，以实时响应最新行为，防止旧高置信度结果卡死
    vlmLastResult.value = null
    visionBehavior.value = behLabel
    visionBehaviorConfidence.value = 0.85
    visionSpecies.value = species3D.value
    visionSpeciesConfidence.value = 0.85
  }
}

function handleInteractionState(payload) {
  const actionByBehavior = { eating: 'feed', drinking: 'water', playing: 'play' }
  interactionBusy.value = Boolean(payload?.busy)
  interactionAction.value = actionByBehavior[payload?.action] || payload?.action || ''
}

// 3D 模式视觉识别引擎：先探 Qwen-VL → 可用则 VLM 定时复核；不可用则降级 YOLO WS
async function start3DVision() {
  // 清可能残留的旧定时器，防重入泄漏/双发
  window.clearInterval(vlmTimer); vlmTimer = 0
  const gen = ++vlmGeneration
  visionDetecting.value = true
  visionConnected.value = true
  visionBoxes.value = []
  visionAbnormal.value = null
  visionError.value = ''
  vlmAvailable = false
  vlmLastResult.value = null
  vlmPending.value = false

  // 探一次 Qwen-VL，看是否已配置
  const cageCanvas = parrotScene?.value?.getCanvas?.()
  if (cageCanvas && cageCanvas.width > 0 && cageCanvas.height > 0) {
    try {
      const jpeg = cageCanvas.toDataURL('image/jpeg', 0.5)
      if (jpeg) {
        vlmPending.value = true
        const result = await analyzeWithVlm(jpeg, parrotBehaviorLabel.value, props.deviceId || 'default')
        if (gen !== vlmGeneration) return // 被更新的 start 或 stop 作废，不复活状态
        vlmLastResult.value = result
        visionBehavior.value = result.behavior
        visionBehaviorConfidence.value = result.confidence
        visionSpecies.value = result.species
        visionSpeciesConfidence.value = result.confidence
        vlmAvailable = true
      }
    } catch (e) {
      if (gen !== vlmGeneration) return
      const msg = String(e.message || '')
      // 5001 / 未启用 / SERVICE_UNAVAILABLE → Qwen 没配置，降级 YOLO
      if (!/5001|未启用|SERVICE_UNAVAILABLE|not enabled/i.test(msg)) {
        vlmAvailable = true // 非配置错误（网络等），继续用 VLM
      }
    } finally {
      if (gen === vlmGeneration) vlmPending.value = false
    }
  }

  if (gen !== vlmGeneration) return // 期间被切换/退出，不再动状态
  if (vlmAvailable) {
    // Qwen-VL 可用：先停可能残留的 YOLO WS，再开 VLM 定时复核
    stopVision()
    vlmTimer = window.setInterval(requestVlmCheck, 5000)
  } else {
    // Qwen 未配置 → 降级为 YOLO+CLIP（WS 真识别）
    startVision()
  }
}

function stop3DVision() {
  // 作废所有在飞的 probe，防其 resolve 后复活 vlmAvailable/开定时器
  vlmGeneration++
  window.clearInterval(vlmTimer); vlmTimer = 0
  if (vlmAvailable) {
    vlmPending.value = false
    vlmLastResult.value = null
  } else {
    stopVision()
  }
  vlmAvailable = false
  visionDetecting.value = false
  visionConnected.value = false
  visionBehavior.value = ''
  visionBehaviorConfidence.value = 0
  visionSpecies.value = ''
  visionSpeciesConfidence.value = 0
  visionAbnormal.value = null
  visionBoxes.value = []
  visionError.value = ''
}

async function requestVlmCheck() {
  if (!vlmAvailable || videoMode.value !== 'mock' || vlmPending.value) return
  const cageCanvas = parrotScene?.value?.getCanvas?.()
  if (!cageCanvas) return
  if (cageCanvas.width <= 0 || cageCanvas.height <= 0) return
  let jpeg = ''
  try { jpeg = cageCanvas.toDataURL('image/jpeg', 0.5) } catch { return }
  if (!jpeg) return
  vlmPending.value = true
  try {
    const result = await analyzeWithVlm(jpeg, parrotBehaviorLabel.value, props.deviceId || 'default')
    vlmLastResult.value = result
    visionBehavior.value = result.behavior
    visionBehaviorConfidence.value = result.confidence
    visionSpecies.value = result.species
    visionSpeciesConfidence.value = result.confidence
    visionError.value = ''
  } catch (e) {
    console.warn('[3D VLM] 复核失败：', e.message)
    visionError.value = ''
  } finally {
    vlmPending.value = false
  }
}

function triggerParrotInteraction(action) {
  if (interactionBusy.value || videoMode.value !== 'mock') return
  const methods = {
    feed: () => parrotScene.value?.feed?.(),
    water: () => parrotScene.value?.refillWater?.(),
    play: () => parrotScene.value?.play?.(),
  }
  const accepted = methods[action]?.()
  if (accepted) {
    interactionBusy.value = true
    interactionAction.value = action
  }
}

function enterLiveMode() {
  if (isLiveMode.value) return
  isLiveMode.value = true
  emit('open', props.card)
  nextTick(() => {
    // 实时面板经 v-if 重建后 canvas 是新元素，需重新绑定叠加层与帧来源
    setVisionOverlay(overlayCanvas.value)
    setVisionFrameSource(currentFrameCanvas)
    syncVisionCanvasSize()
    // 默认 3D 模拟模式，启动 VLM 视觉识别引擎
    if (videoMode.value === 'mock') start3DVision()
  })
}

function exitLiveMode() {
  isLiveMode.value = false
  stopVision()
  stop3DVision()
  stopCameraStream()
  videoMode.value = 'mock'
}

let voiceTimer = null
let mumbleInterval = null
let audioContext = null
let analyser = null
let micStream = null
let detectVolumeInterval = null
let detectionCooldown = false

// 开始环境声音/鹦鹉叫音量监测
function startVolumeDetection() {
  if (typeof window === 'undefined') return
  const AudioContextClass = window.AudioContext || window.webkitAudioContext
  if (!AudioContextClass) {
    console.warn('[MonitorCard] 浏览器不支持 AudioContext')
    isListening.value = true
    return
  }
  
  if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
    console.warn('[MonitorCard] 浏览器不支持 getUserMedia')
    isListening.value = true
    return
  }

  navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
    micStream = stream
    audioContext = new AudioContextClass()
    const source = audioContext.createMediaStreamSource(stream)
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 256
    source.connect(analyser)
    
    const bufferLength = analyser.frequencyBinCount
    const dataArray = new Uint8Array(bufferLength)
    
    isListening.value = true

    detectVolumeInterval = setInterval(() => {
      if (!micEnabled.value || detectionCooldown || isRecording.value) return
      
      analyser.getByteFrequencyData(dataArray)
      let sum = 0
      for (let i = 0; i < bufferLength; i++) {
        sum += dataArray[i]
      }
      const average = sum / bufferLength
      
      // 当音量超过阈值（平均值大于 32）时，认为检测到鹦鹉的叫声
      if (average > 32) {
        detectionCooldown = true
        triggerParrotAmbientSoundDetected()
        
        // 12秒冷却时间，防止过于频繁触发
        setTimeout(() => {
          detectionCooldown = false
        }, 12000)
      }
    }, 150)
  }).catch(err => {
    console.warn('[MonitorCard] 无法获取麦克风流进行音量检测:', err)
    // 降级：仅显示监听状态
    isListening.value = true
  })
}

function stopVolumeDetection() {
  isListening.value = false
  if (detectVolumeInterval) {
    clearInterval(detectVolumeInterval)
    detectVolumeInterval = null
  }
  if (audioContext) {
    try { audioContext.close() } catch (e) {}
    audioContext = null
  }
  if (micStream) {
    try {
      micStream.getTracks().forEach(track => track.stop())
    } catch (e) {}
    micStream = null
  }
}

function triggerParrotAmbientSoundDetected() {
  const randomIndex = Math.floor(Math.random() * parrotReplies.length)
  const soundText = parrotReplies[randomIndex]
  
  parrotResponseText.value = soundText
  showParrotBubble.value = true
  
  if (videoMode.value === 'mock') {
    handleParrotBehavior({ key: 'calling', label: '鸣叫' })
  }
  
  // 自动开始录制鹦鹉说话的声音
  startAutoRecordingParrotSound(`检测到鸣叫：${soundText.substring(0, 10)}`)
  
  speakParrotResponse(soundText)
  
  if (voiceTimer) clearTimeout(voiceTimer)
  voiceTimer = setTimeout(() => {
    showParrotBubble.value = false
    if (videoMode.value === 'mock') {
      handleParrotBehavior({ key: 'idle', label: '站立观察' })
    }
  }, 5000)
}

function speakParrotResponse(text) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  window.speechSynthesis.cancel()
  
  // 清洗掉表情和特殊字符以便朗读
  const cleanText = text.replace(/[🐦👤📥]|[^\w\s\u4e00-\u9fa5]/g, '').trim()
  const utterance = new SpeechSynthesisUtterance(cleanText)
  
  const voices = window.speechSynthesis.getVoices()
  const zhVoice = voices.find(v => v.lang.includes('zh') || v.lang.includes('ZH'))
  if (zhVoice) {
    utterance.voice = zhVoice
  }
  
  // 调高音调（1.9）和略微加快速度（1.2）使声音听起来像鹦鹉
  utterance.pitch = 1.9
  utterance.rate = 1.2
  
  utterance.onstart = () => {
    parrotSpeaking.value = true
  }
  utterance.onend = () => {
    parrotSpeaking.value = false
  }
  utterance.onerror = () => {
    parrotSpeaking.value = false
  }
  
  window.speechSynthesis.speak(utterance)
}

// 自动侦测并录音鹦鹉回应/碎碎念逻辑
let autoRecorder = null
let autoChunks = []

async function startAutoRecordingParrotSound(title) {
  if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
    return
  }
  // 如果当前已经在手动录音或自动录音，则跳过
  if (isRecording.value) return
  
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    autoChunks = []
    
    let options = { mimeType: 'audio/webm' }
    if (!MediaRecorder.isTypeSupported('audio/webm')) {
      options = { mimeType: 'audio/ogg' }
    }
    
    autoRecorder = new MediaRecorder(stream, options)
    
    autoRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        autoChunks.push(event.data)
      }
    }
    
    autoRecorder.onstop = () => {
      const audioBlob = new Blob(autoChunks, { type: autoRecorder.mimeType })
      
      const reader = new FileReader()
      reader.onloadend = async () => {
        try {
          const payload = {
            mediaType: 'recording',
            title: title || '鹦鹉叫声',
            imageBase64: reader.result,
            durationSeconds: 4,
            tags: 'parrot-auto-detected'
          }
          await createRecording(props.parrotId, payload)
          showCaptureFeedback('已自动录制并保存鹦鹉声音')
        } catch (e) {
          console.error('自动保存鹦鹉声音失败', e)
        }
      }
      reader.readAsDataURL(audioBlob)
      stream.getTracks().forEach(track => track.stop())
    }
    
    isRecording.value = true
    recordTime.value = 0
    autoRecorder.start()
    
    // 4秒后自动停止录制（鹦鹉单句叫声时长）
    setTimeout(() => {
      if (autoRecorder && autoRecorder.state !== 'inactive') {
        autoRecorder.stop()
        isRecording.value = false
      }
    }, 4000)
    
  } catch (e) {
    console.error('无法启动自动录音', e)
  }
}

// 模拟鹦鹉在房间的“碎碎念”检测定时器
function startMumbleDetection() {
  if (mumbleInterval) clearInterval(mumbleInterval)
  mumbleInterval = setInterval(() => {
    // 仅当麦克风开启、鹦鹉没有在说话、且未在录音时，一定概率触发碎碎念
    if (micEnabled.value && !parrotSpeaking.value && !isRecording.value) {
      if (Math.random() < 0.45) {
        triggerParrotMumble()
      }
    }
  }, 25000)
}

function stopMumbleDetection() {
  if (mumbleInterval) {
    clearInterval(mumbleInterval)
    mumbleInterval = null
  }
}

function triggerParrotMumble() {
  const randomIndex = Math.floor(Math.random() * parrotReplies.length)
  const mumbleText = parrotReplies[randomIndex]
  
  parrotResponseText.value = mumbleText
  showParrotBubble.value = true
  
  if (videoMode.value === 'mock') {
    handleParrotBehavior({ key: 'calling', label: '鸣叫' })
  }
  
  // 自动开启录制并命名为“鹦鹉碎碎念”
  startAutoRecordingParrotSound(`鹦鹉碎碎念：${mumbleText.substring(0, 10)}`)
  
  speakParrotResponse(mumbleText)
  
  if (voiceTimer) clearTimeout(voiceTimer)
  voiceTimer = setTimeout(() => {
    showParrotBubble.value = false
    if (videoMode.value === 'mock') {
      handleParrotBehavior({ key: 'idle', label: '站立观察' })
    }
  }, 5000)
}

// 手动录音逻辑 (MediaRecorder - 供右侧栏手动开启/预览/保存)
let mediaRecorder = null
let audioChunks = []
let recordInterval = 0

async function startAudioRecording() {
  if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
    alert('当前浏览器或协议不支持录音功能，请检查是否为 HTTPS 或 localhost')
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    
    let options = { mimeType: 'audio/webm' }
    if (!MediaRecorder.isTypeSupported('audio/webm')) {
      options = { mimeType: 'audio/ogg' }
    }
    
    mediaRecorder = new MediaRecorder(stream, options)
    
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }
    
    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType })
      recordPreviewUrl.value = URL.createObjectURL(audioBlob)
      
      const reader = new FileReader()
      reader.onloadend = () => {
        recordBase64.value = reader.result
      }
      reader.readAsDataURL(audioBlob)
      
      stream.getTracks().forEach(track => track.stop())
    }
    
    recordTime.value = 0
    isRecording.value = true
    mediaRecorder.start()
    
    recordInterval = window.setInterval(() => {
      recordTime.value++
    }, 1000)
    
  } catch (e) {
    console.error('无法启动录音', e)
    alert('启动录音失败: ' + e.message)
  }
}

function stopAudioRecording() {
  if (!isRecording.value || !mediaRecorder) return
  isRecording.value = false
  window.clearInterval(recordInterval)
  mediaRecorder.stop()
  
  const now = new Date()
  const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  recordTitleDraft.value = `鹦鹉叫声记录 ${timeStr}`
  showSaveRecordModal.value = true
}

function toggleAudioRecording() {
  if (isRecording.value) {
    stopAudioRecording()
  } else {
    startAudioRecording()
  }
}

function cancelSaveRecord() {
  showSaveRecordModal.value = false
  recordPreviewUrl.value = ''
  recordBase64.value = ''
}

async function saveAudioRecord() {
  if (!recordBase64.value) return
  try {
    const payload = {
      mediaType: 'recording',
      title: recordTitleDraft.value.trim() || '鹦鹉学舌',
      imageBase64: recordBase64.value,
      durationSeconds: recordTime.value,
      tags: 'user-recorded'
    }
    await createRecording(props.parrotId, payload)
    showSaveRecordModal.value = false
    showCaptureFeedback('录音已保存到成长报告')
  } catch (e) {
    console.error('保存录音失败', e)
    alert('保存失败: ' + e.message)
  }
}

async function saveResponseAsRecord() {
  if (!parrotResponseText.value) return
  try {
    const payload = {
      mediaType: 'recording',
      title: `学舌：${parrotResponseText.value.substring(0, 10)}`,
      imageBase64: 'data:audio/wav;base64,UklGRigAAABXQVZFZm10IBIAAAABAAEARKwAAIhYAQACABAAAABkYXRhAgAAAAAA',
      durationSeconds: 3,
      tags: 'mimic'
    }
    await createRecording(props.parrotId, payload)
    showCaptureFeedback('学舌对话已记录')
  } catch (e) {
    console.error('保存学舌对话失败', e)
  }
}

function toggleMic() {
  micEnabled.value = !micEnabled.value
  if (micEnabled.value) {
    dialogueActive.value = true
    if (!recognition) initSpeechRecognition()
    if (recognition && !isListening.value) {
      try { recognition.start() } catch (e) {}
    }
  } else {
    dialogueActive.value = false
    isListening.value = false
    if (recognition) {
      try { recognition.stop() } catch (e) {}
    }
  }
}

function changeVolume(delta) {
  volume.value = Math.min(100, Math.max(0, volume.value + delta))
}

function showCaptureFeedback(customText) {
  window.clearTimeout(captureFlashTimer)
  window.clearTimeout(saveToastTimer)
  captureFlash.value = false
  saveToastVisible.value = false
  toastText.value = customText || monitorText.value.saved

  requestAnimationFrame(() => {
    captureFlash.value = !customText
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
  const canvas = currentFrameCanvas()
  if (!canvas) return

  const image = videoMode.value === 'mock'
    ? parrotScene.value?.captureJpeg?.({ quality: 0.6, maxWidth: 480 })
    : compressCanvasToJpeg(canvas, 0.6, 480)
  if (!image) return

  // JPEG 压缩 + 缩小尺寸，控制 base64 体积（单张约 20~30KB，避免 localStorage 爆容量）
  const snapshot = {
    id: `shot-${Date.now()}`,
    parrotId: props.parrotId || 'sun-001',
    source: props.card.route,
    savedAt: new Date().toISOString(),
    image,
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

// 画面源切换：3D 模拟 / 摄像头（真实流 + 视觉识别）
async function switchMode(mode) {
  if (videoMode.value === mode) return
  videoMode.value = mode
  if (mode === 'camera') {
    syncVisionCanvasSize(videoCanvas.value)
    const ok = await startCameraStream()
    if (ok) {
      stop3DVision()
      startVision()
    } else {
      // 摄像头不可用，回退模拟
      videoMode.value = 'mock'
      nextTick(() => {
        syncVisionCanvasSize()
        start3DVision()
      })
    }
  } else {
    stopVision()
    stopCameraStream()
    start3DVision()
    nextTick(() => syncVisionCanvasSize())
  }
}

async function startCameraStream() {
  if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
    console.warn('[MonitorCard] 浏览器不支持 getUserMedia')
    return false
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: { ideal: 720 }, height: { ideal: 430 }, facingMode: 'user' },
      audio: false,
    })
    cameraStream = stream
    if (cameraVideo.value) {
      cameraVideo.value.srcObject = stream
      await cameraVideo.value.play().catch(() => {})
    }
    cameraStarted = true
    drawCameraFrame()
    return true
  } catch (e) {
    console.warn('[MonitorCard] 摄像头启动失败：', e.message)
    return false
  }
}

function stopCameraStream() {
  cameraStarted = false
  if (cameraFrame) cancelAnimationFrame(cameraFrame)
  cameraFrame = 0
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop())
    cameraStream = null
  }
  if (cameraVideo.value) cameraVideo.value.srcObject = null
}

// 摄像头模式：把 <video> 当前帧画进 videoCanvas（与模拟共用同一 canvas，叠加层坐标 1:1）
// 水平翻转以取消摄像头镜像，呈自然自拍视角；canvas 内容即发送给后端的帧，
// 后端返回的框坐标与翻转后画面一致，叠加层直接绘制即对齐。
function drawCameraFrame() {
  if (!cameraStarted) return
  const canvas = videoCanvas.value
  const video = cameraVideo.value
  if (canvas && video && video.readyState >= 2) {
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.save()
    ctx.scale(-1, 1)
    ctx.drawImage(video, -canvas.width, 0, canvas.width, canvas.height)
    ctx.restore()
  }
  cameraFrame = requestAnimationFrame(drawCameraFrame)
}

function formatPercent(value) {
  const n = Number(value) || 0
  return `${Math.round(n * 100)}%`
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
  // 视觉识别初始化：叠加层保持独立，帧来源按 3D / 摄像头模式动态选择。
  setVisionOverlay(overlayCanvas.value)
  setVisionFrameSource(currentFrameCanvas)
  setVisionDeviceId(props.deviceId || 'default')
  // 3D 模式默认启动视觉识别（handle3DReady 也会触发，世代号幂等去重）
  nextTick(() => {
    if (videoMode.value === 'mock') start3DVision()
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
  stopVision()
  stop3DVision()
  stopCameraStream()
  // 页面切换可能先卸载组件、后触发浏览器的 fullscreenchange，因此在卸载时主动清理父级状态。
  emit('fullscreen-change', false)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
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
          {{ toastText || monitorText.saved }}
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

        <div class="video-frame" :class="{ 'is-capturing': captureFlash, 'mock-mode': videoMode === 'mock' }">
          <ParrotCage3D
            v-show="videoMode === 'mock'"
            ref="parrotScene"
            :active="videoMode === 'mock'"
            :locale="locale"
            @ready="handle3DReady"
            @resize="handle3DResize"
            @error="handle3DError"
            @behavior-change="handleParrotBehavior"
            @interaction-state="handleInteractionState"
          />
          <canvas
            v-show="videoMode === 'camera'"
            ref="videoCanvas"
            width="720"
            height="430"
            aria-label="摄像头实时画面"
          ></canvas>
          <canvas ref="overlayCanvas" class="vision-overlay" width="720" height="430" aria-hidden="true"></canvas>
          <video ref="cameraVideo" class="camera-source" autoplay muted playsinline></video>

          <div class="mode-toggle" role="group" :aria-label="visionText.modeLabel">
            <button type="button" :class="{ active: videoMode === 'mock' }" @click="switchMode('mock')">{{ visionText.mockMode }}</button>
            <button type="button" :class="{ active: videoMode === 'camera' }" @click="switchMode('camera')">{{ visionText.cameraMode }}</button>
          </div>

          <div v-if="videoMode === 'mock' || visionDetecting" class="vision-readout" role="status">
            <span><em>{{ visionText.behaviorLabel }}</em><strong>{{ visionBehavior || '--' }}</strong></span>
            <span><em>{{ visionText.speciesLabel }}</em><strong>{{ visionSpecies || '--' }}</strong></span>
            <span v-if="visionBehaviorConfidence"><em>{{ visionText.confidenceLabel }}</em><strong>{{ formatPercent(visionBehaviorConfidence) }}</strong></span>
            <span v-if="visionError" class="vision-error">{{ visionError }}</span>
            <span v-if="vlmPending" class="vision-vlm">🔄 复核中…</span>
            <span v-else-if="vlmLastResult" class="vision-vlm">🤖 已复核</span>
          </div>

          <div v-if="visionAbnormal" class="abnormal-banner" :class="visionAbnormal.severity === 'DANGER' ? 'danger' : 'warning'" role="alert">
            ⚠ {{ visionAbnormal.message }}
          </div>

          <div v-if="videoMode === 'mock'" class="parrot-behavior-pill" aria-live="polite">
            <span aria-hidden="true"></span>
            {{ parrotBehaviorLabel }}
          </div>

          <div v-if="videoMode === 'mock'" class="parrot-interaction-bar" role="group" aria-label="鹦鹉互动">
            <button
              type="button"
              :disabled="interactionBusy"
              :class="{ active: interactionAction === 'feed' }"
              @click="triggerParrotInteraction('feed')"
            >
              <span aria-hidden="true">●</span>{{ interactionAction === 'feed' ? monitorText.interacting : monitorText.feed }}
            </button>
            <button
              type="button"
              :disabled="interactionBusy"
              :class="{ active: interactionAction === 'water' }"
              @click="triggerParrotInteraction('water')"
            >
              <span aria-hidden="true">◆</span>{{ interactionAction === 'water' ? monitorText.interacting : monitorText.water }}
            </button>
            <button
              type="button"
              :disabled="interactionBusy"
              :class="{ active: interactionAction === 'play' }"
              @click="triggerParrotInteraction('play')"
            >
              <span aria-hidden="true">✦</span>{{ interactionAction === 'play' ? monitorText.interacting : monitorText.play }}
            </button>
          </div>

          <!-- 鹦鹉对话气泡 -->
          <div v-if="showParrotBubble" class="parrot-chat-bubble" :class="{ 'is-speaking': parrotSpeaking }">
            <span class="parrot-bubble-arrow"></span>
            <div class="bubble-content">
              <span>🐦 鹦鹉:</span>
              <p>{{ parrotResponseText }}</p>
            </div>
            <button type="button" class="bubble-save-btn" title="记录此段话语" @click="saveResponseAsRecord">
              📥 记录
            </button>
          </div>

          <!-- 语音波形与状态指示 -->
          <div v-if="isListening || isRecording" class="voice-wave-overlay">
            <div class="wave-container">
              <span class="wave-bar bar-1"></span>
              <span class="wave-bar bar-2"></span>
              <span class="wave-bar bar-3"></span>
              <span class="wave-bar bar-4"></span>
              <span class="wave-bar bar-5"></span>
            </div>
            <span class="voice-status-text">
              {{ isRecording ? '🎙️ 正在录制并保存鹦鹉声音...' : '🎧 正在监听环境/等待鹦鹉回应...' }}
            </span>
          </div>

          <div class="video-live-badge">
            <span></span>
            LIVE
          </div>
        </div>

        <aside class="right-live-tools" aria-label="监控工具">
          <button class="capture-button" type="button" aria-label="截图并保存到宠物档案" @click="captureCurrentFrame">
            <span class="camera-icon" aria-hidden="true"></span>
          </button>
          <!-- 录音记录工具按钮 -->
          <button 
            class="mic-tool-button" 
            :class="{ 'recording-now': isRecording }"
            type="button" 
            :aria-label="isRecording ? '停止录音' : '录制鹦鹉叫声'" 
            @click="toggleAudioRecording"
          >
            <span class="mic-tool-icon"></span>
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

    <!-- 录音保存弹窗 -->
    <transition name="modal-fade">
      <div v-if="showSaveRecordModal" class="save-record-modal-overlay">
        <div class="save-record-modal">
          <header class="modal-header">
            <h3>💾 保存有趣学舌</h3>
          </header>
          <div class="modal-body">
            <label class="modal-field">
              <span>给这段录音取个名字：</span>
              <input v-model="recordTitleDraft" type="text" placeholder="有趣学舌" />
            </label>
            <div class="audio-preview-section">
              <span>试听：</span>
              <audio :src="recordPreviewUrl" controls class="modal-audio-player"></audio>
            </div>
            <p class="modal-meta-info">时长: {{ recordTime }} 秒 | 录制于今天</p>
          </div>
          <footer class="modal-footer">
            <button type="button" class="btn-cancel" @click="cancelSaveRecord">取消</button>
            <button type="button" class="btn-save" @click="saveAudioRecord">保存到报告</button>
          </footer>
        </div>
      </div>
    </transition>

    <!-- 浏览器全屏只显示全屏元素及其后代，指标弹窗会 Teleport 到此宿主。 -->
    <div id="monitor-modal-host" class="monitor-modal-host"></div>
  </section>
</template>
