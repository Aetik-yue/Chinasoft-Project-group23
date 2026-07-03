<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import ParrotVisual from './ParrotVisual.vue'

const props = defineProps({
  card: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open'])

const isLiveMode = ref(false)
const isFullscreen = ref(false)
const micEnabled = ref(true)
const volume = ref(62)
const currentTime = ref('')
const videoCanvas = ref(null)
const monitorCard = ref(null)
const savedShots = ref([])
const environment = ref([
  { key: 'humidity', label: '湿度', value: '58%', route: '/api/monitor/environment/humidity' },
  { key: 'temperature', label: '温度', value: '26.3℃', route: '/api/monitor/environment/temperature' },
  { key: 'dust', label: '粉尘浓度', value: '低', route: '/api/monitor/environment/dust' },
])

let timeTimer = 0
let animationFrame = 0
let animationStarted = false

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

function enterLiveMode() {
  if (isLiveMode.value) return
  isLiveMode.value = true
  emit('open', props.card)
  nextTick(() => {
    startMockVideoStream()
  })
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

function openWeeklyRecords() {
  emit('open', { ...props.card, route: '/monitor/records?range=7d' })
  // TODO: GET /api/monitor/records?range=7d and render recording list.
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
  timeTimer = window.setInterval(updateTime, 1000)
  document.addEventListener('fullscreenchange', handleFullscreenChange)

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
        <span v-if="card.online" class="online-dot" aria-label="在线"></span>
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
        {{ card.statusLabel }}
      </span>
      <span class="status-rays" aria-hidden="true"></span>
    </button>

    <div v-else class="live-monitor-panel" aria-label="实时视频监控模式">
      <div class="live-topbar">
        <time class="live-clock">{{ currentTime }}</time>
        <span class="record-retention">监控记录保留 7 天</span>
      </div>

      <div class="live-content">
        <aside class="environment-stack" aria-label="环境指标">
          <article v-for="item in environment" :key="item.key" class="environment-item" :data-api="item.route">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
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
