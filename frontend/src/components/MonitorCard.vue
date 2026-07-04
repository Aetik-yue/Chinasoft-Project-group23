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
})

const emit = defineEmits(['open', 'dust-detail'])

const isLiveMode = ref(false)
const isFullscreen = ref(false)
const micEnabled = ref(true)
const volume = ref(62)
const currentTime = ref('')
const videoCanvas = ref(null)
const monitorCard = ref(null)
const savedShots = ref([])
const environment = ref([
  { key: 'humidity', label: '湿度', value: '待接入', route: '/api/smoke/realtime#humidity' },
  { key: 'temperature', label: '温度', value: '待接入', route: '/api/smoke/realtime#temperature' },
  { key: 'smoke', label: '烟雾浓度', value: '-- ppm', route: '/api/smoke/realtime#smokeValue' },
])
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

let timeTimer = 0
let sensorTimer = 0
let animationFrame = 0
let animationStarted = false
let realtimeTimer = 0

const RISK_LABEL = { normal: '正常', low: '低风险', medium: '中风险', high: '高风险' }
const ALARM_LABEL = { safe: '安全', alarm: '告警中', offline: '设备离线' }

function setEnvironment(key, value) {
  const item = environment.value.find((e) => e.key === key)
  if (item) item.value = value
}

// 每 3 秒拉取一次实时烟雾数据，驱动环境指标与风险状态。
// 后端 temperature/humidity 当前返回 null，按"待接入"展示（不动后端）。
async function refreshRealtime() {
  try {
    const data = await getRealtimeSmoke(props.deviceId)
    realtimeError.value = ''
    online.value = !!data?.connected
    const smoke = data?.smokeValue
    setEnvironment('smoke', smoke != null ? `${smoke} ppm` : '-- ppm')
    setEnvironment('temperature', data?.temperature != null ? `${data.temperature}℃` : '待接入')
    setEnvironment('humidity', data?.humidity != null ? `${data.humidity}%` : '待接入')
    const riskText = RISK_LABEL[data?.riskLevel] || data?.riskLevel || '--'
    const alarmText = ALARM_LABEL[data?.alarmStatus] || data?.alarmStatus || ''
    statusLabel.value = `当前状态：${alarmText || riskText}`
  } catch (e) {
    realtimeError.value = e.message
    online.value = false
    statusLabel.value = '当前状态：获取失败'
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
  if (normalized.includes('high') || fallback === '高') return '高'
  if (normalized.includes('medium') || normalized.includes('middle') || fallback === '中') return '中'
  const number = Number(value)
  if (!Number.isFinite(number)) return '低'
  if (number >= 80) return '高'
  if (number >= 35) return '中'
  return '低'
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

async function refreshSensorSnapshot() {
  try {
    const response = await fetch('/api/smoke/realtime', { cache: 'no-store' })
    if (!response.ok) throw new Error(`status ${response.status}`)
    sensorSnapshot.value = normalizeSensorPayload(await response.json())
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
  }
}

function openDustDetail() {
  emit('dust-detail', { ...sensorSnapshot.value })
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

function captureCurrentFrame() {
  const canvas = videoCanvas.value
  if (!canvas) return

  const snapshot = {
    id: `shot-${Date.now()}`,
    parrotId: 'sun-001',
    source: props.card.route,
    savedAt: new Date().toISOString(),
    image: canvas.toDataURL('image/png'),
  }

  const nextShots = [snapshot, ...savedShots.value].slice(0, 20)
  savedShots.value = nextShots
  localStorage.setItem('parrotArchiveSnapshots', JSON.stringify(nextShots))
  // TODO: POST snapshot to pet archive endpoint when backend is ready.
}

async function openWeeklyRecords() {
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
}

onMounted(() => {
  updateTime()
  refreshSensorSnapshot()
  timeTimer = window.setInterval(updateTime, 1000)
  sensorTimer = window.setInterval(refreshSensorSnapshot, 5000)
  document.addEventListener('fullscreenchange', handleFullscreenChange)

  // 实时烟雾数据：启动即拉一次，之后每 3 秒轮询（与 API 文档轮询节奏一致）
  refreshRealtime()
  realtimeTimer = window.setInterval(refreshRealtime, 3000)

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
        <span v-if="online" class="online-dot" aria-label="在线"></span>
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
      <div class="live-topbar">
        <button class="live-back-button" type="button" aria-label="返回实时通话卡片" @click="exitLiveMode">
          <span aria-hidden="true"></span>
        </button>
        <time class="live-clock">{{ currentTime }}</time>
        <span class="record-retention">监控记录保留 7 天</span>
      </div>

      <div class="live-content">
        <aside class="environment-stack" aria-label="环境指标">
          <button
            v-for="item in environment"
            :key="item.key"
            class="environment-item"
            :class="{ 'environment-button': item.interactive }"
            type="button"
            :data-api="item.route"
            :disabled="!item.interactive"
            @click="item.interactive && openDustDetail()"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em v-if="item.subValue">{{ item.subValue }}</em>
          </button>
        </aside>

        <div class="video-frame">
          <canvas ref="videoCanvas" width="720" height="430" aria-label="实时视频画面"></canvas>
          <div class="video-live-badge">
            <span></span>
            LIVE
          </div>
        </div>

        <aside class="right-live-tools" aria-label="监控工具">
          <button class="capture-button" type="button" aria-label="截图并保存到宠物档案" @click="captureCurrentFrame">
            <span class="camera-icon" aria-hidden="true"></span>
            截图
          </button>
          <button class="records-link" type="button" @click="openWeeklyRecords">
            近一周记录
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
  </section>
</template>
