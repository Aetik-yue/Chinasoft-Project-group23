<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Cropper, CircleStencil } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import CurrentBirdCard from './components/CurrentBirdCard.vue'
import EntryCard from './components/EntryCard.vue'
import LedgerCharts from './components/LedgerCharts.vue'
import MedicalCharts from './components/MedicalCharts.vue'
import LoginView from './components/LoginView.vue'
import MonitorCard from './components/MonitorCard.vue'
import ParrotVisual from './components/ParrotVisual.vue'
import handbookIcon from './assets/home-icons/handbook.png'
import medicalIcon from './assets/home-icons/medical.png'
import archiveIcon from './assets/home-icons/archive.png'
import settingsIcon from './assets/home-icons/settings.png'
import { recognizeParrotBehavior } from './api/parrot'
import {
  changePassword as apiChangePassword,
  deleteAccount as apiDeleteAccount,
  fetchUserProfile,
  updateUserProfile as apiUpdateUserProfile,
} from './api/auth'
import { parseMarkdown } from './utils/markdown'
import { getEnvironmentReport } from './api/environment'
import { getAlarmLogs } from './api/alarm'
import { getRealtimeSmoke, getSmokeHistory } from './api/smoke'
import {
  deleteParrot as deleteParrotApi,
  deleteLedgerRecord as deleteLedgerRecordApi,
  createLedgerRecord as createLedgerRecordApi,
  createMedicalRecord as createMedicalRecordApi,
  createParrot,
  createPhoto,
  createWeight as createWeightApi,
  deletePhoto as deletePhotoApi,
  getBehaviorTodayStats,
  getTodaySleepSummary,
  listLedgerRecords,
  listMedicalRecords,
  listParrots,
  listPhotos,
  listWeights,
  updateParrot,
  updateLedgerRecord as updateLedgerRecordApi,
  updateMedicalRecord as updateMedicalRecordApi,
  deleteMedicalRecord as deleteMedicalRecordApi,
  updateWeight as updateWeightApi,
} from './api/care'
import { listDevices } from './api/device'
import { getUserPreferences, updateUserPreferences } from './api/preferences'
import { http } from './api/request'
import {
  archiveProfiles,
  currentParrot,
  detailViews,
  entryCards,
  handbookModules,
  hospitalPins,
  medicalModules,
  parrotSpeciesOptions,
  parrots,
  photoRecords,
  primaryCards,
  recordingRecords,
  reportCurveSets,
  reportRecords,
  reportStats,
  tutorials,
  userProfile,
} from './data/mockDashboard'
import { getSpeciesCareProfile, getToxicReason } from './data/speciesCareProfiles'

const activeRoute = ref('')
const thirdView = ref('')
const lastOpenedRoute = ref('')
const petSwitchOpen = ref(false)
const localParrots = ref([...parrots])
const profiles = ref([...archiveProfiles])
const readBadgeKeys = ref(loadReadBadgeKeys())
const notificationBadges = ref(
  Object.fromEntries(Object.entries(entryCards).map(([key, card]) => [
    key,
    readBadgeKeys.value.includes(key) ? 0 : card.badge || 0,
  ])),
)
const EMPTY_REMOTE_PARROT = Object.freeze({
  id: '',
  petId: '',
  deviceId: 'device-001',
  avatarType: 'avatar-orange',
  name: '暂无档案',
  shortName: '暂无档案',
  species: '未录入',
  birthday: '',
  weight: '未录入',
  sex: '未知',
  status: '未录入',
  ageStage: '请先新增鹦鹉档案',
  route: '/archive',
})
const selectedParrot = ref(currentParrot)
const activeArchiveId = ref(archiveProfiles[0]?.id || '')
const activeReportRange = ref('月报')
// 成长报告所选日期（YYYY-MM-DD）；为空时后端按 range 取最近一个已结束周期。
const reportDate = ref('')
// ECharts 容器 ref。
const echartsRef = ref(null)
// 专属推荐 · 推荐食谱饼图容器 ref。
const dietChartRef = ref(null)
let dietChartInstance = null
// 日期选择弹窗：当前选中的 range。
const reportPickerRange = ref('')
const reportPickerDate = ref('')

// AMap 实时地图状态
let amapInstance = null
let AMapClass = null
let amapMarkers = []
const mapLoaded = ref(false)
const hospitalSearchQuery = ref('')
const dynamicHospitalPins = ref([...hospitalPins])

const filteredHospitalPins = computed(() => dynamicHospitalPins.value)

function openReportPicker(range) {
  reportPickerRange.value = range
  // 如果当前已选日期仍落在该 range 周期内，就保留它；否则回落到默认日期。
  reportPickerDate.value = dateBelongsToRange(reportDate.value, range)
    ? reportDate.value
    : defaultReportDate(range)
  openModal('report-date', range === '日报' ? '选日期' : range === '周报' ? '选周' : '选月', {})
}
function confirmReportDate() {
  if (!reportPickerDate.value) return
  reportDate.value = reportPickerDate.value
  activeReportRange.value = reportPickerRange.value
  closeModal()
  // 进入详情页视图
  const key = reportPickerRange.value === '日报' ? 'daily-detail'
    : reportPickerRange.value === '周报' ? 'weekly-detail' : 'monthly-detail'
  thirdView.value = key
}

// 顶部“日报/周报/月报”按钮 hover 时打开的日期选择浮层。
function ensureHistoryHoverDate(range) {
  historyHoverRange.value = range
  if (!historyHoverDate.value[range]) {
    historyHoverDate.value[range] = defaultReportDate(range)
  }
  // 定位到已选日期所在月份。
  const selected = historyHoverDate.value[range]
  const ref = selected ? new Date(`${selected}T00:00:00`) : new Date()
  if (!Number.isNaN(ref.getTime())) {
    historyCalendarMonth.value[range] = { year: ref.getFullYear(), month: ref.getMonth() }
  }
}
function changeHistoryCalendarMonth(range, delta) {
  const current = historyCalendarMonth.value[range]
  let year = current.year
  let month = current.month + delta
  if (month < 0) { month = 11; year-- }
  if (month > 11) { month = 0; year++ }
  historyCalendarMonth.value[range] = { year, month }
}
function confirmHistoryDate(range) {
  const date = historyHoverDate.value[range]
  if (!date) return
  reportDate.value = date
  activeReportRange.value = range
  const key = range === '日报' ? 'daily-detail'
    : range === '周报' ? 'weekly-detail' : 'monthly-detail'
  thirdView.value = key
}
function selectHistoryDay(range, day) {
  const max = new Date(`${defaultReportDate(range)}T23:59:59`)
  if (day > max) return
  if (range === '周报') {
    const sun = endOfWeek(day)
    historyHoverDate.value['周报'] = formatDate(sun)
  } else if (range === '月报') {
    const last = endOfMonth(day.getFullYear(), day.getMonth())
    historyHoverDate.value['月报'] = formatDate(last)
  } else {
    historyHoverDate.value['日报'] = formatDate(day)
  }
}
function selectHistoryMonth(year, month) {
  const last = endOfMonth(year, month)
  const max = new Date(`${defaultReportDate('月报')}T23:59:59`)
  if (last > max) return
  historyHoverDate.value['月报'] = formatDate(last)
}
function openAlarmDetail() {
  if (latestAlarmRecord.value) {
    showAlarmToast(latestAlarmRecord.value.message || '环境异常')
  }
}

// 各 range 的默认日期（也是日期选择器的 max）：日报=昨天、周报=上周日、月报=上月末。
function defaultReportDate(range) {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const ymd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  if (range === '周报') {
    const sun = new Date(now)
    const dow = sun.getDay()
    sun.setDate(sun.getDate() - (dow === 0 ? 7 : dow)) // 最近一个已完整过去的周日
    return ymd(sun)
  }
  if (range === '月报') {
    return ymd(new Date(now.getFullYear(), now.getMonth(), 0)) // 上月最后一天
  }
  const y = new Date(now); y.setDate(y.getDate() - 1)
  return ymd(y)
}

// 自定义日期选择器辅助函数。
const pad2 = (n) => String(n).padStart(2, '0')
function formatDate(d) {
  if (!d || Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}
// 把后端 LocalDateTime 字符串（如 2026-07-11T14:30:00）格式化成 MM-DD HH:mm。
function formatRecordTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}
function startOfWeek(d) {
  const date = new Date(d)
  const dow = (date.getDay() + 6) % 7 // 周一=0..周日=6
  date.setDate(date.getDate() - dow)
  date.setHours(0, 0, 0, 0)
  return date
}
function endOfWeek(d) {
  const mon = startOfWeek(d)
  const sun = new Date(mon)
  sun.setDate(mon.getDate() + 6)
  sun.setHours(23, 59, 59, 999)
  return sun
}
function startOfMonth(year, month) {
  return new Date(year, month, 1)
}
function endOfMonth(year, month) {
  return new Date(year, month + 1, 0)
}
function isSameDay(d1, d2) {
  if (!d1 || !d2 || Number.isNaN(d1.getTime()) || Number.isNaN(d2.getTime())) return false
  return d1.getFullYear() === d2.getFullYear()
    && d1.getMonth() === d2.getMonth()
    && d1.getDate() === d2.getDate()
}
function isBeforeOrSameDay(d1, d2) {
  if (!d1 || !d2) return false
  return new Date(`${formatDate(d1)}T00:00:00`).getTime() <= new Date(`${formatDate(d2)}T00:00:00`).getTime()
}
function calendarDays(year, month) {
  const first = startOfMonth(year, month)
  const start = startOfWeek(first)
  const days = []
  for (let i = 0; i < 42; i++) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    days.push(d)
  }
  return days
}
function monthLabel(m) {
  return `${m + 1}月`
}
function weekLabelByDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(`${dateStr}T00:00:00`)
  if (Number.isNaN(d.getTime())) return ''
  const mon = startOfWeek(d)
  const sun = endOfWeek(d)
  return `${mon.getMonth() + 1}/${mon.getDate()} - ${sun.getMonth() + 1}/${sun.getDate()}`
}
function monthLabelByDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(`${dateStr}T00:00:00`)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
}

const modal = ref(null)
const monitorFullscreen = ref(false)
const selectedHospital = ref(hospitalPins[0])
const diagnosisForm = ref({
  energy: '精神活跃',
  appetite: '正常进食',
  breathing: '无异常',
  droppings: '正常',
})
// 症状维度：每维 4 档（0 正常 → 3 急症），score 用于风险引擎，hint 为临床依据 i18n key。
const TRIAGE_DIMENSIONS = [
  {
    key: 'energy', label: 'energy', hint: 'triageHintEnergyDim',
    options: [
      { value: '精神活跃', score: 0, hint: 'triageHintEnergy0' },
      { value: '略显安静', score: 1, hint: 'triageHintEnergy1' },
      { value: '蓬毛嗜睡', score: 2, hint: 'triageHintEnergy2' },
      { value: '伏底闭眼', score: 3, hint: 'triageHintEnergy3' },
    ],
  },
  {
    key: 'appetite', label: 'appetite', hint: 'triageHintAppetiteDim',
    options: [
      { value: '正常进食', score: 0, hint: 'triageHintAppetite0' },
      { value: '食量下降', score: 1, hint: 'triageHintAppetite1' },
      { value: '明显拒食', score: 2, hint: 'triageHintAppetite2' },
      { value: '拒食逾24h', score: 3, hint: 'triageHintAppetite3' },
    ],
  },
  {
    key: 'breathing', label: 'breathing', hint: 'triageHintBreathingDim',
    options: [
      { value: '无异常', score: 0, hint: 'triageHintBreathing0' },
      { value: '偶尔喷嚏', score: 1, hint: 'triageHintBreathing1' },
      { value: '尾上下摆', score: 2, hint: 'triageHintBreathing2' },
      { value: '持续张口呼吸', score: 3, hint: 'triageHintBreathing3' },
    ],
  },
  {
    key: 'droppings', label: 'droppings', hint: 'triageHintDroppingsDim',
    options: [
      { value: '正常', score: 0, hint: 'triageHintDroppings0' },
      { value: '偏稀多尿', score: 1, hint: 'triageHintDroppings1' },
      { value: '含未消化', score: 2, hint: 'triageHintDroppings2' },
      { value: '黄绿或血便', score: 3, hint: 'triageHintDroppings3' },
    ],
  },
]
const tutorialKeyword = ref('')
const activeTutorialId = ref('')
const tutorialArticleHtml = ref('')
const tutorialArticleLoading = ref(false)
const tutorialArticleError = ref('')
const birdImage = ref(null)
const birdImagePreview = ref('')
const birdLoading = ref(false)
const birdError = ref('')
const medicalRecordSearch = ref('')
const newMedicalDraft = ref({
  recordDate: new Date().toISOString().slice(0, 10),
  recordType: 'symptom',
  content: '',
  hospitalName: '',
  hospitalPhone: '',
})
const medFormMoreOpen = ref(false)
const ledgerKeyword = ref('')
const LEDGER_CATEGORIES = Object.freeze(['食物', '医疗', '清洁', '玩具', '其他'])
const ledgerCategoryFilter = ref('全部')
const ledgerDraft = ref({
  time: currentLocalDateText(),
  tag: '食物',
  description: '',
  amount: '',
})
const ledgerSaving = ref(false)
const ledgerDeleting = ref(false)
const ledgerFeedback = ref('')
const ledgerFormError = ref('')
const editingMedicalId = ref('')
const editingMedicalDraft = ref({
  recordDate: new Date().toISOString().slice(0, 10),
  recordType: 'symptom',
  content: '',
  hospitalName: '',
  hospitalPhone: '',
})
const editingLedgerId = ref('')
const editingLedgerDraft = ref(null)
const medicalRecords = ref([
  { id: 'm1', recordId: null, recordDate: '2026-07-01', recordType: 'symptom', title: null, content: '羽粉偏高，通风后恢复', hospitalName: '', hospitalPhone: '' },
  { id: 'm2', recordId: null, recordDate: '2026-06-20', recordType: 'recheck', title: null, content: '体重 77.5g，精神正常', hospitalName: '', hospitalPhone: '' },
  { id: 'm3', recordId: null, recordDate: '2026-06-02', recordType: 'medication', title: null, content: '药浴后保温 2 小时', hospitalName: '异宠诊所', hospitalPhone: '021-12345678' },
])
const ledgerRecords = ref([
  { id: 'l1', time: '2026-07-03', createdAt: '2026-07-03 09:18', updatedAt: '', tag: '食物', description: '老爹 · 主粮补充装', amount: 88, system: true },
  { id: 'l2', time: '2026-07-01', createdAt: '2026-07-01 18:42', updatedAt: '', tag: '玩具', description: '刀哥 · 磨爪站杆', amount: 36, system: true },
  { id: 'l3', time: '2026-06-28', createdAt: '2026-06-28 10:07', updatedAt: '2026-06-29 11:30', tag: '医疗', description: '农药 · 体检挂号', amount: 120, system: true },
])
const profileForm = ref({
  species: '小太阳',
  name: '',
  birthday: '2024-05-18',
  weight: '',
  sex: '未知',
  currentStatus: '站立',
  deviceId: 'device-001',
})
const availableDevices = ref([])
const profileEditId = ref('')

// 成长报告用的真实环境历史：从后端 /environment/history 直接读库。
// 每条 { time: ISO 时间, temperature, humidity, dust }，缺项为 null。
const environmentHistory = ref([])
const environmentLoading = ref(false)

// 成长报告的时间范围 → 后端 range 参数。
const rangeToParam = { '日报': 'daily', '周报': 'weekly', '月报': 'monthly' }

const environmentError = ref('')

// 成长报告实时仪表盘数据。
const realtimeSnapshot = ref({
  temperature: null,
  humidity: null,
  smokeValue: null,
  smokeStale: false,      // 烟雾值来自上一条记录（传感器离线时的兜底）
  smokeRecordTime: '',    // 上次记录时间，配合 smokeStale 展示
  dustUnit: 'ppm',
  dustLevel: '',
  connected: false,
  updateTime: '',
  riskLevel: '',
  alarmStatus: '',
})
// 防重复调用 / 首次加载态：repeatLoading 为 true 时 loadRealtimeEnv 跳过（两个 watch 同时点火
// 或轮询与被手动刷新重叠时只能跑一个）；everLoaded 区分"还没加载过"和"加载了但没有数据"。
const realtimeRepeatLoading = ref(false)
const realtimeEverLoaded = ref(false)
const todayEnvHistory = ref([])
const todaySleepSummary = ref({ sleepDurationMinutes: 0 })
const latestAlarmRecord = ref(null)
// 顶部日报/周报/月报按钮 hover 日期选择浮层。
const historyHoverDate = ref({ '日报': '', '周报': '', '月报': '' })
const historyHoverRange = ref('')
const historyCalendarMonth = ref({
  '日报': { year: new Date().getFullYear(), month: new Date().getMonth() },
  '周报': { year: new Date().getFullYear(), month: new Date().getMonth() },
  '月报': { year: new Date().getFullYear(), month: new Date().getMonth() },
})
let dashboardRealtimeTimer = 0

async function loadRealtimeEnv() {
  // ★ 防重复调用：两个 watch 同时点火或轮询与手动刷新重叠时只跑一个。
  if (realtimeRepeatLoading.value) return
  const deviceId = selectedArchive.value?.deviceId || selectedParrot.value?.deviceId || ''
  if (!deviceId) { realtimeEverLoaded.value = true; return }
  realtimeRepeatLoading.value = true
  try {
    const data = await getRealtimeSmoke(deviceId)
    let smokeValue = data?.smokeValue ?? null
    let smokeStale = false
    let smokeRecordTime = ''
    // 传感器离线（smokeValue 为 null）时，用历史最后一条记录兜底，
    // 避免粉尘直接判缺失导致评分失真（比如温湿度满分时凑出 100 分）。
    if (smokeValue == null) {
      try {
        const hist = await getSmokeHistory({ deviceId, range: '7d' })
        const last = Array.isArray(hist) && hist.length ? hist[hist.length - 1] : null
        if (last?.value != null) {
          smokeValue = last.value
          smokeStale = true
          smokeRecordTime = last.time || ''
        }
      } catch (e) {
        console.warn('烟雾历史兜底加载失败：', e?.message)
      }
      // ★ 历史兜底偶发落空时，保留上一次成功拿到的兜底值（避免粉尘在两次轮询间短暂"暂无数据"）
      if (smokeValue == null) {
        const prev = realtimeSnapshot.value
        if (prev.smokeStale && prev.smokeValue != null) {
          smokeValue = prev.smokeValue
          smokeStale = true
          smokeRecordTime = prev.smokeRecordTime
        }
      }
    }
    realtimeSnapshot.value = {
      temperature: data?.temperature ?? null,
      humidity: data?.humidity ?? null,
      smokeValue,
      smokeStale,
      smokeRecordTime,
      dustUnit: data?.unit || 'ppm',
      dustLevel: data?.riskLevel || '',
      connected: !!data?.connected,
      updateTime: data?.updateTime || new Date().toISOString(),
      riskLevel: data?.riskLevel || '',
      alarmStatus: data?.alarmStatus || '',
    }
  } catch (e) {
    console.warn('实时环境加载失败：', e?.message)
  } finally {
    realtimeRepeatLoading.value = false
    realtimeEverLoaded.value = true
  }
}

async function loadTodayEnvironmentHistory() {
  const deviceId = selectedArchive.value?.deviceId || selectedParrot.value?.deviceId || ''
  if (!deviceId) { todayEnvHistory.value = []; return }
  try {
    const data = await getEnvironmentReport(deviceId, 'daily', todayText.value)
    todayEnvHistory.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.warn('今日环境历史加载失败：', e?.message)
    todayEnvHistory.value = []
  }
}

async function loadTodaySleepSummary() {
  const deviceId = selectedArchive.value?.deviceId || selectedParrot.value?.deviceId || ''
  if (!deviceId) { todaySleepSummary.value = { sleepDurationMinutes: 0 }; return }
  try {
    const data = await getTodaySleepSummary(deviceId)
    todaySleepSummary.value = data || { sleepDurationMinutes: 0 }
  } catch (e) {
    todaySleepSummary.value = { sleepDurationMinutes: 0 }
  }
}

async function loadLatestAlarm() {
  const deviceId = selectedArchive.value?.deviceId || selectedParrot.value?.deviceId || ''
  if (!deviceId) { latestAlarmRecord.value = null; return }
  try {
    const data = await getAlarmLogs({ limit: 1, deviceId })
    const list = Array.isArray(data) ? data : data?.list || []
    latestAlarmRecord.value = list[0] || null
  } catch (e) {
    latestAlarmRecord.value = null
  }
}

function startDashboardPolling() {
  window.clearInterval(dashboardRealtimeTimer)
  dashboardRealtimeTimer = window.setInterval(() => {
    loadRealtimeEnv()
    loadLatestAlarm()
  }, 5000)
}

function stopDashboardPolling() {
  window.clearInterval(dashboardRealtimeTimer)
  dashboardRealtimeTimer = 0
}

// 专属推荐页专用：只轮询实时环境（温湿度/烟雾会随传感器变化），不拉告警。
let careEnvTimer = 0
function startCareEnvPolling() {
  window.clearInterval(careEnvTimer)
  careEnvTimer = window.setInterval(() => loadRealtimeEnv(), 5000)
}
function stopCareEnvPolling() {
  window.clearInterval(careEnvTimer)
  careEnvTimer = 0
}

async function loadEnvironmentHistory() {
  const deviceId = selectedArchive.value?.deviceId || selectedParrot.value?.deviceId || ''
  const range = rangeToParam[activeReportRange.value] || 'daily'
  const date = reportDate.value || undefined
  environmentLoading.value = true
  environmentError.value = ''
  try {
    const data = await getEnvironmentReport(deviceId, range, date)
    environmentHistory.value = Array.isArray(data) ? data : []
    if (!environmentHistory.value.length) {
      environmentError.value = deviceId
        ? `设备 ${deviceId} 在所选周期内暂无环境记录`
        : '当前鹦鹉未绑定监测设备'
    }
  } catch (error) {
    environmentHistory.value = []
    environmentError.value = error?.message || '环境历史加载失败'
    console.warn('加载环境历史失败：', error?.message)
  } finally {
    environmentLoading.value = false
  }
}

// 切换日报/周报/月报时，如果当前日期不落在该 range 周期内，
// 才重置为默认日期；否则保留用户已选日期。
watch(
  () => activeReportRange.value,
  (range) => {
    if (!dateBelongsToRange(reportDate.value, range)) {
      reportDate.value = defaultReportDate(range)
    }
  },
  { immediate: true },
)
// 今日行为统计（进入成长报告时加载）。
const todayBehaviorStats = ref({ total: 0, stats: [] })
async function loadTodayBehavior() {
  const deviceId = selectedArchive.value?.deviceId || selectedParrot.value?.deviceId || ''
  if (!deviceId) { todayBehaviorStats.value = { total: 0, stats: [] }; return }
  try {
    const data = await getBehaviorTodayStats(deviceId)
    todayBehaviorStats.value = data || { total: 0, stats: [] }
  } catch (e) {
    console.warn('加载今日行为统计失败：', e?.message)
    todayBehaviorStats.value = { total: 0, stats: [] }
  }
}
function behaviorCountOf(label) {
  const entry = todayBehaviorStats.value?.stats?.find((s) => s.behavior === label)
  return entry?.count || 0
}

const latestWeightText = computed(() => {
  const weights = (selectedArchive.value?.weightHistory || [])
    .map((w) => Number(w.value)).filter(Number.isFinite)
  return weights.length ? `${weights[weights.length - 1]}g` : '-'
})

// 进入成长报告、切换鹦鹉或改日期时重新拉数据。
watch(
  () => [activeRoute.value, reportDate.value, selectedParrot.value?.id],
  () => {
    stopDashboardPolling()
    if (activeRoute.value === '/growth-report') {
      loadEnvironmentHistory()
      loadTodayBehavior()
      // 实时仪表盘数据
      loadRealtimeEnv()
      loadTodayEnvironmentHistory()
      loadTodaySleepSummary()
      loadLatestAlarm()
      startDashboardPolling()
    } else if (activeRoute.value === '/care-handbook' && thirdView.value === 'care-profile') {
      // 进入「专属推荐」时拉一次实时环境，并开 5s 轮询让温湿度/烟雾实时变化
      loadRealtimeEnv()
      startCareEnvPolling()
    } else {
      stopCareEnvPolling()
    }
  },
)

// 根据实际数据跨度挑选一个"好看"的桶粒度（毫秒）。
// 目标：无论数据只有 10 分钟还是 10 天，都能生成 ~8–16 个有意义的桶。
function pickBucketSize(spanMs) {
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const candidates = [
    1 * minute, 2 * minute, 5 * minute, 10 * minute, 15 * minute, 30 * minute,
    1 * hour, 2 * hour, 4 * hour, 6 * hour, 12 * hour,
    1 * day, 2 * day, 7 * day,
  ]
  // 让 spanMs 被分成 8–16 桶的最小粒度
  const ideal = spanMs / 12
  return candidates.find((c) => c >= ideal) || candidates[candidates.length - 1]
}

// 把指定指标在时间窗口内的采样聚合为等宽桶（用于折线图）。
// 桶粒度由实际数据跨度自适应决定，x 轴标签按粒度切换格式。
// 返回 { points, xAxis, latest }，无数据的桶用 null 占位（折线会断开）。
function aggregateCurve(samples, key, range) {
  const now = Date.now()
  const windowMap = { '日报': 24 * 3600 * 1000, '周报': 7 * 24 * 3600 * 1000, '月报': 30 * 24 * 3600 * 1000 }
  const windowMs = windowMap[range] || windowMap['日报']
  const start = now - windowMs

  const inWindow = samples.filter((s) => s.t >= start && s[key] != null && Number.isFinite(s[key]))
  if (!inWindow.length) return { points: [], xAxis: [], latest: null }

  const firstT = inWindow[0].t
  const lastT = inWindow[inWindow.length - 1].t
  const span = Math.max(lastT - firstT, 60 * 1000)
  const bucketSize = pickBucketSize(span)
  const bucketCount = Math.max(1, Math.ceil(span / bucketSize) + 1)

  const buckets = Array.from({ length: bucketCount }, () => [])
  inWindow.forEach((s) => {
    const idx = Math.min(bucketCount - 1, Math.floor((s.t - firstT) / bucketSize))
    buckets[idx].push(s[key])
  })

  const points = buckets.map((vals) => (vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : null))
  const latest = [...points].reverse().find((v) => v != null) ?? null

  const hourMs = 3600 * 1000
  const xAxis = buckets.map((_, i) => {
    const t = firstT + i * bucketSize
    const d = new Date(t)
    if (bucketSize < hourMs) return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    if (bucketSize < 24 * hourMs) return `${String(d.getHours()).padStart(2, '0')}:00`
    return `${d.getMonth() + 1}/${d.getDate()}`
  })

  return { points, xAxis, latest }
}

// 简易健康评分（0-100）：环境舒适度 70 分 + 体重稳定性 30 分。
// 温度舒适区 20-26℃、湿度舒适区 40-60%、粉尘越低越好。
// envHistory / weightHistory 可传入，供实时仪表盘基于今日数据计算；
// 不传时默认使用当前成长报告周期数据，兼容历史详情页。
function computeHealthScore(envHistory = environmentHistory.value, weightHistory = selectedArchive.value?.weightHistory) {
  const records = medicalRecords.value || []
  const cutoff = Date.now() - 90 * 86400000
  const recent90 = records.filter((r) => {
    const d = new Date(r.recordDate || r.date || '')
    return !Number.isNaN(d.getTime()) && d.getTime() >= cutoff
  })

  const counts = { symptom: 0, diagnosis: 0, medication: 0, recheck: 0, other: 0 }
  recent90.forEach((r) => {
    const t = r.recordType || 'other'
    if (counts[t] != null) counts[t] += 1
  })

  let total = 100
  const SCORE_RULES = [
    { value: 'symptom', weight: -8, cap: -40 },
    { value: 'diagnosis', weight: -6, cap: -30 },
    { value: 'medication', weight: -3, cap: -15 },
    { value: 'recheck', weight: 3, cap: 12 },
    { value: 'other', weight: -1, cap: -5 },
  ]

  SCORE_RULES.forEach((rule) => {
    const cnt = counts[rule.value] || 0
    const contribution = rule.weight < 0
      ? Math.max(rule.weight * cnt, rule.cap)
      : Math.min(rule.weight * cnt, rule.cap)
    total += contribution
  })

  return Math.max(0, Math.min(100, Math.round(total)))
}

function getScoreColor(score) {
  const val = Number(score) || 0
  if (val < 60) return '#ef4444' // 低于60是红色
  if (val <= 80) return '#f59e0b' // 60到80是黄色
  return '#10b981' // 高于80是绿色
}

function getTodayPoints(key) {
  const samples = toSamples(todayEnvHistory.value)
  if (key === 'temperature') return samples.map((s) => s.temperature)
  if (key === 'humidity') return samples.map((s) => s.humidity)
  if (key === 'dust') return samples.map((s) => s.dust)
  return []
}

function getSparklinePaths(key, width = 160, height = 50) {
  const points = getTodayPoints(key).filter((v) => v != null && Number.isFinite(v))
  if (points.length < 2) {
    return { line: '', area: '' }
  }
  const min = Math.min(...points)
  const max = Math.max(...points)
  const range = max - min || 1
  const n = points.length
  
  const coords = points.map((v, i) => {
    const x = (i / (n - 1)) * width
    const y = height - ((v - min) / range) * (height - 10) - 5
    return { x, y }
  })
  
  const linePath = 'M ' + coords.map((c) => `${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' L ')
  const areaPath = `${linePath} L ${width.toFixed(1)},${height.toFixed(1)} L 0,${height.toFixed(1)} Z`
  
  return { line: linePath, area: areaPath }
}

function getTodaySparkline(key, type = 'line') {
  const paths = getSparklinePaths(key)
  return type === 'line' ? paths.line : paths.area
}

function getSparklineColor(key) {
  if (key === 'temperature') return '#e28738'
  if (key === 'humidity') return '#0284c7'
  return '#8b5cf6'
}

// 实时仪表盘辅助函数。
function formatNumber(value, digits = 0) {
  if (value == null) return '--'
  const n = Number(value)
  if (!Number.isFinite(n)) return '--'
  return n.toFixed(digits).replace(/\.0$/, '')
}

function formatEnvTime(iso) {
  if (!iso) return '--'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--'
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

function getTemperatureLevel(v) {
  if (!Number.isFinite(Number(v))) return '待接入'
  if (v < 18) return '偏低'
  if (v > 30) return '偏高'
  return '适宜'
}

function getHumidityLevel(v) {
  if (!Number.isFinite(Number(v))) return '待接入'
  if (v < 40) return '偏低'
  if (v > 70) return '偏高'
  return '适宜'
}

function getDustLevel(v, level = '') {
  if (level === '高' || String(level).toLowerCase().includes('high')) return '高'
  if (level === '中' || String(level).toLowerCase().includes('medium')) return '中'
  const n = Number(v)
  if (!Number.isFinite(n)) return '待接入'
  if (n >= 80) return '高'
  if (n >= 35) return '中'
  return '低'
}

function formatSleepDuration(minutes) {
  if (!minutes || minutes <= 0) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分`
}

function readCachedUser() {
  if (typeof localStorage === 'undefined') return null
  try {
    const raw = localStorage.getItem('parrotAuthUser')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const cachedUser = readCachedUser()
const account = ref({
  avatarParrotId: cachedUser?.avatarParrotId || userProfile.avatarParrotId,
  username: cachedUser?.username || userProfile.username,
  userId: cachedUser?.userId || userProfile.userId,
  phone: cachedUser?.phone || userProfile.phone,
  email: cachedUser?.email || userProfile.email,
  location: cachedUser?.location || userProfile.location,
  phoneBound: Boolean(cachedUser?.phone || userProfile.phone),
  emailBound: Boolean(cachedUser?.email || userProfile.email),
  avatarImage: localStorage.getItem('parrotUserAvatar') || cachedUser?.avatarImage || '',
})
const loginUser = ref(cachedUser ? { ...cachedUser } : null)
const isSettingsEditing = ref(false)
const settingsDraft = ref({ ...account.value })
const passwordChanging = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const passwordMessage = ref('')
const apiKeyDraft = ref({ qwenApiKey: '', deepseekApiKey: '' })
const apiKeySaving = ref(false)
const apiKeyMessage = ref('')
// --- 头像裁剪弹窗状态 ---
const showAvatarCropDialog = ref(false)
const pendingAvatarSrc = ref('')
const avatarCropperRef = ref(null)
const qqWhitelistDraft = ref('')
const qqWhitelistSaving = ref(false)
const qqWhitelistMessage = ref('')
const weightDraft = ref('')
const capturedPhotos = ref([])
const basePhotoRecords = ref([...photoRecords])
const petAvatarMediaMap = ref({})
const petAvatarPhotoCache = ref({})
const careApiReady = ref(false)
const preferenceApiReady = ref(false)
const isAuthenticated = ref(Boolean(localStorage.getItem('parrotAuthToken')))
const gallerySelectMode = ref(false)
const selectedPhotoKeys = ref([])
const reportToastVisible = ref(false)
const alarmToast = ref('')
const notificationEnabled = ref(true)
const permissionEnabled = ref(true)
const systemPrefs = ref({
  language: 'zh',
  theme: 'light',
  fontFamily: 'default',
  fontSize: 16,
  fontColor: 'black',
})

const i18n = {
  zh: {
    cards: {
      archive: ['宠物档案', '头像模型、资料、体重与相册'],
      growth: ['成长报告', '日报周报月报与健康曲线'],
      settings: ['用户设置', '头像、账号、位置与权限'],
      medical: ['医疗助手', '智能问诊、附近医院与病历'],
      ledger: ['记账本', '按时间记录饲养花费'],
      handbook: ['饲养手册', '教程库、专属推荐、拍照识鸟'],
      monitor: ['实时视频通话', ''],
    },
    language: '语言选项',
    chinese: '中文',
    english: 'English',
    spanish: 'Español',
    japanese: '日本語',
    theme: '主题',
    day: '白天',
    night: '夜间',
    font: '字体',
    defaultFont: '默认',
    fontSize: '字号',
    color: '颜色',
    black: '黑色',
    white: '白色',
    phone: '手机绑定',
    email: '绑定邮箱',
    bound: '已绑定',
    unbound: '未绑定',
    change: '更换',
    confirm: '确定',
    cancel: '取消',
    edit: '编辑',
    save: '保存',
    inputPhone: '输入新的手机号',
    inputEmail: '输入新的邮箱',
    permissions: '通知设置与设备权限',
    about: '关于我们',
    system: '系统信息',
    version: '版本号',
    daily: '日报',
    weekly: '周报',
    monthly: '月报',
    gaugeHint: '点击查看仪表盘',
    currentLevel: '当前程度',
    connected: '已连接后端实时数据',
    // fallback: '后端未连接，当前为保底模拟值',
    currentParrot: '当前鹦鹉',
    username: '用户名',
    userId: '用户 ID',
    location: '位置信息',
    logout: '退出登录',
    deleteAccount: '注销账号',
    deleteAccountTitle: '确认注销账号',
    deleteAccountWarning: '注销后，您的宠物档案、病历、记账、照片等数据将被永久删除，无法恢复。',
    deleteAccountConfirm: '确认注销',
    changePassword: '修改密码',
    currentPassword: '当前密码',
    newPassword: '新密码',
    confirmNewPassword: '确认新密码',
    passwordMismatch: '两次输入的新密码不一致',
    wrongPassword: '当前密码错误',
    passwordChanged: '密码修改成功',
    bindDevice: '绑定设备',
    noDevice: '暂不绑定',
    deleteProfile: '删除档案',
    deleteProfileTitle: '确认删除档案',
    deleteProfileWarning: '删除后，该鹦鹉的体重、病历、记账、照片等所有数据将一并被删除，无法恢复。',
    deleteProfileConfirm: '确认删除',
    apiKeySettings: 'API Key 设置',
    qwenApiKey: '通义千问 API Key（视觉识别）',
    deepseekApiKey: 'DeepSeek API Key（QQ 机器人）',
    apiKeySaved: 'API Key 已保存',
    apiKeySaveError: '保存失败，请重试',
    connectQq: '接入 QQ',
    qqWhitelistLabel: '已接入白名单的 QQ 号',
    qqWhitelistPlaceholder: '请输入要允许交互的 QQ 号，多个用英文逗号分隔',
    qqWhitelistSaved: 'QQ 白名单已保存',
    qqWhitelistSaveError: '保存白名单失败，请重试',
  },
  en: {
    cards: {
      archive: ['Pet Profiles', 'Avatar, profile, weight and album'],
      growth: ['Growth Report', 'Daily, weekly, monthly health curves'],
      settings: ['User Settings', 'Avatar, account, location and permissions'],
      medical: ['Medical Helper', 'Triage, nearby hospitals and records'],
      ledger: ['Ledger', 'Track parrot-care expenses'],
      handbook: ['Care Handbook', 'Tutorials, care profile and bird ID'],
      monitor: ['Live Video Call', ''],
    },
    language: 'Language',
    chinese: 'Chinese',
    english: 'English',
    spanish: 'Spanish',
    japanese: 'Japanese',
    theme: 'Theme',
    day: 'Day',
    night: 'Night',
    font: 'Font',
    defaultFont: 'Default',
    fontSize: 'Size',
    color: 'Color',
    black: 'Black',
    white: 'White',
    phone: 'Phone',
    email: 'Email',
    bound: 'Bound',
    unbound: 'Hidden',
    change: 'Change',
    confirm: 'Confirm',
    cancel: 'Cancel',
    edit: 'Edit',
    save: 'Save',
    inputPhone: 'Enter 11-digit phone',
    inputEmail: 'Enter new email',
    permissions: 'Notifications and Device Permissions',
    about: 'About Us',
    system: 'System Info',
    version: 'Version',
    daily: 'Daily',
    weekly: 'Weekly',
    monthly: 'Monthly',
    gaugeHint: 'Open gauge',
    currentLevel: 'Level',
    connected: 'Live backend data',
    fallback: 'Backend offline, using fallback value',
    currentParrot: 'Current Parrot',
    username: 'Username',
    userId: 'User ID',
    location: 'Location',
    logout: 'Log Out',
    deleteAccount: 'Delete Account',
    deleteAccountTitle: 'Confirm Account Deletion',
    deleteAccountWarning: 'This will permanently delete your pet profiles, medical records, ledger entries, photos and other data. This action cannot be undone.',
    deleteAccountConfirm: 'Confirm Delete',
    changePassword: 'Change Password',
    currentPassword: 'Current Password',
    newPassword: 'New Password',
    confirmNewPassword: 'Confirm New Password',
    passwordMismatch: 'The two new passwords do not match',
    wrongPassword: 'Current password is incorrect',
    passwordChanged: 'Password changed successfully',
    bindDevice: 'Bind Device',
    noDevice: 'Not bound',
    deleteProfile: 'Delete Profile',
    deleteProfileTitle: 'Confirm Profile Deletion',
    deleteProfileWarning: 'This will permanently delete this parrot profile and all its weight, medical, ledger and photo records. This action cannot be undone.',
    deleteProfileConfirm: 'Confirm Delete',
    apiKeySettings: 'API Key Settings',
    qwenApiKey: 'Qwen API Key (Vision Recognition)',
    deepseekApiKey: 'DeepSeek API Key (QQ Bot)',
    apiKeySaved: 'API Key saved',
    apiKeySaveError: 'Save failed, please try again',
    connectQq: 'Connect QQ',
    qqWhitelistLabel: 'Whitelisted QQ Numbers',
    qqWhitelistPlaceholder: 'Enter QQ numbers to whitelist, separated by commas',
    qqWhitelistSaved: 'QQ whitelist saved',
    qqWhitelistSaveError: 'Save failed, please try again',
  },
  es: {
    cards: {
      archive: ['Perfiles', 'Avatar, datos, peso y álbum'],
      growth: ['Informe', 'Curvas diarias, semanales y mensuales'],
      settings: ['Ajustes', 'Avatar, cuenta, ubicación y permisos'],
      medical: ['Asistente médico', 'Consulta, hospitales y registros'],
      ledger: ['Gastos', 'Registra gastos de cuidado'],
      handbook: ['Manual', 'Tutoriales, perfil de cuidado e identificación'],
      monitor: ['Videollamada', ''],
    },
    language: 'Idioma',
    chinese: 'Chino',
    english: 'Inglés',
    spanish: 'Español',
    japanese: 'Japonés',
    theme: 'Tema',
    day: 'Día',
    night: 'Noche',
    font: 'Fuente',
    defaultFont: 'Predeterminada',
    fontSize: 'Tamaño',
    color: 'Color',
    black: 'Negro',
    white: 'Blanco',
    phone: 'Teléfono',
    email: 'Correo',
    bound: 'Vinculado',
    unbound: 'Oculto',
    change: 'Cambiar',
    confirm: 'Confirmar',
    cancel: 'Cancelar',
    edit: 'Editar',
    save: 'Guardar',
    inputPhone: 'Introduce 11 dígitos',
    inputEmail: 'Introduce correo',
    permissions: 'Notificaciones y permisos',
    about: 'Sobre nosotros',
    system: 'Información del sistema',
    version: 'Versión',
    daily: 'Diario',
    weekly: 'Semanal',
    monthly: 'Mensual',
    gaugeHint: 'Ver indicador',
    currentLevel: 'Nivel',
    connected: 'Datos en vivo',
    fallback: 'Sin backend, usando valor local',
    currentParrot: 'Loro actual',
    username: 'Usuario',
    userId: 'ID de usuario',
    location: 'Ubicación',
    logout: 'Cerrar sesión',
    deleteAccount: 'Eliminar cuenta',
    deleteAccountTitle: 'Confirmar eliminación de cuenta',
    deleteAccountWarning: 'Se eliminarán permanentemente los perfiles de mascotas, registros médicos, gastos, fotos y otros datos. Esta acción no se puede deshacer.',
    deleteAccountConfirm: 'Confirmar eliminación',
    changePassword: 'Cambiar contraseña',
    currentPassword: 'Contraseña actual',
    newPassword: 'Nueva contraseña',
    confirmNewPassword: 'Confirmar nueva contraseña',
    passwordMismatch: 'Las dos contraseñas nuevas no coinciden',
    wrongPassword: 'La contraseña actual es incorrecta',
    passwordChanged: 'Contraseña cambiada correctamente',
    bindDevice: ' Vincular dispositivo',
    noDevice: 'Sin vincular',
    deleteProfile: 'Eliminar perfil',
    deleteProfileTitle: 'Confirmar eliminación del perfil',
    deleteProfileWarning: 'Se eliminará permanentemente este perfil de loro y todos sus registros de peso, médicos, gastos y fotos. Esta acción no se puede deshacer.',
    deleteProfileConfirm: 'Confirmar eliminación',
    apiKeySettings: 'Configuración de API Key',
    qwenApiKey: 'Qwen API Key (Reconocimiento visual)',
    deepseekApiKey: 'DeepSeek API Key (Bot QQ)',
    apiKeySaved: 'API Key guardada',
    apiKeySaveError: 'Error al guardar, inténtelo de nuevo',
    connectQq: 'Conectar QQ',
    qqWhitelistLabel: 'Números de QQ en lista blanca',
    qqWhitelistPlaceholder: 'Ingrese números de QQ, separados por comas',
    qqWhitelistSaved: 'Lista blanca de QQ guardada',
    qqWhitelistSaveError: 'Error al guardar, inténtelo de nuevo',
  },
  ja: {
    cards: {
      archive: ['ペット記録', 'アバター、情報、体重、アルバム'],
      growth: ['成長レポート', '日報・週報・月報と健康曲線'],
      settings: ['ユーザー設定', 'アバター、アカウント、位置、権限'],
      medical: ['医療サポート', '問診、近くの病院、記録'],
      ledger: ['家計簿', '飼育費用を記録'],
      handbook: ['飼育ガイド', '教程、専用推奨、鳥識別'],
      monitor: ['ライブ通話', ''],
    },
    language: '言語',
    chinese: '中国語',
    english: '英語',
    spanish: 'スペイン語',
    japanese: '日本語',
    theme: 'テーマ',
    day: '昼',
    night: '夜',
    font: 'フォント',
    defaultFont: '標準',
    fontSize: 'サイズ',
    color: '色',
    black: '黒',
    white: '白',
    phone: '電話番号',
    email: 'メール',
    bound: '連携済み',
    unbound: '非表示',
    change: '変更',
    confirm: '確定',
    cancel: 'キャンセル',
    edit: '編集',
    save: '保存',
    inputPhone: '11桁の番号',
    inputEmail: 'メールを入力',
    permissions: '通知とデバイス権限',
    about: '私たちについて',
    system: 'システム情報',
    version: 'バージョン',
    daily: '日報',
    weekly: '週報',
    monthly: '月報',
    gaugeHint: 'メーターを表示',
    currentLevel: '現在レベル',
    connected: 'リアルタイム接続済み',
    fallback: '未接続、代替値を表示',
    currentParrot: '現在のインコ',
    username: 'ユーザー名',
    userId: 'ユーザー ID',
    location: '位置情報',
    logout: 'ログアウト',
    deleteAccount: 'アカウント削除',
    deleteAccountTitle: 'アカウント削除の確認',
    deleteAccountWarning: 'ペットのプロフィール、病历、记账、写真などのデータが永久に削除されます。この操作は元に戻せません。',
    deleteAccountConfirm: '削除を確認',
    changePassword: 'パスワード変更',
    currentPassword: '現在のパスワード',
    newPassword: '新しいパスワード',
    confirmNewPassword: '新しいパスワードの確認',
    passwordMismatch: '新しいパスワードが一致しません',
    wrongPassword: '現在のパスワードが正しくありません',
    passwordChanged: 'パスワードを変更しました',
    bindDevice: 'デバイスを紐付ける',
    noDevice: '紐付けなし',
    deleteProfile: '記録を削除',
    deleteProfileTitle: '記録の削除確認',
    deleteProfileWarning: 'このインコの記録と体重、病历、家計簿、写真のすべてのデータが永久に削除されます。この操作は元に戻せません。',
    deleteProfileConfirm: '削除を確認',
    apiKeySettings: 'API Key 設定',
    qwenApiKey: 'Qwen API Key（視覚認識）',
    deepseekApiKey: 'DeepSeek API Key（QQ ボット）',
    apiKeySaved: 'API Key を保存しました',
    apiKeySaveError: '保存に失敗しました。再試行してください',
    connectQq: 'QQ 連携',
    qqWhitelistLabel: 'ホワイトリストに登録された QQ 番号',
    qqWhitelistPlaceholder: 'ホワイトリストに登録する QQ 番号を入力してください（カンマ区切り）',
    qqWhitelistSaved: 'QQ ホワイトリストを保存しました',
    qqWhitelistSaveError: '保存に失敗しました。再試行してください',
  },
}

const uiCopy = {
  zh: {
    changeSuffix: '变化',
    reportToast: '新的成长报告已出炉~',
    selectPhotos: '多选',
    cancelSelect: '取消多选',
    exportSelected: '导出所选',
    deletePhotos: '删除所选',
    selectAll: '全选',
    savePhoto: '另存为',
    noSelection: '请选择照片',
    snapshotPhoto: '监控截图',
    photoTitles: ['最兴奋照片', '睡觉照片', '吃饭照片', '站立照片', '扇翅膀照片', '大叫照片'],
    modules: {
      diagnosis: ['智能问诊', '填写外在表现问卷，获得初步风险判断'],
      hospitals: ['附近医院', '查看可治疗异宠的医院和联系方式'],
      records: ['病历', '按时间记录就诊、用药和复查事项'],
      health: ['健康分析', '基于病历记录的健康评分与趋势可视化'],
      tutorials: ['教程库', '新手喂养、剪羽、药浴、清洁教程'],
      'care-profile': ['专属推荐', '基于当前鹦鹉品种的专属饲养方案与环境适配评分'],
      'bird-id': ['拍照识鹦鹉', '上传或拍照识别种类与行为'],
    },
    reportStats: ['健康评分', '睡眠时长', '鸣叫次数', '进食次数', '排泄次数'],
    reportRecords: {
      photos: ['照片记录', '最兴奋照片 4 张，睡觉照片 6 张'],
      recordings: ['录音', '学舌 5 段，歌曲练习 3 次'],
      risk: ['健康风险提醒', '下午羽粉偏高，建议通风 20 分钟'],
    },
    tutorials: [
      ['新手到家 7 天照护', '新手喂养', '10 分钟'],
      ['安全剪羽与替代训练', '剪羽教程', '12 分钟'],
      ['药浴前后的保温要点', '药浴教程', '8 分钟'],
      ['笼舍日常清洁与消毒', '清洁教程', '9 分钟'],
      ['判断鹦鹉是否健康', '健康观察', '8 分钟'],
      ['夏季防暑与冬季保暖', '环境管理', '9 分钟'],
      ['鹦鹉常见有毒食物清单', '食物安全', '7 分钟'],
      ['换羽期护理要点', '换羽护理', '8 分钟'],
      ['啄羽问题排查', '行为问题', '10 分钟'],
      ['日常喂养与食谱搭配', '喂养指南', '9 分钟'],
      ['笼舍与玩具布置', '环境布置', '8 分钟'],
      ['训练入门：上手与回笼', '训练教程', '10 分钟'],
      ['外出与外出笼使用', '外出安全', '8 分钟'],
      ['夜间光照与睡眠管理', '作息管理', '7 分钟'],
      ['修剪指甲与喙部护理', '日常护理', '7 分钟'],
      ['急救箱与常见意外处理', '急救常识', '10 分钟'],
      ['鹦鹉情绪与肢体语言', '行为理解', '8 分钟'],
      ['新手常见误区', '避坑指南', '9 分钟'],
      ['饮水与补钙常识', '营养健康', '7 分钟'],
      ['全年护理日历', '季节管理', '8 分钟'],
    ],
    curves: {
      temperature: ['温度曲线', '环境温度'],
      humidity: ['湿度曲线', '环境湿度'],
      dust: ['粉尘曲线', '羽粉浓度'],
      weight: ['体重变化曲线', '体重'],
    },
    labels: {
      stable: '稳定', temperature: '温度', humidity: '湿度', dust: '粉尘浓度', low: '低', mid: '中', high: '高', suitable: '适宜', lowState: '偏低', highState: '偏高',
      hourlyTrend: '小时趋势', trend: '趋势', notifications: '通知设置', devicePermissions: '设备权限',
      tutorialSearch: '搜索教程关键字', birdTitle: '鹦鹉识别（种类+行为）', choosePhoto: '选择 / 拍照',
      chooseFile: '选择文件', noFile: '未选择文件', recognize: '识别行为', recognizing: '识别中…',
      birdAlt: '待识别鹦鹉', birdResult: '识别结果', chooseBirdFirst: '请先选择或拍摄一张鹦鹉图片',
      recognizeFail: '识别失败', detectedParrot: '检测到鹦鹉', noParrot: '未检测到鹦鹉',
      species: '种类', behavior: '行为', confidence: '置信度', behaviorUnavailable: '行为识别未启用或未出结果',
      submit: '提交', refresh: '刷新', searchRecord: '搜索病历关键字', newRecord: '填写一条新的病历记录',
      add: '新增', modify: '修改', delete: '删除', playRecording: '播放录音',
    },
  },
  en: {
    changeSuffix: ' change',
    reportToast: 'A new growth report is ready~',
    selectPhotos: 'Select',
    cancelSelect: 'Cancel',
    exportSelected: 'Export selected',
    deletePhotos: 'Delete selected',
    selectAll: 'Select all',
    savePhoto: 'Save as',
    noSelection: 'Select photos first',
    snapshotPhoto: 'Monitor screenshot',
    photoTitles: ['Excited photo', 'Sleep photo', 'Meal photo', 'Standing photo', 'Wing photo', 'Calling photo'],
    modules: {
      diagnosis: ['Smart Triage', 'Fill in symptoms and get an initial risk suggestion'],
      hospitals: ['Nearby Hospitals', 'Find exotic-pet hospitals and contacts'],
      records: ['Medical Records', 'Track visits, medicine and follow-ups'],
      health: ['Health Analysis', 'Health score and trend from medical records'],
      tutorials: ['Tutorial Library', 'Beginner care, trimming, bath and cleaning guides'],
      'care-profile': ['Care Profile', 'Species-specific care plan and environment match score'],
      'bird-id': ['Bird ID', 'Upload or take a photo to identify species'],
    },
    reportStats: ['Health Score', 'Sleep Duration', 'Calls', 'Meals', 'Droppings'],
    reportRecords: {
      photos: ['Photo Records', '4 excited photos, 6 sleep photos'],
      recordings: ['Recordings', '5 mimicry clips, 3 song practices'],
      risk: ['Health Risk Alert', 'Dust is high this afternoon; ventilate for 20 minutes'],
    },
    tutorials: [
      ['First 7 Days at Home', 'Beginner care', '10 min'],
      ['Safe Wing Trimming & Training', 'Wing care', '12 min'],
      ['Warmth Before/After Medicated Bath', 'Bath care', '8 min'],
      ['Daily Cage Cleaning & Disinfection', 'Cleaning', '9 min'],
      ['How to Tell If Your Parrot Is Healthy', 'Health check', '8 min'],
      ['Summer Heat & Winter Warmth', 'Environment', '9 min'],
      ['Common Toxic Foods for Parrots', 'Food safety', '7 min'],
      ['Molting Season Care', 'Molting', '8 min'],
      ['Diagnosing Feather Plucking', 'Behavior', '10 min'],
      ['Daily Diet & Recipe Guide', 'Feeding', '9 min'],
      ['Cage & Toy Setup', 'Setup', '8 min'],
      ['Training Basics: Step Up & Recall', 'Training', '10 min'],
      ['Travel & Travel Cage Use', 'Outing', '8 min'],
      ['Night Lighting & Sleep Management', 'Routine', '7 min'],
      ['Nail & Beak Care', 'Daily care', '7 min'],
      ['First Aid Kit & Common Accidents', 'First aid', '10 min'],
      ['Parrot Body Language & Emotions', 'Behavior', '8 min'],
      ['Common Beginner Mistakes', 'Tips', '9 min'],
      ['Water & Calcium Basics', 'Nutrition', '7 min'],
      ['Year-Round Care Calendar', 'Seasonal', '8 min'],
    ],
    curves: {
      temperature: ['Temperature Curve', 'Ambient temperature'],
      humidity: ['Humidity Curve', 'Ambient humidity'],
      dust: ['Dust Curve', 'Feather dust'],
      weight: ['Weight Curve', 'Weight'],
    },
    labels: {
      stable: 'Stable', temperature: 'Temperature', humidity: 'Humidity', dust: 'Dust', low: 'Low', mid: 'Medium', high: 'High', suitable: 'Good', lowState: 'Low', highState: 'High',
      hourlyTrend: 'Hourly trend', trend: 'trend', notifications: 'Notifications', devicePermissions: 'Device permissions',
      tutorialSearch: 'Search tutorials', birdTitle: 'Parrot ID (species + behavior)', choosePhoto: 'Choose / take photo',
      chooseFile: 'Choose file', noFile: 'No file selected', recognize: 'Identify Behavior', recognizing: 'Identifying...',
      birdAlt: 'Parrot to identify', birdResult: 'Recognition Result', chooseBirdFirst: 'Please choose or take a parrot photo first',
      recognizeFail: 'Recognition failed', detectedParrot: 'Parrot detected', noParrot: 'No parrot detected',
      species: 'Species', behavior: 'Behavior', confidence: 'Confidence', behaviorUnavailable: 'Behavior recognition is unavailable or has no result',
      submit: 'Submit', refresh: 'Refresh', searchRecord: 'Search medical records', newRecord: 'Write a new medical record',
      add: 'Add', modify: 'Edit', delete: 'Delete', playRecording: 'Play recording',
    },
  },
  es: {
    changeSuffix: ' cambio',
    reportToast: 'Nuevo informe de crecimiento listo~',
    selectPhotos: 'Seleccionar',
    cancelSelect: 'Cancelar',
    exportSelected: 'Exportar',
    deletePhotos: 'Eliminar',
    selectAll: 'Todo',
    savePhoto: 'Guardar',
    noSelection: 'Selecciona fotos',
    snapshotPhoto: 'Captura de monitor',
    photoTitles: ['Foto emocionada', 'Foto durmiendo', 'Foto comiendo', 'Foto de pie', 'Foto de alas', 'Foto gritando'],
    modules: {
      diagnosis: ['Consulta inteligente', 'Completa síntomas y recibe una sugerencia inicial'],
      hospitals: ['Hospitales cercanos', 'Busca hospitales para mascotas exóticas y contactos'],
      records: ['Historial médico', 'Registra visitas, medicinas y revisiones'],
      health: ['Análisis de salud', 'Puntuación y tendencia desde el historial'],
      tutorials: ['Biblioteca', 'Guías de cuidado, corte, baño y limpieza'],
      'care-profile': ['Perfil de cuidado', 'Plan específico y puntuación de entorno'],
      'bird-id': ['Identificar ave', 'Sube o toma una foto para identificar especies'],
    },
    reportStats: ['Salud', 'Sueño', 'Llamadas', 'Comidas', 'Excrementos'],
    reportRecords: {
      photos: ['Fotos', '4 fotos emocionadas, 6 fotos durmiendo'],
      recordings: ['Grabaciones', '5 clips de imitación, 3 canciones'],
      risk: ['Riesgo de salud', 'Polvo alto por la tarde; ventila 20 minutos'],
    },
    tutorials: [
      ['Primeros 7 días en casa', 'Cuidado inicial', '10 min'],
      ['Corte seguro de alas y alternativas', 'Entrenamiento', '12 min'],
      ['Calor antes y después del baño medicinal', 'Baño', '8 min'],
      ['Limpieza y desinfección diaria', 'Limpieza', '9 min'],
      ['Cómo saber si tu loro está sano', 'Salud', '8 min'],
      ['Calor del verano y frío del invierno', 'Ambiente', '9 min'],
      ['Alimentos tóxicos comunes', 'Seguridad', '7 min'],
      ['Cuidado durante la muda', 'Muda', '8 min'],
      ['Diagnóstico del arrancamiento de plumas', 'Conducta', '10 min'],
      ['Dieta diaria y recetas', 'Alimentación', '9 min'],
      ['Jaula y juguetes', 'Montaje', '8 min'],
      ['Entrenamiento: subir y volver', 'Entrenamiento', '10 min'],
      ['Salidas y jaula de transporte', 'Paseos', '8 min'],
      ['Luz nocturna y sueño', 'Rutina', '7 min'],
      ['Uñas y pico', 'Cuidado diario', '7 min'],
      ['Botiquín y accidentes comunes', 'Primeros auxilios', '10 min'],
      ['Lenguaje corporal del loro', 'Conducta', '8 min'],
      ['Errores comunes de principiantes', 'Consejos', '9 min'],
      ['Agua y calcio', 'Nutrición', '7 min'],
      ['Calendario anual de cuidado', 'Estacional', '8 min'],
    ],
    curves: {
      temperature: ['Curva de temperatura', 'Temperatura ambiental'],
      humidity: ['Curva de humedad', 'Humedad ambiental'],
      dust: ['Curva de polvo', 'Polvo de plumas'],
      weight: ['Curva de peso', 'Peso'],
    },
    labels: {
      stable: 'Estable', temperature: 'Temperatura', humidity: 'Humedad', dust: 'Polvo', low: 'Bajo', mid: 'Medio', high: 'Alto', suitable: 'Adecuado', lowState: 'Bajo', highState: 'Alto',
      hourlyTrend: 'Tendencia por hora', trend: 'tendencia', notifications: 'Notificaciones', devicePermissions: 'Permisos del dispositivo',
      tutorialSearch: 'Buscar tutoriales', birdTitle: 'Identificación de loro (especie + conducta)', choosePhoto: 'Elegir / tomar foto',
      chooseFile: 'Elegir archivo', noFile: 'Sin archivo', recognize: 'Identificar conducta', recognizing: 'Identificando...',
      birdAlt: 'Loro para identificar', birdResult: 'Resultado', chooseBirdFirst: 'Elige o toma una foto del loro primero',
      recognizeFail: 'Error de reconocimiento', detectedParrot: 'Loro detectado', noParrot: 'No se detectó loro',
      species: 'Especie', behavior: 'Conducta', confidence: 'Confianza', behaviorUnavailable: 'Reconocimiento de conducta no disponible o sin resultado',
      submit: 'Enviar', refresh: 'Actualizar', searchRecord: 'Buscar historiales', newRecord: 'Escribe un nuevo historial',
      add: 'Añadir', modify: 'Modificar', delete: 'Eliminar', playRecording: 'Reproducir grabación',
    },
  },
  ja: {
    changeSuffix: '変化',
    reportToast: '新しい成長レポートができました~',
    selectPhotos: '複数選択',
    cancelSelect: '選択解除',
    exportSelected: '選択を書き出し',
    deletePhotos: '選択を削除',
    selectAll: '全選択',
    savePhoto: '保存',
    noSelection: '写真を選択してください',
    snapshotPhoto: 'モニター画像',
    photoTitles: ['興奮写真', '睡眠写真', '食事写真', '立ち姿写真', '羽ばたき写真', '鳴き声写真'],
    modules: {
      diagnosis: ['スマート問診', '外見の様子を入力して初期リスクを確認'],
      hospitals: ['近くの病院', 'エキゾチックアニマル対応病院と連絡先'],
      records: ['カルテ', '診察、投薬、再診を時系列で記録'],
      health: ['健康分析', 'カルテに基づく健康スコアと傾向可視化'],
      tutorials: ['チュートリアル', '初心者飼育、羽切り、薬浴、清掃ガイド'],
      'care-profile': ['専用推奨', '種類別ケア計画と環境適合スコア'],
      'bird-id': ['鳥識別', '写真をアップロードして種類を識別'],
    },
    reportStats: ['健康スコア', '睡眠時間', '鳴き声回数', '食事回数', '排泄回数'],
    reportRecords: {
      photos: ['写真記録', '興奮写真 4 枚、睡眠写真 6 枚'],
      recordings: ['録音', '物まね 5 本、歌練習 3 回'],
      risk: ['健康リスク通知', '午後の羽粉が高め、20 分換気を推奨'],
    },
    tutorials: [
      ['お迎え後7日間のケア', '初心者飼育', '10分'],
      ['安全な羽切りと代替トレーニング', '羽切り', '12分'],
      ['薬浴前後の保温ポイント', '薬浴', '8分'],
      ['ケージの日常清掃と消毒', '清掃', '9分'],
      ['インコの健康状態を見極める', '健康観察', '8分'],
      ['夏の暑さ対策と冬の保温', '環境管理', '9分'],
      ['インコの有毒食物リスト', '食べ物安全', '7分'],
      ['換羽期のケア', '換羽', '8分'],
      ['抜羽問題の診断', '行動', '10分'],
      ['日常の餌とレシピ', '給餌', '9分'],
      ['ケージとおもちゃの配置', '設営', '8分'],
      ['訓練入門：ステップアップと帰巣', '訓練', '10分'],
      ['外出とキャリーの使い方', 'お出かけ', '8分'],
      ['夜間の照明と睡眠管理', 'ルーティン', '7分'],
      ['爪切りとくちばしケア', '日常ケア', '7分'],
      ['救急箱と事故対応', '応急処置', '10分'],
      ['インコのボディランゲージ', '行動', '8分'],
      ['初心者によくある誤解', 'コツ', '9分'],
      ['水とカルシウムの基礎', '栄養', '7分'],
      ['年間ケアカレンダー', '季節管理', '8分'],
    ],
    curves: {
      temperature: ['温度曲線', '環境温度'],
      humidity: ['湿度曲線', '環境湿度'],
      dust: ['粉じん曲線', '羽粉濃度'],
      weight: ['体重変化曲線', '体重'],
    },
    labels: {
      stable: '安定', temperature: '温度', humidity: '湿度', dust: '粉じん濃度', low: '低', mid: '中', high: '高', suitable: '適切', lowState: '低め', highState: '高め',
      hourlyTrend: '時間別推移', trend: '推移', notifications: '通知設定', devicePermissions: 'デバイス権限',
      tutorialSearch: '教程キーワード検索', birdTitle: 'インコ識別（種類＋行動）', choosePhoto: '選択 / 撮影',
      chooseFile: 'ファイル選択', noFile: '未選択', recognize: '行動を識別', recognizing: '識別中…',
      birdAlt: '識別するインコ', birdResult: '識別結果', chooseBirdFirst: '先にインコの写真を選択または撮影してください',
      recognizeFail: '識別に失敗しました', detectedParrot: 'インコを検出', noParrot: 'インコ未検出',
      species: '種類', behavior: '行動', confidence: '信頼度', behaviorUnavailable: '行動識別は未有効、または結果がありません',
      submit: '送信', refresh: '更新', searchRecord: 'カルテを検索', newRecord: '新しいカルテを記入',
      add: '追加', modify: '修正', delete: '削除', playRecording: '録音を再生',
    },
  },
}

const activeView = computed(() => detailViews[activeRoute.value])
const reportCurveSet = computed(() => reportCurveSets[activeReportRange.value] || reportCurveSets.月报 || { curves: [] })
const text = computed(() => i18n[systemPrefs.value.language] || i18n.zh)
const ui = computed(() => uiCopy[systemPrefs.value.language] || uiCopy.zh)
// 把体重记录的 time/measuredAt 解析成毫秒时间戳（仅用于时间窗口过滤与排序）。
function weightTimeMs(item) {
  if (item.measuredAt) {
    const ms = new Date(item.measuredAt).getTime()
    if (Number.isFinite(ms)) return ms
  }
  if (item.time && /^\d{2}-\d{2}$/.test(item.time)) {
    // 短日期 "MM-DD"，补上当前年份
    return new Date(`${new Date().getFullYear()}-${item.time}T00:00:00`).getTime()
  }
  if (item.time) {
    const ms = new Date(item.time).getTime()
    if (Number.isFinite(ms)) return ms
  }
  return null
}

// 后端返回的 {time, temperature, humidity, dust} 转成 aggregateCurve 需要的 {t, ...} 格式。
function toSamples(history) {
  return history
    .map((p) => {
      const t = p.time ? new Date(p.time).getTime() : NaN
      return {
        t: Number.isFinite(t) ? t : null,
        temperature: p.temperature != null ? Number(p.temperature) : null,
        humidity: p.humidity != null ? Number(p.humidity) : null,
        dust: p.dust != null ? Number(p.dust) : null,
      }
    })
    .filter((s) => s.t != null)
    .sort((a, b) => a.t - b.t)
}

// 成长报告曲线：直接用后端 /environment/report 返回的点（已按周期聚合好，不再二次平均）。
// daily=每小时一点、weekly/monthly=每天一点。数值即数据库值，1 位小数，无数据时曲线区为空。
function formatReportTime(time, range) {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return range === '日报' ? `${pad(d.getHours())}:00` : `${d.getMonth() + 1}/${d.getDate()}`
}
// 曲线 tooltip 数值格式化：温度 1 位小数，湿度/粉尘/体重取整。
function formatPointValue(point, unit) {
  if (point == null || !Number.isFinite(Number(point))) return '-'
  if (unit === '℃') return Number(point).toFixed(1)
  return Math.round(Number(point))
}
function reportPeriodRange(range, dateStr) {
  const ref = dateStr ? new Date(`${dateStr}T00:00:00`) : new Date(`${defaultReportDate(range)}T00:00:00`)
  if (Number.isNaN(ref.getTime())) return { start: -Infinity, end: Infinity }
  const start = new Date(ref); start.setHours(0, 0, 0, 0)
  const end = new Date(ref); end.setHours(23, 59, 59, 999)
  if (range === '周报') {
    const dow = (ref.getDay() + 6) % 7 // 周一=0..周日=6
    start.setDate(ref.getDate() - dow)
    end.setDate(start.getDate() + 6); end.setHours(23, 59, 59, 999)
  } else if (range === '月报') {
    start.setDate(1)
    end.setMonth(start.getMonth() + 1, 0); end.setHours(23, 59, 59, 999)
  }
  return { start: start.getTime(), end: end.getTime() }
}

// 判断 dateStr（YYYY-MM-DD）是否落在 range 对应的自然周期内。
// 用于日期选择器保留用户已选日期，避免切换 range 时被无谓重置。
function dateBelongsToRange(dateStr, range) {
  if (!dateStr) return false
  const { start, end } = reportPeriodRange(range, dateStr)
  const ref = new Date(`${dateStr}T00:00:00`).getTime()
  return ref >= start && ref <= end
}
const lastValue = (arr) => {
  for (let i = arr.length - 1; i >= 0; i--) {
    if (arr[i] != null && Number.isFinite(Number(arr[i]))) return Number(arr[i])
  }
  return null
}
const reportCurves = computed(() => {
  const rows = environmentHistory.value
  if (!rows.length) return []
  const range = activeReportRange.value
  const step = Math.max(1, Math.ceil(rows.length / 8))
  const xAxis = rows.map((p, i) => i % step === 0 ? formatReportTime(p.time, range) : '')
  const tempPoints = rows.map((p) => p.temperature)
  const humPoints = rows.map((p) => p.humidity)
  const dustPoints = rows.map((p) => p.dust)

  const period = reportPeriodRange(range, reportDate.value)
  const weightHistory = (selectedArchive.value?.weightHistory || [])
    .map((item) => ({ item, ms: weightTimeMs(item) }))
    // 没有时间戳的记录无法判断是否在周期内，直接排除，避免被当成 1970-01-01 排在最前面。
    .filter((entry) => Number.isFinite(Number(entry.item.value))
      && entry.ms != null
      && entry.ms >= period.start
      && entry.ms <= period.end)
    .sort((a, b) => a.ms - b.ms)

  const weightPoints = weightHistory.map((entry) => Number(entry.item.value))
  const weightLatest = weightPoints.length ? weightPoints[weightPoints.length - 1] : null
  const tempLatest = lastValue(tempPoints)
  const humLatest = lastValue(humPoints)
  const dustLatest = lastValue(dustPoints)

  function computeYAxis(points, label) {
    // 温度用固定刻度 20/23/25，其他动态
    if (label === '温度' || label === 'Temperature') {
      return { min: 20, max: 25, ticks: [20, 23, 25] }
    }
    const valid = points.filter((v) => v != null && Number.isFinite(Number(v)))
    if (!valid.length) return { min: 0, max: 1, ticks: [0, 0.5, 1] }
    const min = Math.min(...valid)
    const max = Math.max(...valid)
    const pad = Math.max((max - min) * 0.15, 0.5)
    const lo = min - pad, hi = max + pad
    const mid = (lo + hi) / 2
    return { min: lo, max: hi, ticks: [lo, mid, hi] }
  }
  const tempYAxis = computeYAxis(tempPoints, '温度')
  const humYAxis = computeYAxis(humPoints, '湿度')
  const dustYAxis = computeYAxis(dustPoints, '粉尘')

  const fullXAxis = rows.map((p) => formatReportTime(p.time, range))

  const curves = [
    { label: labelText('temperature'), value: tempLatest != null ? `${tempLatest.toFixed(1)}℃` : '—', unit: '℃', axis: labelText('temperature'), points: tempPoints, xAxis, fullXAxis, yAxis: tempYAxis },
    { label: labelText('humidity'), value: humLatest != null ? `${humLatest.toFixed(0)}%` : '—', unit: '%', axis: labelText('humidity'), points: humPoints, xAxis, fullXAxis, yAxis: humYAxis },
    { label: labelText('dust'), value: dustLatest != null ? `${dustLatest.toFixed(0)}` : '—', unit: 'ppm', axis: labelText('dust'), points: dustPoints, xAxis, fullXAxis, yAxis: dustYAxis },
  ]
  // 日报不显示体重（一天只填一次体重，无曲线意义）
  if (range !== '日报') {
    const weightValue = weightLatest != null ? `${weightLatest}g` : '—'
    curves.push({ label: '体重变化曲线', value: weightValue, unit: 'g', axis: '体重', points: weightPoints, xAxis: weightHistory.map((entry) => entry.item.time), fullXAxis: weightHistory.map((entry) => entry.item.time) })
  }
  return curves
})

// 实时仪表盘：今日健康评分，基于今日 24h 环境历史。
const dashboardHealthScore = computed(() =>
  computeHealthScore(todayEnvHistory.value, selectedArchive.value?.weightHistory),
)

// 实时仪表盘：今日关键指标卡数据。
const dashboardStats = computed(() => [
  { key: 'health', label: '健康评分', value: String(dashboardHealthScore.value), tip: '基于今日环境与体重稳定性' },
  { key: 'weight', label: '体重', value: latestWeightText.value, tip: todayWeightRecorded.value ? '今日已称重' : '今日未称重' },
  { key: 'calls', label: '鸣叫次数', value: behaviorCountOf('鸣叫') || '-', tip: '需后端识别“鸣叫”' },
  { key: 'meals', label: '进食次数', value: behaviorCountOf('进食') || '-', tip: '基于行为识别' },
  { key: 'droppings', label: '排泄次数', value: behaviorCountOf('排泄') || '-', tip: '需后端识别“排泄”' },
])

// 实时仪表盘：实时环境指标卡数据。
const dashboardRealtimeEnv = computed(() => {
  const s = realtimeSnapshot.value
  return [
    { key: 'temperature', label: '温度', value: s.temperature, displayValue: `${formatNumber(s.temperature, 1)}℃`, unit: '℃', level: getTemperatureLevel(s.temperature), connected: s.connected, gaugeMax: 45 },
    { key: 'humidity', label: '湿度', value: s.humidity, displayValue: `${formatNumber(s.humidity, 0)}%`, unit: '%', level: getHumidityLevel(s.humidity), connected: s.connected, gaugeMax: 100 },
    { key: 'dust', label: '粉尘浓度', value: s.smokeValue, displayValue: `${formatNumber(s.smokeValue, 0)}${s.dustUnit}`, unit: s.dustUnit, level: getDustLevel(s.smokeValue, s.dustLevel), connected: s.connected, gaugeMax: 120 },
  ]
})

// 今日是否已记录体重。
const todayWeightRecorded = computed(() => {
  const history = selectedArchive.value?.weightHistory || []
  return history.some((item) => {
    const ms = weightTimeMs(item)
    if (ms == null) return false
    const d = new Date(ms)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` === todayText.value
  })
})

// 历史日期选择器：日报/周报可用的月份列表（最早的月份在上面，最近的月份在下面，往上滑看更早日期）。
const historyMonthList = computed(() => {
  const now = new Date()
  const months = []
  for (let i = 12; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({ year: d.getFullYear(), month: d.getMonth() })
  }
  return months
})

// 历史日期选择器：月报可用的年份列表（最近 6 年）。
const historyYearList = computed(() => {
  const current = new Date().getFullYear()
  const years = []
  for (let i = 5; i >= 0; i--) {
    years.push(current - i)
  }
  return years
})

const languageClass = computed(() => `lang-${systemPrefs.value.language}`)
const themeClass = computed(() => (systemPrefs.value.theme === 'dark' ? 'night-theme' : 'day-theme'))
const settingsColorLabel = computed(() => (systemPrefs.value.theme === 'dark' ? text.value.white : text.value.black))
const localizedEntryCards = computed(() => {
  const cards = text.value.cards || i18n.zh.cards
  return Object.fromEntries(Object.entries(entryCards).map(([key, card]) => {
    const [title, subtitle] = cards[key] || [card.title, card.subtitle]
    return [key, { ...card, title, subtitle, badge: notificationBadges.value[key] || 0 }]
  }))
})
const localizedPrimaryCards = computed(() => ({
  monitor: {
    ...primaryCards.monitor,
    title: text.value.cards?.monitor?.[0] || primaryCards.monitor.title,
  },
}))
const localizedActiveTitle = computed(() => {
  if (!activeView.value) return ''
  
  // 1. 成长报告子页面
  if (thirdView.value === 'daily-detail' || thirdView.value === 'weekly-detail' || thirdView.value === 'monthly-detail') {
    return `${activeReportRange.value} · ${reportDate.value}`
  }
  if (thirdView.value === 'report-photos') {
    return '成长照片'
  }
  if (thirdView.value === 'report-recordings') {
    return '学舌录音'
  }

  // 2. 医疗助手子页面
  if (activeView.value.kind === 'medical') {
    if (thirdView.value === 'diagnosis') return '智能问诊'
    if (thirdView.value === 'hospitals') return '附近医院'
    if (thirdView.value === 'health') return '健康分析'
    if (thirdView.value === 'records') return '病历'
  }

  // 3. 饲养手册子页面
  if (activeView.value.kind === 'handbook') {
    if (thirdView.value === 'care-profile') return '专属推荐'
    if (thirdView.value === 'tutorials') return '新手教程'
    if (thirdView.value === 'tutorial-detail') return '教程详情'
    if (thirdView.value === 'bird-id') return '拍照识鹦鹉'
  }

  // 4. 宠物档案子页面
  if (activeView.value.kind === 'archive') {
    if (thirdView.value === 'archive-gallery') return '宠物相册'
    if (thirdView.value && String(thirdView.value).startsWith('archive:')) return '档案详情'
  }

  const match = Object.values(localizedEntryCards.value).find((card) => card.route === activeRoute.value)
  return match?.title || activeView.value.title
})
const reportRanges = computed(() => [
  { value: '日报', label: text.value.daily },
  { value: '周报', label: text.value.weekly },
  { value: '月报', label: text.value.monthly },
])
const localizedReportStats = computed(() => {
  const score = computeHealthScore()
  // 最新环境值：从入库的环境历史里取最近一条作为当前读数。
  const latestEnv = environmentHistory.value.slice().reverse().find(
    (p) => p.temperature != null || p.humidity != null || p.dust != null,
  ) || {}
  const temperature = latestEnv.temperature ?? null
  const humidity = latestEnv.humidity ?? null
  const dust = latestEnv.dust ?? null

  const weightHistory = selectedArchive.value?.weightHistory || []
  const weights = weightHistory.map((item) => Number(item.value)).filter(Number.isFinite)
  const weightLatest = weights.length ? `${weights[weights.length - 1]}g` : '—'
  // 体重趋势：按当前周期内“最早一次 → 最近一次”的变化计算，
  // 而不是最后两次的差值，这样更符合周报/月报“周期总变化”的直觉。
  const weightFirst = weights.length >= 2 ? weights[0] : null
  const weightLast = weights.length >= 1 ? weights[weights.length - 1] : null
  const weightTrend = weightFirst == null || weightLast == null
    ? ''
    : `${weightLast - weightFirst >= 0 ? '+' : ''}${(weightLast - weightFirst).toFixed(1)}g`

  const realStats = [
    { label: '健康评分', value: String(score), trend: score >= 80 ? labelText('stable') : '↓' },
    { label: labelText('temperature'), value: temperature != null ? `${temperature.toFixed(1)}℃` : '—', trend: '' },
    { label: labelText('humidity'), value: humidity != null ? `${humidity.toFixed(0)}%` : '—', trend: '' },
    { label: labelText('dust'), value: dust != null ? `${dust.toFixed(0)}` : '—', trend: '' },
    { label: '体重', value: weightLatest, trend: weightTrend },
  ]
  return realStats.map((stat, index) => ({
    ...stat,
    label: ui.value.reportStats[index] || stat.label,
    trend: stat.trend === '稳定' ? labelText('stable') : stat.trend,
  }))
})
const localizedReportRecords = computed(() => reportRecords.map((record) => {
  const copy = ui.value.reportRecords[record.action]
  return copy ? { ...record, type: copy[0], value: copy[1] } : record
}))
const localizedMedicalModules = computed(() => medicalModules.map((module) => {
  const copy = ui.value.modules[module.key]
  return copy ? { ...module, title: copy[0], note: copy[1] } : module
}))
const localizedHandbookModules = computed(() => handbookModules.map((module) => {
  const copy = ui.value.modules[module.key]
  return copy ? { ...module, title: copy[0], note: copy[1] } : module
}))
const localizedTutorialCards = computed(() => tutorials.map((item, index) => {
  const copy = ui.value.tutorials?.[index]
  return copy ? { ...item, title: copy[0], tag: copy[1], minutes: copy[2] } : item
}))
const activeTutorial = computed(() => tutorials.find((item) => item.id === activeTutorialId.value) || null)
// 分诊卡维度：基于 TRIAGE_DIMENSIONS 注入 i18n 文案，供模板 v-for。
const diagnosisFields = computed(() => TRIAGE_DIMENSIONS.map((dim) => ({
  key: dim.key,
  label: labelText(dim.label),
  dimHint: labelText(dim.hint),
  options: dim.options.map((opt) => ({
    value: opt.value,
    score: opt.score,
    hint: labelText(opt.hint),
    text: valueText(opt.value),
  })),
})))
// 按当前表单值实时算风险等级（0-3），驱动顶部徽章。
function triageScore(form) {
  return TRIAGE_DIMENSIONS.reduce((sum, dim) => {
    const opt = dim.options.find((o) => o.value === form[dim.key])
    return sum + (opt ? opt.score : 0)
  }, 0)
}
function triageLevelOf(score) {
  if (score >= 5) return 3
  if (score >= 3) return 2
  if (score >= 1) return 1
  return 0
}
const liveRisk = computed(() => {
  const score = triageScore(diagnosisForm.value)
  const level = triageLevelOf(score)
  return { score, level, label: labelText(['triageLevel0', 'triageLevel1', 'triageLevel2', 'triageLevel3'][level]) }
})
// 真实风险引擎：按实际勾选算分 + 红旗组合，产出富结果对象。
function triageParrot(form) {
  const score = triageScore(form)
  let level = triageLevelOf(score)
  const flags = []
  const redFlags = []
  const valueOf = (key) => form[key]
  const scoreOf = (key) => {
    const dim = TRIAGE_DIMENSIONS.find((d) => d.key === key)
    const opt = dim && dim.options.find((o) => o.value === valueOf(key))
    return opt ? opt.score : 0
  }
  // 红旗组合（命中即升为紧急就医 level=3）
  if (scoreOf('breathing') >= 3) {
    redFlags.push(labelText('triageFlagBreathing'))
  }
  if (scoreOf('droppings') >= 3 && scoreOf('energy') >= 2) {
    redFlags.push(labelText('triageFlagTox'))
  }
  if (scoreOf('appetite') >= 3) {
    redFlags.push(labelText('triageFlagAppetite'))
  }
  if (scoreOf('energy') >= 3) {
    redFlags.push(labelText('triageFlagCritical'))
  }
  if (redFlags.length) level = 3
  // 普通 flags：所有 ≥2（橙）档的维度各一条症状→依据
  TRIAGE_DIMENSIONS.forEach((dim) => {
    const opt = dim.options.find((o) => o.value === valueOf(dim.key))
    if (opt && opt.score >= 2) {
      flags.push({ dim: labelText(dim.label), text: valueText(opt.value), hint: labelText(opt.hint) })
    }
  })
  const levelKeys = ['triageLevel0', 'triageLevel1', 'triageLevel2', 'triageLevel3']
  const adviceKeys = ['triageAdvice0', 'triageAdvice1', 'triageAdvice2', 'triageAdvice3']
  const summaryKeys = ['triageSummary0', 'triageSummary1', 'triageSummary2', 'triageSummary3']
  return {
    level,
    levelLabel: labelText(levelKeys[level]),
    summary: labelText(summaryKeys[level]),
    advice: labelText(adviceKeys[level]),
    flags,
    redFlags,
    disclaimer: labelText('triageDisclaimer'),
    score,
  }
}
const localizedArchivePhotoRecords = computed(() => archivePhotoRecords.value.map((photo, index) => {
  const baseIndex = index - capturedPhotos.value.length
  const fallbackTitle = ui.value.photoTitles[baseIndex] || photo.title
  return {
    ...photo,
    title: photo.image ? `${ui.value.snapshotPhoto} ${formatShotTime(photo.savedAt).slice(5)}` : fallbackTitle,
  }
}))
const selectedArchive = computed(() => {
  const id = thirdView.value.startsWith('archive:') ? thirdView.value.replace('archive:', '') : activeArchiveId.value
  return profiles.value.find((profile) => profile.id === id) || profiles.value[0]
})
const selectedArchivePetId = computed(() => selectedArchive.value?.id || selectedParrot.value?.id || '')
const selectedAvatarParrot = computed(() => (
  localParrots.value.find((parrot) => parrot.id === account.value.avatarParrotId) || localParrots.value[0] || EMPTY_REMOTE_PARROT
))
const settingsAvatarType = computed(() => (
  (isSettingsEditing.value ? settingsDraft.value.avatarImage : account.value.avatarImage) || selectedAvatarParrot.value.avatarType
))
const profileFormAgeStage = computed(() => getAgeStage(profileForm.value.birthday))
const filteredTutorials = computed(() => {
  const keyword = tutorialKeyword.value.trim()
  if (!keyword) return localizedTutorialCards.value
  return localizedTutorialCards.value.filter((item) => `${item.title}${item.tag}`.includes(keyword))
})

// 教程卡片按 tag 关键字归类，每类一个强调色 + emoji 图标，让列表一眼能区分主题。
// 命中第一条即返回；都不命中走兜底中性色。
const TUTORIAL_CATEGORIES = [
  { match: /新手|避坑|误区|训练|入门/, icon: '🌱', color: '#2f9e6e' },
  { match: /食物|喂养|营养|饮水|补钙|食谱/, icon: '🥣', color: '#d68c3d' },
  { match: /健康|药浴|换羽|急救|啄羽|剪羽|指甲|喙|护理/, icon: '🩺', color: '#c0527a' },
  { match: /环境|布置|清洁|笼舍|外出|作息|睡眠|光照|季节/, icon: '🏠', color: '#3a7bb3' },
  { match: /行为|情绪|肢体|语言/, icon: '🪶', color: '#7b5bb3' },
]
const TUTORIAL_FALLBACK = { icon: '📘', color: '#8a6d4f' }
function tutorialCategory(tag) {
  return TUTORIAL_CATEGORIES.find((c) => c.match.test(tag)) || TUTORIAL_FALLBACK
}

// === 专属推荐：当前鹦鹉品种的饲养配置 + 环境适配度评分 ===
// careProfileSpecies：专属推荐页手动选中的品种；空字符串=跟随当前鹦鹉。
// 进入页面时重置为空，每次都先展示「我养的这只」。
const careProfileSpecies = ref('')
// 实际展示用的品种：手动选了别的就用别的，否则跟随当前鹦鹉
const careActiveSpecies = computed(() => careProfileSpecies.value || selectedParrot.value?.species)
// currentSpeciesCare：按当前展示品种取专属配置（未录入品种走兜底通用配置）
const currentSpeciesCare = computed(() => getSpeciesCareProfile(careActiveSpecies.value))
// 是否在预览非自己鹦鹉的品种（用于环境评分卡显示「预览」提示）
const carePreviewingOther = computed(() => {
  const mine = selectedParrot.value?.species
  return !!mine && !!careProfileSpecies.value && careProfileSpecies.value !== mine
})
// 品种下拉选项：把我养的这只排到第一个，方便一眼定位；我的品种不在 11 个里也补进去
const careSpeciesOptions = computed(() => {
  const mine = selectedParrot.value?.species
  const all = parrotSpeciesOptions.slice()
  if (mine && !all.includes(mine)) all.unshift(mine)
  if (mine) {
    const i = all.indexOf(mine)
    if (i > 0) { all.splice(i, 1); all.unshift(mine) }
  }
  return all
})
function selectCareSpecies(species) {
  const mine = selectedParrot.value?.species
  // 选回自己养的这只（或空）就归位为「跟随当前鹦鹉」，保持和进页面重置一致
  careProfileSpecies.value = (!species || species === mine) ? '' : species
}

// 关联教程已改为各品种内嵌的专属护理建议（careAdvice），不再依赖大教程库。

// 单项评分工具：温度/湿度按「适宜区间」偏离度扣分，粉尘按「品种阈值」衰减扣分
function scoreRange(value, range) {
  if (value == null || !Number.isFinite(value)) return null
  const [min, max] = range
  if (value < min) return Math.max(0, Math.round(100 - (min - value) * 8))
  if (value > max) return Math.max(0, Math.round(100 - (value - max) * 8))
  return 100
}
function scoreThreshold(value, threshold) {
  if (value == null || !Number.isFinite(value)) return null
  const { good, warn } = threshold
  if (value < good) return 100
  if (value < warn) return Math.round(100 - ((value - good) / (warn - good)) * 40) // good~warn：100 -> 60
  return Math.max(0, Math.round(60 - ((value - warn) / Math.max(warn, 1)) * 60)) // >warn：60 -> 0
}
function envLevelKey(score) {
  if (score == null) return 'none'
  if (score >= 85) return 'good'
  if (score >= 70) return 'fair'
  if (score >= 50) return 'warn'
  return 'bad'
}

// 综合环境适配度：温度 0.35 + 湿度 0.25 + 粉尘 0.4（粉尘对鹦鹉最致命，权重最高）
// 三项齐全给完整总分；缺项时按可用项权重归一化重算（partial），
// 既不让「没连传感器」直接变成离线，也用 partial 标记提醒用户数据不全、仅供参考。
const envMatch = computed(() => {
  const profile = currentSpeciesCare.value
  const snap = realtimeSnapshot.value
  const tempScore = scoreRange(snap.temperature, profile.tempRange)
  const humidityScore = scoreRange(snap.humidity, profile.humidityRange)
  const dustScore = scoreThreshold(snap.smokeValue, profile.dustThreshold)
  const dustUnit = snap.dustUnit || 'ppm'
  const items = [
    {
      key: 'temperature',
      label: labelText('envTemp'),
      value: snap.temperature,
      score: tempScore,
      rangeText: `${profile.tempRange[0]}–${profile.tempRange[1]} ℃`,
      unit: '℃',
      advice: tempScore == null ? labelText('envNoData')
        : tempScore >= 100 ? labelText('envSuitable')
        : (snap.temperature < profile.tempRange[0] ? labelText('envLow') : labelText('envHigh')),
    },
    {
      key: 'humidity',
      label: labelText('envHumidity'),
      value: snap.humidity,
      score: humidityScore,
      rangeText: `${profile.humidityRange[0]}–${profile.humidityRange[1]} %`,
      unit: '%',
      advice: humidityScore == null ? labelText('envNoData')
        : humidityScore >= 100 ? labelText('envSuitable')
        : (snap.humidity < profile.humidityRange[0] ? labelText('envLow') : labelText('envHigh')),
    },
    {
      key: 'dust',
      label: labelText('envDust'),
      value: snap.smokeValue,
      score: dustScore,
      stale: snap.smokeStale,
      rangeText: `优 ≤${profile.dustThreshold.good} / 警 ≤${profile.dustThreshold.warn} ${dustUnit}`,
      unit: dustUnit,
      advice: dustScore == null ? labelText('envNoData')
        : snap.smokeStale ? `${labelText('envLastRecord')} ${formatRecordTime(snap.smokeRecordTime)}`
        : dustScore >= 100 ? labelText('envDustGood')
        : dustScore >= 60 ? labelText('envDustWarn')
        : labelText('envDustBad'),
    },
  ]
  // 按可用项的权重归一化重算：缺粉尘时只用温湿度算分，避免一刀切判离线。
  const scored = [
    { score: tempScore, weight: 0.35 },
    { score: humidityScore, weight: 0.25 },
    { score: dustScore, weight: 0.4 },
  ]
  const present = scored.filter((it) => it.score != null)
  let total = null
  let partial = false
  if (present.length > 0) {
    const wsum = present.reduce((s, it) => s + it.weight, 0)
    total = Math.round(present.reduce((s, it) => s + it.score * it.weight, 0) / wsum)
    partial = present.length < scored.length
  }
  return {
    total,
    partial,
    levelKey: envLevelKey(total),
    items,
    connected: !!snap.connected,
  }
})

function refreshEnvSnapshot() {
  loadRealtimeEnv()
}

// 进入「专属推荐」时拉一次实时环境，并开 5s 轮询让温湿度/烟雾实时变化；离开时关轮询。
// 进入「附近医院」时初始化高德地图，离开时销毁。
watch(
  () => thirdView.value,
  (view) => {
    if (view === 'care-profile') {
      careProfileSpecies.value = '' // 每次进入都先展示「我养的这只」
      loadRealtimeEnv()
      startCareEnvPolling()
    } else {
      stopCareEnvPolling()
    }
    if (view === 'hospitals') {
      nextTick(() => {
        initAMap()
      })
    } else {
      destroyAMap()
    }
  },
)

// 推荐食谱饼图：进入专属推荐页 / 切换品种 / 切换主题时重绘；离开页面时销毁实例。
// nextTick 等 DOM 里 dietChartRef 挂载后再初始化，否则 echarts.init 拿不到容器。
watch(
  [
    () => thirdView.value,
    () => currentSpeciesCare.value,
    () => systemPrefs.value.theme,
  ],
  () => {
    if (thirdView.value !== 'care-profile') {
      if (dietChartInstance) { dietChartInstance.dispose(); dietChartInstance = null }
      return
    }
    nextTick(() => initDietChart(currentSpeciesCare.value?.dietRate))
  },
)

// 饮食配比/粉尘耐受的中文显示
const DIET_LABELS = { pellet: '颗粒料', veg: '蔬菜', fruit: '水果', seed: '种子坚果' }
function dietLabel(key) {
  return DIET_LABELS[key] || key
}
const DUST_TOLERANCE_TEXT = { tolerant: '较耐受', moderate: '中等', sensitive: '敏感' }
function dustToleranceText(t) {
  return DUST_TOLERANCE_TEXT[t] || t
}

const filteredMedicalRecords = computed(() => {
  const keyword = medicalRecordSearch.value.trim()
  if (!keyword) return medicalRecords.value
  return medicalRecords.value.filter((item) => (
    `${item.content || ''} ${item.title || ''} ${item.hospitalName || ''}`.includes(keyword)
  ))
})
const filteredLedgerRecords = computed(() => {
  const keyword = ledgerKeyword.value.trim()
  return ledgerRecords.value.filter((item) => {
    const matchesCategory = ledgerCategoryFilter.value === '全部'
      || normalizeLedgerCategory(item.tag) === ledgerCategoryFilter.value
    const matchesKeyword = !keyword || ledgerSearchText(item).includes(keyword)
    return matchesCategory && matchesKeyword
  })
})
const ledgerTotal = computed(() => (
  ledgerRecords.value.reduce((total, item) => total + Number(item.amount || 0), 0)
))
const todayText = computed(() => currentLocalDateText())
const currentMonthText = computed(() => todayText.value.slice(0, 7))
const ledgerMonthTotal = computed(() => (
  ledgerRecords.value
    .filter((item) => String(item.time || '').startsWith(currentMonthText.value))
    .reduce((total, item) => total + Number(item.amount || 0), 0)
))
const ledgerRecordCount = computed(() => ledgerRecords.value.length)
const archivePhotoRecords = computed(() => [
  ...capturedPhotos.value.filter((photo) => photo.parrotId === selectedArchivePetId.value),
  ...basePhotoRecords.value.filter((photo) => photo.parrotId === selectedArchivePetId.value),
])
const avatarSelectablePhotos = computed(() => archivePhotoRecords.value)
// 档案首页仅预览最近六张真实归档照片；照片接口与本地截图列表均按最新优先维护。
// 不足六张的格子由模板中的占位图补足，且不参与照片计数。
const archivePhotoPreview = computed(() => archivePhotoRecords.value.slice(0, 6))
const archivePhotoPlaceholderCount = computed(() => Math.max(0, 6 - archivePhotoPreview.value.length))
const selectedPhotoObjects = computed(() => (
  localizedArchivePhotoRecords.value.filter((photo) => selectedPhotoKeys.value.includes(photoKey(photo)))
))

function loadReadBadgeKeys() {
  if (typeof localStorage === 'undefined') return []
  try {
    const parsed = JSON.parse(localStorage.getItem('parrotReadBadges') || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function persistReadBadge(key) {
  if (!key || readBadgeKeys.value.includes(key)) return
  readBadgeKeys.value = [...readBadgeKeys.value, key]
  try {
    localStorage.setItem('parrotReadBadges', JSON.stringify(readBadgeKeys.value))
  } catch {
    // Local persistence is optional; the in-memory read state still works.
  }
}

let reportToastTimer = 0
let alarmToastTimer = 0

function showGrowthReportToast() {
  window.clearTimeout(reportToastTimer)
  reportToastVisible.value = true
  reportToastTimer = window.setTimeout(() => {
    reportToastVisible.value = false
  }, 2000)
}

function showAlarmToast(message) {
  window.clearTimeout(alarmToastTimer)
  alarmToast.value = message || '环境异常'
  alarmToastTimer = window.setTimeout(() => {
    alarmToast.value = ''
  }, 2200)
}

function handleGrowthReportReady() {
  notificationBadges.value = { ...notificationBadges.value, growth: Math.max(1, notificationBadges.value.growth || 0) }
  showGrowthReportToast()
}

function rangeText(value) {
  return reportRanges.value.find((range) => range.value === value)?.label || value
}

function labelText(key) {
  return ui.value.labels?.[key] || EXTRA_LABELS[systemPrefs.value.language]?.[key] || key
}

const EXTRA_LABELS = {
  zh: {
    birth: '出生', addProfile: '增加档案', editProfile: '编辑基本资料', weightRecord: '体重记录',
    growthAlbum: '成长相册', recordWeight: '录入体重', todayWeight: '今日体重',
    autoArchive: '截图和睡眠照片会自动归档。', weightSaved: '体重已保存', weightChart: '体重记录曲线',
    weightAxis: '体重 / g', editTime: '编辑时间', grams: '克数', parrotSpecies: '鹦鹉种类',
    parrotName: '鹦鹉名字', birthday: '出生日期', ageStage: '年龄标识', currentWeight: '当前体重',
    diagnosisTitle: '外在表现问卷', energy: '精神状态', appetite: '进食情况', breathing: '呼吸表现',
    droppings: '排泄情况', myLocation: '我的位置', name: '名称', description: '说明',
    editPlaceholder: '这里填写需要修改的信息。', ledgerTotal: '总开销', searchLedger: '搜索消费记录',
    ledgerDate: '日期', createdAt: '创建时间', ledgerTag: '属性', ledgerDescription: '描述',
    ledgerAmount: '金额', updatedAt: '更新时间', action: '操作', created: '创建', updated: '更新',
    unedited: '未编辑', tagPlaceholder: '标签：主粮/医疗/用品', descriptionPlaceholder: '描述：玩具铃铛',
    amountPlaceholder: '金额：29',
    careProfileFallback: '该品种暂未录入专属方案，以下为通用建议',
    careProfileNoArchive: '请先在「宠物档案」完善鹦鹉品种，以获得更精准的专属推荐',
    careProfileOverview: '品种速览', careProfileOrigin: '原产地', careProfileBodyLength: '体型', careProfileWeight: '体重',
    careProfileLifespan: '寿命', careProfileTemperament: '性格', careProfileTalking: '学话能力',
    careProfileRisks: '关键风险', careProfileEnv: '专属环境需求',
    careProfileTempRange: '适宜温度', careProfileHumidityRange: '适宜湿度',
    careProfileDustLevel: '羽粉量', careProfileDustTolerance: '粉尘耐受',
    careProfileDiet: '推荐食谱', careProfileDietRate: '饮食配比',
    careProfileRecommended: '推荐食物', careProfileToxic: '禁忌食物',
    careProfileAdvice: '专属护理建议',
    careProfileEnvPreview: '预览：基于你当前环境数据评估该品种适配度',
    tutorialListTitle: '饲养教程库', tutorialCount: '共 {n} 篇', tutorialEmpty: '没有找到相关教程', tutorialBack: '返回教程列表', tutorialRead: '阅读',
    envScoreTitle: '环境适配度评分', envScoreSubtitle: '基于当前品种适宜区间与实时监测',
    envLoading: '正在加载环境数据…', envNotConnected: '未连接实时监测，点击刷新', envRefresh: '刷新', envNoData: '暂无数据',
    envPartialNote: '⚠ 部分监测项无数据，评分仅供参考',
    envLastRecord: '上次记录',
    envTemp: '温度', envHumidity: '湿度', envDust: '粉尘浓度',
    envSuitable: '适宜', envLow: '偏低', envHigh: '偏高',
    envDustGood: '良好', envDustWarn: '偏高，建议通风', envDustBad: '过高，建议立即通风',
    envLevelGood: '优秀', envLevelFair: '良好', envLevelWarn: '注意', envLevelBad: '需处理',
    envRange: '适宜区间', envCurrent: '当前',
    triageResultTitle: '智能问诊结果', triageSubtitle: '按实际表现勾选，实时评估风险等级', triageLiveRisk: '当前风险', triageAdviceLabel: '处置建议',
    triageDisclaimer: 'AI 初判，不能替代异宠医生面诊。鸟类善于隐藏疾病，多项异常同时出现请立即就医。',
    triageHintEnergyDim: '鸟类会隐藏疾病，精神变化往往已是中后期',
    triageHintAppetiteDim: '体重下降是鸟类疾病最早的信号',
    triageHintBreathingDim: 'tail bobbing + 开口呼吸 = 呼吸窘迫',
    triageHintDroppingsDim: '黄绿便 + 多尿 + 萎靡 = 鹦鹉热/重金属中毒风险',
    triageHintEnergy0: '活跃互动、对声音有反应，属健康表现',
    triageHintEnergy1: '略安静但能反应，留意是否开始蓬毛',
    triageHintEnergy2: '蓬毛嗜睡，鸟类隐藏疾病，往往已是中后期',
    triageHintEnergy3: '伏底闭眼属濒危信号，需立即就医',
    triageHintAppetite0: '进食正常，保持日常食谱与每日称重节奏',
    triageHintAppetite1: '食量下降是疾病早期信号，建议每日称重',
    triageHintAppetite2: '明显拒食，需补温补食并尽快就医',
    triageHintAppetite3: '24 小时未进食，急性能量衰竭，需紧急就医',
    triageHintBreathing0: '呼吸平稳、无杂音',
    triageHintBreathing1: '偶尔喷嚏或鼻分泌物，提示轻度呼吸道刺激',
    triageHintBreathing2: '安静时尾上下摆（tail bobbing），呼吸已费力',
    triageHintBreathing3: '持续张口呼吸 = 呼吸窘迫急症',
    triageHintDroppings0: '粪便颜色与形态正常',
    triageHintDroppings1: '偏稀或多尿，需观察是否持续',
    triageHintDroppings2: '含未消化食物，提示消化或嗉囊问题',
    triageHintDroppings3: '黄绿/血便 + 萎靡 = 鹦鹉热或重金属中毒风险',
    triageLevel0: '低风险', triageLevel1: '居家观察', triageLevel2: '建议就医', triageLevel3: '紧急就医',
    triageSummary0: '当前表现以正常档为主，未触发急症信号。',
    triageSummary1: '出现轻度异常，建议居家观察并在 24 小时内复检。',
    triageSummary2: '出现明显异常信号，建议尽快就诊异宠医院。',
    triageSummary3: '触发急症信号，需立即就医。',
    triageAdvice0: '日常养护：保持温度稳定、每日称重、清洁节奏。',
    triageAdvice1: '居家观察：通风 20 分钟、保温、24 小时内复检并每日称重。',
    triageAdvice2: '建议 24-48 小时内就诊异宠医院，记录症状变化带去给医生。',
    triageAdvice3: '紧急：保温 88-90°F（31-32°C）转运，带新鲜粪便样本；若怀疑鹦鹉热需隔离其他鸟并戴手套，立即联系异宠医院。',
    triageFlagBreathing: '持续张口呼吸 → 呼吸窘迫，可能为气道感染、异物或缺氧',
    triageFlagTox: '黄绿/血便 + 精神差 → 鹦鹉热或重金属中毒风险（人畜共患，需防护）',
    triageFlagAppetite: '拒食逾 24 小时 → 急性能量衰竭，需补温补食',
    triageFlagCritical: '伏底闭眼 → 濒危信号，立即就医',
    medTypeSymptom: '症状', medTypeDiagnosis: '就诊', medTypeMedication: '用药', medTypeRecheck: '复查', medTypeOther: '其他',
    medDateLabel: '日期', medTypeLabel: '类型', medMore: '更多（医院 / 电话）', medLess: '收起', medCancel: '取消',
    medContentPlaceholder: '病历内容，如：精神萎靡、食量下降、用药反应…',
    medHospitalPlaceholder: '医院名称（可选）', medPhonePlaceholder: '联系电话（可选）', medAdd: '新增病历',
    medHealthTitle: '健康评分', medHealthSubtitle: '基于近 90 天病历记录的综合评估', medHealthScore: '综合评分',
    medHealthLevelGood: '优秀', medHealthLevelFair: '良好', medHealthLevelCaution: '注意', medHealthLevelWarning: '需关注',
    medNoRecordsHint: '暂无病历记录，保持日常观察',
    medHealthTypeDist: '病历类型分布', medHealthTypeDistSub: '按记录类型', medHealthNoTypeHint: '暂无类型统计',
    medHealthTrend: '病历时间趋势', medHealthTrendSub: '近 6 个月', medHealthNoTrendHint: '记录病历后显示趋势', medHealthRecordCount: '记录',
    medHealthAnalysis: '健康分析', medLastRecord: '最近一次记录', medDaysAgo: ' 天前', medMostFrequent: '最频繁类型', medLastSymptom: '最近症状',
    medHealthAdvice0: '当前评分优秀，继续保持日常养护节奏与定期称重。',
    medHealthAdvice1: '评分良好，留意轻微异常，24 小时内复检并记录。',
    medHealthAdvice2: '评分偏低，近 90 天有较多就诊/症状记录，建议就诊异宠医院复查。',
    medHealthAdvice3: '评分需关注，近期症状/就诊集中，请尽快联系异宠医生。',
    medRecentRecords: '近期病历',
  },
  en: {
    birth: 'Born', addProfile: 'Add Profile', editProfile: 'Edit Profile', weightRecord: 'Weight Record',
    growthAlbum: 'Growth Album', recordWeight: 'Record Weight', todayWeight: 'Today Weight',
    autoArchive: 'Screenshots and sleep photos are archived automatically.', weightSaved: 'Weight saved', weightChart: 'Weight Record Curve',
    weightAxis: 'Weight / g', editTime: 'Edit time', grams: 'Grams', parrotSpecies: 'Parrot species',
    parrotName: 'Parrot name', birthday: 'Birthday', ageStage: 'Age stage', currentWeight: 'Current weight',
    diagnosisTitle: 'Appearance Questionnaire', energy: 'Energy', appetite: 'Appetite', breathing: 'Breathing',
    droppings: 'Droppings', myLocation: 'My location', name: 'Name', description: 'Description',
    editPlaceholder: 'Write the information to edit here.', ledgerTotal: 'Total Spend', searchLedger: 'Search expense records',
    ledgerDate: 'Date', createdAt: 'Created', ledgerTag: 'Category', ledgerDescription: 'Description',
    ledgerAmount: 'Amount', updatedAt: 'Updated', action: 'Action', created: 'Created', updated: 'Updated',
    unedited: 'Not edited', tagPlaceholder: 'Tag: food/medical/supplies', descriptionPlaceholder: 'Description: toy bell',
    amountPlaceholder: 'Amount: 29',
    careProfileFallback: 'No species-specific guide yet; showing general advice',
    careProfileNoArchive: 'Complete the parrot profile first for more accurate advice',
    careProfileOverview: 'Species Overview', careProfileOrigin: 'Origin', careProfileBodyLength: 'Size', careProfileWeight: 'Weight',
    careProfileLifespan: 'Lifespan', careProfileTemperament: 'Temperament', careProfileTalking: 'Talking ability',
    careProfileRisks: 'Key risks', careProfileEnv: 'Ideal environment',
    careProfileTempRange: 'Ideal temp', careProfileHumidityRange: 'Ideal humidity',
    careProfileDustLevel: 'Feather dust', careProfileDustTolerance: 'Dust tolerance',
    careProfileDiet: 'Diet', careProfileDietRate: 'Diet ratio',
    careProfileRecommended: 'Recommended foods', careProfileToxic: 'Toxic foods',
    careProfileAdvice: 'Care Advice',
    careProfileEnvPreview: 'Preview: based on your current environment data',
    tutorialListTitle: 'Care Tutorials', tutorialCount: '{n} tutorials', tutorialEmpty: 'No tutorials found', tutorialBack: 'Back to tutorials', tutorialRead: 'Read',
    envScoreTitle: 'Environment Match Score', envScoreSubtitle: 'Based on species ideal range and live data',
    envLoading: 'Loading environment data…', envNotConnected: 'Live data offline, tap to refresh', envRefresh: 'Refresh', envNoData: 'No data',
    envPartialNote: '⚠ Some sensors offline, score is approximate',
    envLastRecord: 'Last reading',
    envTemp: 'Temperature', envHumidity: 'Humidity', envDust: 'Dust',
    envSuitable: 'Ideal', envLow: 'Low', envHigh: 'High',
    envDustGood: 'Good', envDustWarn: 'High, ventilate', envDustBad: 'Too high, ventilate now',
    envLevelGood: 'Excellent', envLevelFair: 'Fair', envLevelWarn: 'Caution', envLevelBad: 'Action needed',
    envRange: 'Ideal range', envCurrent: 'Current',
    triageResultTitle: 'Triage Result', triageSubtitle: 'Select actual signs — risk updates live', triageLiveRisk: 'Current risk', triageAdviceLabel: 'Recommended action',
    triageDisclaimer: 'AI triage, not a substitute for an exotic-pet vet. Birds hide illness; if several signs appear together, see a vet immediately.',
    triageHintEnergyDim: 'Birds hide illness; energy changes often mean mid-to-late stage',
    triageHintAppetiteDim: 'Weight loss is the earliest sign of illness in birds',
    triageHintBreathingDim: 'Tail bobbing + open-mouth breathing = respiratory distress',
    triageHintDroppingsDim: 'Green droppings + polyuria + lethargy = psittacosis / heavy-metal risk',
    triageHintEnergy0: 'Active and responsive — healthy',
    triageHintEnergy1: 'Quiet but responsive — watch for fluffing',
    triageHintEnergy2: 'Fluffed & drowsy — birds hide illness; often mid-to-late stage',
    triageHintEnergy3: 'On cage floor, eyes closed — critical; see a vet immediately',
    triageHintAppetite0: 'Eating normally — keep routine diet and daily weighing',
    triageHintAppetite1: 'Eating less — early sign of illness; weigh daily',
    triageHintAppetite2: 'Clearly refusing food — warm & assist-feed; see a vet soon',
    triageHintAppetite3: 'No food for 24h+ — acute energy depletion; emergency',
    triageHintBreathing0: 'Even breathing, no noise',
    triageHintBreathing1: 'Occasional sneeze / nasal discharge — mild irritation',
    triageHintBreathing2: 'Tail bobbing at rest — breathing is labored',
    triageHintBreathing3: 'Persistent open-mouth breathing — respiratory emergency',
    triageHintDroppings0: 'Normal color and form',
    triageHintDroppings1: 'Loose or polyuria — monitor if persistent',
    triageHintDroppings2: 'Undigested food — digestive or crop issue',
    triageHintDroppings3: 'Green/yellow or bloody — psittacosis or heavy-metal risk',
    triageLevel0: 'Low risk', triageLevel1: 'Watch at home', triageLevel2: 'See a vet', triageLevel3: 'Emergency',
    triageSummary0: 'Mostly normal signs; no emergency triggers.',
    triageSummary1: 'Mild abnormalities — watch at home and recheck within 24h.',
    triageSummary2: 'Clear abnormal signals — see an exotic-pet vet soon.',
    triageSummary3: 'Emergency signal — seek care immediately.',
    triageAdvice0: 'Routine care: stable temperature, daily weighing, cleaning rhythm.',
    triageAdvice1: 'Ventilate 20 min, keep warm, recheck within 24h, weigh daily.',
    triageAdvice2: 'Visit an exotic-pet vet within 24-48h; bring symptom notes.',
    triageAdvice3: 'Emergency: transport at 88-90°F (31-32°C), bring a fresh dropping sample; if psittacosis suspected, isolate other birds and wear gloves; contact an avian vet now.',
    triageFlagBreathing: 'Persistent open-mouth breathing → respiratory distress (airway infection / foreign body / hypoxia)',
    triageFlagTox: 'Green/bloody droppings + lethargy → psittacosis or heavy-metal toxicity (zoonotic, take precautions)',
    triageFlagAppetite: 'No food for 24h+ → acute energy depletion; warm & assist-feed',
    triageFlagCritical: 'On floor, eyes closed → critical; seek care immediately',
    medTypeSymptom: 'Symptom', medTypeDiagnosis: 'Diagnosis', medTypeMedication: 'Medication', medTypeRecheck: 'Recheck', medTypeOther: 'Other',
    medDateLabel: 'Date', medTypeLabel: 'Type', medMore: 'More (hospital / phone)', medLess: 'Less', medCancel: 'Cancel',
    medContentPlaceholder: 'Record content, e.g. lethargy, eating less, medication response…',
    medHospitalPlaceholder: 'Hospital name (optional)', medPhonePlaceholder: 'Phone (optional)', medAdd: 'Add record',
    medHealthTitle: 'Health Score', medHealthSubtitle: 'Based on records in the last 90 days', medHealthScore: 'Overall',
    medHealthLevelGood: 'Excellent', medHealthLevelFair: 'Fair', medHealthLevelCaution: 'Caution', medHealthLevelWarning: 'Needs attention',
    medNoRecordsHint: 'No records yet; keep routine observation.',
    medHealthTypeDist: 'Record Types', medHealthTypeDistSub: 'By record type', medHealthNoTypeHint: 'No type stats yet.',
    medHealthTrend: 'Record Trend', medHealthTrendSub: 'Last 6 months', medHealthNoTrendHint: 'Add records to see the trend.', medHealthRecordCount: 'Records',
    medHealthAnalysis: 'Health Analysis', medLastRecord: 'Last record', medDaysAgo: ' days ago', medMostFrequent: 'Most frequent', medLastSymptom: 'Latest symptom',
    medHealthAdvice0: 'Score is excellent — keep your routine care and daily weighing.',
    medHealthAdvice1: 'Score is fair — watch minor signs, recheck within 24h.',
    medHealthAdvice2: 'Score is low — several visits/symptoms in the last 90 days; see an exotic-pet vet for a checkup.',
    medHealthAdvice3: 'Score needs attention — recent symptoms/visits are clustered; contact an avian vet soon.',
    medRecentRecords: 'Recent Records',
  },
  es: {
    birth: 'Nacimiento', addProfile: 'Añadir perfil', editProfile: 'Editar perfil', weightRecord: 'Registro de peso',
    growthAlbum: 'Álbum de crecimiento', recordWeight: 'Registrar peso', todayWeight: 'Peso de hoy',
    autoArchive: 'Las capturas y fotos de sueño se archivan automáticamente.', weightSaved: 'Peso guardado', weightChart: 'Curva de peso',
    weightAxis: 'Peso / g', editTime: 'Hora de edición', grams: 'Gramos', parrotSpecies: 'Especie de loro',
    parrotName: 'Nombre del loro', birthday: 'Fecha de nacimiento', ageStage: 'Etapa', currentWeight: 'Peso actual',
    diagnosisTitle: 'Cuestionario externo', energy: 'Energía', appetite: 'Apetito', breathing: 'Respiración',
    droppings: 'Excrementos', myLocation: 'Mi ubicación', name: 'Nombre', description: 'Descripción',
    editPlaceholder: 'Escribe aquí la información a modificar.', ledgerTotal: 'Gasto total', searchLedger: 'Buscar gastos',
    ledgerDate: 'Fecha', createdAt: 'Creado', ledgerTag: 'Categoría', ledgerDescription: 'Descripción',
    ledgerAmount: 'Importe', updatedAt: 'Actualizado', action: 'Acción', created: 'Creado', updated: 'Actualizado',
    unedited: 'Sin editar', tagPlaceholder: 'Etiqueta: comida/médico/suministros', descriptionPlaceholder: 'Descripción: campana de juguete',
    amountPlaceholder: 'Importe: 29',
    careProfileFallback: 'Sin guía específica; se muestra consejo general',
    careProfileNoArchive: 'Completa el perfil del loro para consejos más precisos',
    careProfileOverview: 'Ficha', careProfileOrigin: 'Origen', careProfileBodyLength: 'Tamaño', careProfileWeight: 'Peso',
    careProfileLifespan: 'Vida', careProfileTemperament: 'Carácter', careProfileTalking: 'Habla',
    careProfileRisks: 'Riesgos', careProfileEnv: 'Entorno ideal',
    careProfileTempRange: 'Temp. ideal', careProfileHumidityRange: 'Humedad ideal',
    careProfileDustLevel: 'Polvo', careProfileDustTolerance: 'Tolerancia al polvo',
    careProfileDiet: 'Dieta', careProfileDietRate: 'Proporción',
    careProfileRecommended: 'Alimentos', careProfileToxic: 'Alimentos tóxicos',
    careProfileAdvice: 'Consejos de cuidado',
    careProfileEnvPreview: 'Vista previa: según los datos de tu entorno actual',
    tutorialListTitle: 'Tutoriales de cuidado', tutorialCount: '{n} tutoriales', tutorialEmpty: 'No se encontraron tutoriales', tutorialBack: 'Volver a tutoriales', tutorialRead: 'Leer',
    envScoreTitle: 'Puntuación de entorno', envScoreSubtitle: 'Rango ideal vs datos en vivo',
    envLoading: 'Cargando datos del entorno…', envNotConnected: 'Sin datos en vivo, toca para actualizar', envRefresh: 'Actualizar', envNoData: 'Sin datos',
    envPartialNote: '⚠ Algunos sensores sin datos, puntaje referencial',
    envLastRecord: 'Última lectura',
    envTemp: 'Temperatura', envHumidity: 'Humedad', envDust: 'Polvo',
    envSuitable: 'Ideal', envLow: 'Bajo', envHigh: 'Alto',
    envDustGood: 'Bien', envDustWarn: 'Alto, ventila', envDustBad: 'Muy alto, ventila ya',
    envLevelGood: 'Excelente', envLevelFair: 'Aceptable', envLevelWarn: 'Atención', envLevelBad: 'Actuar',
    envRange: 'Rango ideal', envCurrent: 'Actual',
    triageResultTitle: 'Resultado del triaje', triageSubtitle: 'Selecciona los signos reales — el riesgo se actualiza en vivo', triageLiveRisk: 'Riesgo actual', triageAdviceLabel: 'Acción recomendada',
    triageDisclaimer: 'Triaje con IA, no sustituye al veterinario de exóticos. Las aves ocultan la enfermedad; si varios signos coinciden, ve al vet de inmediato.',
    triageHintEnergyDim: 'Las aves ocultan la enfermedad; los cambios de energía suelen indicar etapa avanzada',
    triageHintAppetiteDim: 'La pérdida de peso es el primer signo de enfermedad',
    triageHintBreathingDim: 'Movimiento de cola + respiración con pico abierto = dificultad respiratoria',
    triageHintDroppingsDim: 'Heces verdes + poliuria + decaimiento = psitacosis / metal pesado',
    triageHintEnergy0: 'Activo y reactivo — saludable',
    triageHintEnergy1: 'Tranquilo pero reactivo — vigila si se eriza',
    triageHintEnergy2: 'Erizado y somnoliento — suele ser etapa avanzada',
    triageHintEnergy3: 'En el suelo, ojos cerrados — crítico; ve al vet ya',
    triageHintAppetite0: 'Come normal — mantén dieta y peso diario',
    triageHintAppetite1: 'Come menos — señal temprana; pesa a diario',
    triageHintAppetite2: 'Rechaza la comida — calienta y alimenta asistido; vet pronto',
    triageHintAppetite3: 'Sin comer 24h+ — agotamiento agudo; urgencias',
    triageHintBreathing0: 'Respiración pareja, sin ruidos',
    triageHintBreathing1: 'Estornudo ocasional / secreción nasal — irritación leve',
    triageHintBreathing2: 'Movimiento de cola en reposo — respira con esfuerzo',
    triageHintBreathing3: 'Respiración con pico abierto sostenida — urgencia respiratoria',
    triageHintDroppings0: 'Color y forma normales',
    triageHintDroppings1: 'Blando o poliuria — vigila si persiste',
    triageHintDroppings2: 'Comida sin digerir — problema digestivo o de buche',
    triageHintDroppings3: 'Verde/amarillo o sangre — psitacosis o metal pesado',
    triageLevel0: 'Riesgo bajo', triageLevel1: 'Observar en casa', triageLevel2: 'Ve al vet', triageLevel3: 'Urgencias',
    triageSummary0: 'Sobre todo signos normales; sin disparadores de urgencia.',
    triageSummary1: 'Anomalías leves — observa en casa y revisa en 24h.',
    triageSummary2: 'Señales anormales claras — ve a un vet de exóticos pronto.',
    triageSummary3: 'Señal de urgencia — busca atención de inmediato.',
    triageAdvice0: 'Cuidado rutinario: temperatura estable, pesaje diario, limpieza.',
    triageAdvice1: 'Ventila 20 min, abriga, revisa en 24h, pesa a diario.',
    triageAdvice2: 'Visita un vet de exóticos en 24-48h; lleva notas de síntomas.',
    triageAdvice3: 'Urgencia: traslada a 31-32°C, lleva muestra fresca de heces; si sospechas psitacosis, aísla otras aves y usa guantes; contacta un vet ahora.',
    triageFlagBreathing: 'Respiración con pico abierto sostenida → dificultad respiratoria (infección / cuerpo extraño / hipoxia)',
    triageFlagTox: 'Heces verdes/sangre + decaimiento → psitacosis o metal pesado (zoonosis, precauciones)',
    triageFlagAppetite: 'Sin comer 24h+ → agotamiento agudo; calienta y alimenta asistido',
    triageFlagCritical: 'En el suelo, ojos cerrados → crítico; busca atención ya',
    medTypeSymptom: 'Síntoma', medTypeDiagnosis: 'Diagnóstico', medTypeMedication: 'Medicación', medTypeRecheck: 'Revisión', medTypeOther: 'Otro',
    medDateLabel: 'Fecha', medTypeLabel: 'Tipo', medMore: 'Más (hospital / teléfono)', medLess: 'Menos', medCancel: 'Cancelar',
    medContentPlaceholder: 'Contenido, p. ej. decaimiento, come menos, respuesta a medicación…',
    medHospitalPlaceholder: 'Hospital (opcional)', medPhonePlaceholder: 'Teléfono (opcional)', medAdd: 'Añadir registro',
    medHealthTitle: 'Puntuación de salud', medHealthSubtitle: 'Basado en registros de los últimos 90 días', medHealthScore: 'Global',
    medHealthLevelGood: 'Excelente', medHealthLevelFair: 'Aceptable', medHealthLevelCaution: 'Atención', medHealthLevelWarning: 'Requiere atención',
    medNoRecordsHint: 'Sin registros aún; mantén la observación diaria.',
    medHealthTypeDist: 'Tipos de registro', medHealthTypeDistSub: 'Por tipo', medHealthNoTypeHint: 'Sin estadísticas aún.',
    medHealthTrend: 'Tendencia', medHealthTrendSub: 'Últimos 6 meses', medHealthNoTrendHint: 'Añade registros para ver la tendencia.', medHealthRecordCount: 'Registros',
    medHealthAnalysis: 'Análisis de salud', medLastRecord: 'Último registro', medDaysAgo: ' días', medMostFrequent: 'Más frecuente', medLastSymptom: 'Último síntoma',
    medHealthAdvice0: 'Puntuación excelente — mantén el cuidado rutinario y el pesaje diario.',
    medHealthAdvice1: 'Puntuación aceptable — vigila signos leves, revisa en 24h.',
    medHealthAdvice2: 'Puntuación baja — varias visitas/síntomas en 90 días; consulta un vet de exóticos.',
    medHealthAdvice3: 'Requiere atención — síntomas/visitas recientes concentrados; contacta un vet pronto.',
    medRecentRecords: 'Registros recientes',
  },
  ja: {
    birth: '出生', addProfile: '記録を追加', editProfile: '基本情報を編集', weightRecord: '体重記録',
    growthAlbum: '成長アルバム', recordWeight: '体重を入力', todayWeight: '今日の体重',
    autoArchive: 'スクリーンショットと睡眠写真は自動で保存されます。', weightSaved: '体重を保存しました', weightChart: '体重記録曲線',
    weightAxis: '体重 / g', editTime: '編集時間', grams: 'グラム', parrotSpecies: 'インコ種類',
    parrotName: 'インコの名前', birthday: '生年月日', ageStage: '年齢区分', currentWeight: '現在体重',
    diagnosisTitle: '外見チェック問診', energy: '元気度', appetite: '食欲', breathing: '呼吸状態',
    droppings: '排泄状態', myLocation: '現在地', name: '名称', description: '説明',
    editPlaceholder: '変更したい情報をここに入力します。', ledgerTotal: '総支出', searchLedger: '支出記録を検索',
    ledgerDate: '日付', createdAt: '作成時間', ledgerTag: '分類', ledgerDescription: '説明',
    ledgerAmount: '金額', updatedAt: '更新時間', action: '操作', created: '作成', updated: '更新',
    unedited: '未編集', tagPlaceholder: 'タグ：主食/医療/用品', descriptionPlaceholder: '説明：おもちゃベル',
    amountPlaceholder: '金額：29',
    careProfileFallback: 'この種類の専用ガイド未登録、汎用アドバイスを表示',
    careProfileNoArchive: 'より精度の高い推奨にはペット記録で種類を入力してください',
    careProfileOverview: '種類情報', careProfileOrigin: '原産地', careProfileBodyLength: 'サイズ', careProfileWeight: '体重',
    careProfileLifespan: '寿命', careProfileTemperament: '性格', careProfileTalking: 'お話し能力',
    careProfileRisks: '主なリスク', careProfileEnv: '適正環境',
    careProfileTempRange: '適温', careProfileHumidityRange: '適湿',
    careProfileDustLevel: '羽粉量', careProfileDustTolerance: '粉塵耐性',
    careProfileDiet: '食事', careProfileDietRate: '配合比',
    careProfileRecommended: '推奨食品', careProfileToxic: '禁忌食品',
    careProfileAdvice: '専用ケアアドバイス',
    careProfileEnvPreview: 'プレビュー：現在の環境データでこの種類の適合度を評価',
    tutorialListTitle: '飼育チュートリアル', tutorialCount: '全 {n} 件', tutorialEmpty: '該当するチュートリアルがありません', tutorialBack: 'チュートリアル一覧へ戻る', tutorialRead: '読む',
    envScoreTitle: '環境適合スコア', envScoreSubtitle: '適正範囲とリアルタイム監視に基づく',
    envLoading: '環境データを読み込み中…', envNotConnected: 'リアルタイム未接続、タップで更新', envRefresh: '更新', envNoData: 'データなし',
    envPartialNote: '⚠ 一部センサー未接続、スコアは参考値',
    envLastRecord: '前回値',
    envTemp: '温度', envHumidity: '湿度', envDust: '粉塵',
    envSuitable: '適正', envLow: '低い', envHigh: '高い',
    envDustGood: '良好', envDustWarn: '偏高、換気推奨', envDustBad: '高すぎ、即時換気',
    envLevelGood: '優秀', envLevelFair: '良好', envLevelWarn: '注意', envLevelBad: '要対応',
    envRange: '適正範囲', envCurrent: '現在',
    triageResultTitle: '問診結果', triageSubtitle: '実際の様子を選択 — リスクがリアルタイム更新', triageLiveRisk: '現在のリスク', triageAdviceLabel: '対応',
    triageDisclaimer: 'AIによる問診であり、エキゾチック獣医の診察を代替するものではありません。鳥は病気を隠します、複数の異常が同時に出たら直ちに受診してください。',
    triageHintEnergyDim: '鳥は病気を隠す、元気度の変化は中期〜後期のことが多い',
    triageHintAppetiteDim: '体重減少は鳥の病気で最も早いシグナル',
    triageHintBreathingDim: '尾の上下動＋開口呼吸＝呼吸窮迫',
    triageHintDroppingsDim: '黄緑便＋多尿＋元気低下＝オウム病／重金属中毒のリスク',
    triageHintEnergy0: '活発で反応あり—健康',
    triageHintEnergy1: 'やや静かだが反応あり—蓬毛に注意',
    triageHintEnergy2: '蓬毛で眠そう—中期〜後期の可能性',
    triageHintEnergy3: 'ケージの底で目を閉じる—危篤、直ちに受診',
    triageHintAppetite0: '通常通り食べる—日常の食事と毎日計量',
    triageHintAppetite1: '食事量低下—初期シグナル、毎日計量',
    triageHintAppetite2: '明らかに拒食—保温・補食し早めに受診',
    triageHintAppetite3: '24時間以上未食—急性エネルギー枯渇、緊急',
    triageHintBreathing0: '呼吸は整い、雑音なし',
    triageHintBreathing1: '時々くしゃみ・鼻分泌物—軽度の刺激',
    triageHintBreathing2: '安静時に尾が上下—呼吸が苦しい',
    triageHintBreathing3: '持続する開口呼吸—呼吸窮迫の緊急',
    triageHintDroppings0: '色・形とも正常',
    triageHintDroppings1: 'ゆるい・多尿—持続を観察',
    triageHintDroppings2: '未消化物—消化器・嗉囊の問題',
    triageHintDroppings3: '黄緑・血便—オウム病や重金属中毒のリスク',
    triageLevel0: '低リスク', triageLevel1: '自宅で経過観察', triageLevel2: '受診推奨', triageLevel3: '緊急受診',
    triageSummary0: '主に正常所見、緊急トリガーなし。',
    triageSummary1: '軽度の異常—自宅で経過観察し24時間以内に再確認。',
    triageSummary2: '明らかな異常シグナル—早めにエキゾチック病院を受診。',
    triageSummary3: '緊急シグナル—直ちに受診。',
    triageAdvice0: '日常ケア：温度安定・毎日計量・清掃リズム。',
    triageAdvice1: '20分換気・保温・24時間以内に再確認し毎日計量。',
    triageAdvice2: '24-48時間以内にエキゾチック病院を受診、症状メモを持参。',
    triageAdvice3: '緊急：31-32°Cで搬送、新鮮な糞便サンプル持参；オウム病疑いなら他鳥を隔離し手袋着用、今すぐ連絡。',
    triageFlagBreathing: '持続する開口呼吸→呼吸窮迫（気道感染・異物・低酸素）',
    triageFlagTox: '黄緑・血便＋元気低下→オウム病または重金属中毒（人獣共通、防護）',
    triageFlagAppetite: '24時間以上未食→急性エネルギー枯渇、保温・補食',
    triageFlagCritical: 'ケージの底で目を閉じる→危篤、直ちに受診',
    medTypeSymptom: '症状', medTypeDiagnosis: '診察', medTypeMedication: '投薬', medTypeRecheck: '再診', medTypeOther: 'その他',
    medDateLabel: '日付', medTypeLabel: '種類', medMore: '詳細（病院・電話）', medLess: '閉じる', medCancel: 'キャンセル',
    medContentPlaceholder: '内容：元気低下・食事量減少・投薬反応など',
    medHospitalPlaceholder: '病院名（任意）', medPhonePlaceholder: '電話（任意）', medAdd: '記録を追加',
    medHealthTitle: '健康スコア', medHealthSubtitle: '過去90日の記録に基づく総合評価', medHealthScore: '総合',
    medHealthLevelGood: '優秀', medHealthLevelFair: '良好', medHealthLevelCaution: '注意', medHealthLevelWarning: '要対応',
    medNoRecordsHint: '記録なし、日常観察を維持。',
    medHealthTypeDist: '記録タイプ分布', medHealthTypeDistSub: 'タイプ別', medHealthNoTypeHint: '統計なし。',
    medHealthTrend: '記録傾向', medHealthTrendSub: '過去6か月', medHealthNoTrendHint: '記録を追加すると傾向を表示。', medHealthRecordCount: '件',
    medHealthAnalysis: '健康分析', medLastRecord: '直近の記録', medDaysAgo: '日前', medMostFrequent: '最多タイプ', medLastSymptom: '直近の症状',
    medHealthAdvice0: 'スコア優秀—日常ケアと毎日計量を維持。',
    medHealthAdvice1: 'スコア良好—軽微な異常に注意、24時間以内に再確認。',
    medHealthAdvice2: 'スコア低—過去90日に受診/症状多め、エキゾチック病院で再診を。',
    medHealthAdvice3: '要対応—直近に症状/受診が集中、早めに獣医に連絡。',
    medRecentRecords: '直近のカルテ',
  },
}

const VALUE_LABELS = {
  en: {
    小太阳: 'Sun conure', 金太阳: 'Sun conure', 虎皮: 'Budgie', 玄凤: 'Cockatiel', 牡丹: 'Lovebird',
    和尚: 'Monk parakeet', 吸蜜: 'Lory', 凯克: 'Caique', 黑顶: 'Black-headed caique', 折衷: 'Eclectus', 裸胸: 'Bare-eyed cockatoo',
    幼年: 'Juvenile', 青少年: 'Adolescent', 成年: 'Adult', 老年: 'Senior', 公: 'Male', 母: 'Female', 未知: 'Unknown',
    站立: 'Standing', 吃东西: 'Eating', 睡觉: 'Sleeping', 当前状态站立: 'Standing', 当前状态吃东西: 'Eating', 当前状态睡觉: 'Sleeping',
    精神很好: 'Bright', 精神一般: 'Normal', 明显萎靡: 'Lethargic', 正常进食: 'Eating normally', 食量下降: 'Eating less', 拒食: 'Refusing food',
    无异常: 'No issue', 偶尔张口: 'Occasional open-mouth breathing', 持续张口呼吸: 'Persistent open-mouth breathing', 正常: 'Normal', 偏稀: 'Loose', 颜色异常: 'Abnormal color',
    主粮: 'Food', 用品: 'Supplies', 医疗: 'Medical', 日常用品: 'Daily supplies', 其他: 'Other',
    主粮补充装: 'food refill pack', 磨爪站杆: 'claw-grinding perch', 体检挂号: 'checkup registration',
    精神活跃: 'Bright & active', 略显安静: 'Quiet but responsive', 蓬毛嗜睡: 'Fluffed & drowsy', 伏底闭眼: 'Floor, eyes closed',
    明显拒食: 'Refusing food', 拒食逾24h: 'No food 24h+',
    偶尔喷嚏: 'Occasional sneeze', 尾上下摆: 'Tail bobbing',
    偏稀多尿: 'Loose / polyuria', 含未消化: 'Undigested food', 黄绿或血便: 'Green / bloody',
    '日期格式应为 xxxx-xx-xx': 'Use yyyy-mm-dd format',
  },
  es: {
    小太阳: 'Cotorra sol', 金太阳: 'Cotorra sol', 虎皮: 'Periquito', 玄凤: 'Ninfa', 牡丹: 'Agapornis',
    和尚: 'Cotorra monje', 吸蜜: 'Lori', 凯克: 'Caique', 黑顶: 'Caique cabecinegro', 折衷: 'Eclectus', 裸胸: 'Cacatúa de ojos desnudos',
    幼年: 'Cría', 青少年: 'Joven', 成年: 'Adulto', 老年: 'Mayor', 公: 'Macho', 母: 'Hembra', 未知: 'Desconocido',
    站立: 'De pie', 吃东西: 'Comiendo', 睡觉: 'Durmiendo', 当前状态站立: 'De pie', 当前状态吃东西: 'Comiendo', 当前状态睡觉: 'Durmiendo',
    精神很好: 'Muy activo', 精神一般: 'Normal', 明显萎靡: 'Decaído', 正常进食: 'Come normal', 食量下降: 'Come menos', 拒食: 'No come',
    无异常: 'Sin anomalía', 偶尔张口: 'Abre el pico a veces', 持续张口呼吸: 'Respira con el pico abierto', 正常: 'Normal', 偏稀: 'Blando', 颜色异常: 'Color anormal',
    主粮: 'Alimento', 用品: 'Suministros', 医疗: 'Médico', 日常用品: 'Uso diario', 其他: 'Otro',
    主粮补充装: 'recarga de alimento', 磨爪站杆: 'percha lima uñas', 体检挂号: 'registro de revisión',
    精神活跃: 'Muy activo', 略显安静: 'Tranquilo reactivo', 蓬毛嗜睡: 'Erizado somnoliento', 伏底闭眼: 'En suelo, ojos cerrados',
    明显拒食: 'Rechaza comida', 拒食逾24h: 'Sin comer 24h+',
    偶尔喷嚏: 'Estornudo ocasional', 尾上下摆: 'Movimiento de cola',
    偏稀多尿: 'Blando / poliuria', 含未消化: 'Sin digerir', 黄绿或血便: 'Verde / sangre',
    '日期格式应为 xxxx-xx-xx': 'Usa formato aaaa-mm-dd',
  },
  ja: {
    小太阳: 'コガネメキシコ', 金太阳: 'コガネメキシコ', 虎皮: 'セキセイインコ', 玄凤: 'オカメインコ', 牡丹: 'ボタンインコ',
    和尚: 'オキナインコ', 吸蜜: 'ロリキート', 凯克: 'シロハラインコ', 黑顶: 'ズグロシロハラインコ', 折衷: 'オオハナインコ', 裸胸: 'アカビタイムジオウム',
    幼年: '幼鳥', 青少年: '若鳥', 成年: '成鳥', 老年: 'シニア', 公: 'オス', 母: 'メス', 未知: '不明',
    站立: '立つ', 吃东西: '食事中', 睡觉: '睡眠中', 当前状态站立: '立つ', 当前状态吃东西: '食事中', 当前状态睡觉: '睡眠中',
    精神很好: '元気', 精神一般: '普通', 明显萎靡: '元気がない', 正常进食: '通常通り食べる', 食量下降: '食事量低下', 拒食: '拒食',
    无异常: '異常なし', 偶尔张口: '時々開口', 持续张口呼吸: '開口呼吸が続く', 正常: '正常', 偏稀: 'ゆるい', 颜色异常: '色が異常',
    主粮: '主食', 用品: '用品', 医疗: '医療', 日常用品: '日用品', 其他: 'その他',
    主粮补充装: '主食補充パック', 磨爪站杆: '爪とぎ止まり木', 体检挂号: '健康診断受付',
    精神活跃: '活発', 略显安静: 'やや静か', 蓬毛嗜睡: '蓬毛で眠そう', 伏底闭眼: '底部・目を閉じる',
    明显拒食: '明らかに拒食', 拒食逾24h: '24時間以上未食',
    偶尔喷嚏: '時々くしゃみ', 尾上下摆: '尾の上下動',
    偏稀多尿: 'ゆるい・多尿', 含未消化: '未消化', 黄绿或血便: '黄緑・血便',
    '日期格式应为 xxxx-xx-xx': 'yyyy-mm-dd 形式で入力',
  },
}

function valueText(value) {
  return VALUE_LABELS[systemPrefs.value.language]?.[value] || value
}

function profileMeta(profile, includeStatus = false) {
  const base = `${valueText(profile.species)} · ${labelText('birth')} ${profile.birthday} · ${profile.weight} · ${valueText(profile.sex)}`
  return includeStatus ? `${base} · ${valueText(profile.status)}` : base
}

function photoCountText(value) {
  const count = String(value || '').match(/\d+/)?.[0] || '0'
  const units = { zh: '张', en: 'photos', es: 'fotos', ja: '枚' }
  return `${count} ${units[systemPrefs.value.language] || units.zh}`
}

function localizedWeightNote(value) {
  return String(value || '').replace('录入', labelText('recordWeight'))
}

const HOSPITAL_ADDRESS_LABELS = {
  en: {
    h1: 'No. 565 Xujiahui Road, Huangpu District, Shanghai',
    h2: 'No. 2393 Hongqiao Road, Changning District, Shanghai',
    h3: 'No. 1786 Chengshan Road, Pudong New Area, Shanghai',
  },
  es: {
    h1: 'N.º 565, Xujiahui Road, Distrito de Huangpu, Shanghái',
    h2: 'N.º 2393, Hongqiao Road, Distrito de Changning, Shanghái',
    h3: 'N.º 1786, Chengshan Road, Nueva Área de Pudong, Shanghái',
  },
  ja: {
    h1: '上海市黄浦区徐家汇路565号',
    h2: '上海市長寧区虹橋路2393号',
    h3: '上海市浦東新区成山路1786号',
  },
}

const HOSPITAL_NAME_LABELS = {
  en: {
    h1: 'Shanghai Shenpu Pet Hospital (Huangpu Main Branch)',
    h2: 'Shanghai Naughty Family Pet Hospital (Hongqiao Main Branch)',
    h3: 'Shanghai Chongyi Pet Clinic (Pudong Branch)',
  },
  es: {
    h1: 'Hospital de Mascotas Shanghai Shenpu (Sede Central de Huangpu)',
    h2: 'Hospital de Mascotas Shanghai Naughty Family (Sede de Hongqiao)',
    h3: 'Clínica de Mascotas Shanghai Chongyi (Sucursal de Pudong)',
  },
  ja: {
    h1: '上海申普動物病院 (黄浦総院)',
    h2: '上海わんぱく家族動物病院 (虹橋総店)',
    h3: '上海寵伊動物クリニック (浦東分店)',
  },
}

function hospitalName(hospital) {
  return HOSPITAL_NAME_LABELS[systemPrefs.value.language]?.[hospital.id] || hospital.name
}

function hospitalAddress(hospital) {
  return HOSPITAL_ADDRESS_LABELS[systemPrefs.value.language]?.[hospital.id] || hospital.address
}

function ledgerTagText(record) {
  return normalizeLedgerCategory(record.tag)
}

function ledgerDescriptionText(record) {
  let description = String(record.description || '')
  if (record.system) {
    const match = description.match(/^(.+?) · (.+)$/)
    if (match) description = `${match[1]} · ${valueText(match[2])}`
  }

  // 账本接口已经按当前鹦鹉隔离记录，因此只清理与当前档案名字完全匹配的开头，
  // 避免误删正常描述中包含的点号或分隔符。
  const parrotName = String(selectedParrot.value.name || selectedParrot.value.shortName || '').trim()
  if (!parrotName || !description.startsWith(parrotName)) return description
  const suffix = description.slice(parrotName.length)
  const separators = [' · ', '·', ' • ', '•', '・', '.', '。', ':', '：']
  const separator = separators.find((item) => suffix.startsWith(item))
  return separator ? suffix.slice(separator.length).trimStart() : description
}

function ledgerSearchText(record) {
  return `${record.time}${ledgerTagText(record)}${ledgerDescriptionText(record)}${record.amount}${record.createdAt}${record.updatedAt}`
}

function normalizeLedgerCategory(category) {
  const value = String(category || '').trim()
  if (LEDGER_CATEGORIES.includes(value)) return value
  if (/主粮|零食|饲料|食物|食品/.test(value)) return '食物'
  if (/医疗|体检|药|就诊/.test(value)) return '医疗'
  if (/清洁|卫生|消毒|用品/.test(value)) return '清洁'
  if (/玩具|娱乐/.test(value)) return '玩具'
  return '其他'
}

function currentLocalDateText() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60 * 1000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

function formatLedgerAmount(value) {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

function openLedgerCreate() {
  ledgerDraft.value = {
    time: todayText.value,
    tag: '食物',
    description: '',
    amount: '',
  }
  ledgerFormError.value = ''
  openModal('ledger-create', '记一笔支出')
}

const SPECIES_API_TO_UI = {
  太阳锥尾鹦鹉: '小太阳',
  小太阳鹦鹉: '小太阳',
  虎皮鹦鹉: '虎皮',
  玄凤鹦鹉: '玄凤',
  鸡尾鹦鹉: '玄凤',
  牡丹鹦鹉: '牡丹',
  和尚鹦鹉: '和尚',
  吸蜜鹦鹉: '吸蜜',
  凯克鹦鹉: '凯克',
  黑顶凯克: '黑顶',
  折衷鹦鹉: '折衷',
  裸胸鹦鹉: '裸胸',
  金太阳鹦鹉: '金太阳',
}

const SPECIES_UI_TO_API = {
  小太阳: '太阳锥尾鹦鹉',
  虎皮: '虎皮鹦鹉',
  玄凤: '玄凤鹦鹉',
  牡丹: '牡丹鹦鹉',
  和尚: '和尚鹦鹉',
  吸蜜: '吸蜜鹦鹉',
  凯克: '凯克鹦鹉',
  黑顶: '黑顶凯克',
  折衷: '折衷鹦鹉',
  裸胸: '裸胸鹦鹉',
  金太阳: '金太阳鹦鹉',
}

const SEX_API_TO_UI = { male: '公', female: '母', unknown: '未知' }
const SEX_UI_TO_API = { 公: 'male', 母: 'female', 未知: 'unknown' }
const STATUS_API_TO_UI = { standing: '站立', eating: '吃东西', sleeping: '睡觉', calling: '大叫', flapping: '扇翅膀' }
const STATUS_UI_TO_API = { 站立: 'standing', 吃东西: 'eating', 睡觉: 'sleeping', 大叫: 'calling', 扇翅膀: 'flapping' }

function normalizeSpecies(value) {
  return SPECIES_API_TO_UI[value] || value || '小太阳'
}

function speciesToApi(value) {
  return SPECIES_UI_TO_API[value] || value || '太阳锥尾鹦鹉'
}

function sexToApi(value) {
  return SEX_UI_TO_API[value] || value || 'unknown'
}

function statusToApi(value) {
  return STATUS_UI_TO_API[value] || value || 'standing'
}

function statusFromApi(value) {
  return STATUS_API_TO_UI[value] || value || '站立'
}

function isoDateTime(date = new Date()) {
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function shortDate(value) {
  if (!value) return todayText.value.slice(5)
  return String(value).slice(5, 10)
}

function formatBackendDateTime(value) {
  if (!value) return formatStamp()
  return String(value).replace('T', ' ').slice(0, 16)
}

function weightText(value) {
  const number = Number(value)
  return Number.isFinite(number) ? `${Number(number.toFixed(1))}g` : '未录入'
}

function mapProfileFromApi(profile) {
  const id = profile.petId || profile.id
  const species = normalizeSpecies(profile.species)
  const status = statusFromApi(profile.currentStatus)
  const weight = weightText(profile.weightGrams)
  return {
    id,
    petId: id,
    deviceId: profile.deviceId || '',
    avatarType: 'avatar-orange',
    name: profile.name || '鹦鹉',
    shortName: profile.name || '鹦鹉',
    species,
    birthday: profile.birthday || todayText.value,
    weight,
    sex: SEX_API_TO_UI[profile.sex] || '未知',
    status,
    ageStage: getAgeStage(profile.birthday),
    route: '/archive',
    apiRaw: profile,
  }
}

function mapArchiveProfileFromApi(profile) {
  const parrot = mapProfileFromApi(profile)
  return {
    ...parrot,
    status: `当前状态${parrot.status}`,
    device: profile.deviceId || '未绑定设备',
    photos: '0 张',
    lastWeight: profile.weightGrams ? `${String(profile.updatedAt || profile.createdAt || todayText.value).slice(0, 10)} ${labelText('recordWeight')} ${weightText(profile.weightGrams)}` : '',
    weightHistory: profile.weightGrams ? [{ id: null, time: shortDate(profile.updatedAt || profile.createdAt || todayText.value), value: Number(profile.weightGrams) }] : [],
    apiRaw: profile,
  }
}

function mapWeightFromApi(item) {
  return {
    id: item.id,
    time: shortDate(item.measuredAt || item.createdAt),
    value: Number(item.weightGrams),
    measuredAt: item.measuredAt,
    remark: item.remark || '',
  }
}

function mapMedicalRecordFromApi(record) {
  const title = String(record.title || '').trim()
  const content = String(record.content || '').trim()
  // 当前页面用一个输入框维护病历正文；兼容旧数据中 title/content 重复的记录，
  // 只有两者确实不同时才保留“标题：正文”的完整信息。
  const detail = title && content && title !== content
    ? `${title}：${content}`
    : content || title
  return {
    id: record.recordId,
    recordId: record.recordId,
    text: `${record.recordDate || todayText.value} ${detail}`.trim(),
    recordDate: record.recordDate,
    recordType: record.recordType || 'other',
    title: record.title || '',
    content: record.content || '',
    hospitalName: record.hospitalName || '',
    hospitalPhone: record.hospitalPhone || '',
    attachments: record.attachments || [],
  }
}

// 病历卡片显示用：从 recordDate 或 text 前缀解析年月日；正文去掉日期前缀/优先用 title+content。
function medRecordDateParts(record) {
  const raw = record.recordDate || (record.text || '').match(/^\d{4}-\d{2}-\d{2}/)?.[0] || ''
  const m = raw.match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? { year: m[1], month: m[2], day: m[3] } : null
}
function medRecordMonth(record) {
  return medRecordDateParts(record)?.month || '—'
}
function medRecordDay(record) {
  return medRecordDateParts(record)?.day || '—'
}
function medRecordText(record) {
  if (record.title && record.content && record.title !== record.content) {
    return `${record.title}：${record.content}`
  }
  if (record.content) return record.content
  if (record.title) return record.title
  return (record.text || '').replace(/^\d{4}-\d{2}-\d{2}\s*/, '')
}

// 病历类型分诊（后端合法 recordType 取值 + 颜色 + i18n key）。
const MEDICAL_RECORD_TYPES = [
  { value: 'symptom', labelKey: 'medTypeSymptom', color: '#b87e16' },
  { value: 'diagnosis', labelKey: 'medTypeDiagnosis', color: '#3a7fc4' },
  { value: 'medication', labelKey: 'medTypeMedication', color: '#2f9a87' },
  { value: 'recheck', labelKey: 'medTypeRecheck', color: '#2f7d5a' },
  { value: 'other', labelKey: 'medTypeOther', color: '#6f8a93' },
]
const localizedMedRecordTypes = computed(() => MEDICAL_RECORD_TYPES.map((t) => ({
  value: t.value,
  label: labelText(t.labelKey),
  color: t.color,
})))
// 健康分析页 SFC 所需 i18n 文案包（语言切换时整体重算）。
const medHealthLabels = computed(() => ({
  title: labelText('medHealthTitle'),
  subtitle: labelText('medHealthSubtitle'),
  score: labelText('medHealthScore'),
  levelGood: labelText('medHealthLevelGood'),
  levelFair: labelText('medHealthLevelFair'),
  levelCaution: labelText('medHealthLevelCaution'),
  levelWarning: labelText('medHealthLevelWarning'),
  noRecordsHint: labelText('medNoRecordsHint'),
  typeDist: labelText('medHealthTypeDist'),
  typeDistSub: labelText('medHealthTypeDistSub'),
  noTypeHint: labelText('medHealthNoTypeHint'),
  trend: labelText('medHealthTrend'),
  trendSub: labelText('medHealthTrendSub'),
  noTrendHint: labelText('medHealthNoTrendHint'),
  recordCount: labelText('medHealthRecordCount'),
  analysis: labelText('medHealthAnalysis'),
  lastRecord: labelText('medLastRecord'),
  daysAgo: labelText('medDaysAgo'),
  mostFrequent: labelText('medMostFrequent'),
  lastSymptom: labelText('medLastSymptom'),
  advice0: labelText('medHealthAdvice0'),
  advice1: labelText('medHealthAdvice1'),
  advice2: labelText('medHealthAdvice2'),
  advice3: labelText('medHealthAdvice3'),
  recent: labelText('medRecentRecords'),
}))
function medRecordTypeLabel(type) {
  const t = MEDICAL_RECORD_TYPES.find((x) => x.value === type)
  return labelText(t ? t.labelKey : 'medTypeOther')
}
function emptyMedicalDraft() {
  return { recordDate: todayText.value, recordType: 'symptom', content: '', hospitalName: '', hospitalPhone: '' }
}
function medicalRecordDraftToRequest(draft) {
  const content = String(draft?.content || '').trim()
  const date = String(draft?.recordDate || '').trim() || new Date().toISOString().slice(0, 10)
  return {
    recordDate: date,
    recordType: draft?.recordType || 'symptom',
    title: null,
    content,
    hospitalName: String(draft?.hospitalName || '').trim() || null,
    hospitalPhone: String(draft?.hospitalPhone || '').trim() || null,
    attachments: [],
  }
}

function medicalRecordToRequest(textValue) {
  const text = String(textValue || '').trim()
  const match = text.match(/^(\d{4}-\d{2}-\d{2})\s+(.+)$/)
  const content = match ? match[2] : text
  return {
    recordDate: match ? match[1] : todayText.value,
    recordType: 'other',
    // 单文本录入不再复制一份相同标题，避免后续展示成“描述：描述”。
    title: null,
    content,
    hospitalName: null,
    hospitalPhone: null,
    attachments: [],
  }
}

function mapLedgerRecordFromApi(record) {
  return {
    id: record.ledgerId,
    ledgerId: record.ledgerId,
    time: record.expenseDate || todayText.value,
    createdAt: formatBackendDateTime(record.createdAt),
    updatedAt: record.updatedAt ? formatBackendDateTime(record.updatedAt) : '',
    tag: normalizeLedgerCategory(record.category),
    description: record.description || '',
    amount: Number(record.amount || 0),
    currency: record.currency || 'CNY',
    system: false,
    tagSystem: false,
  }
}

function mapPhotoFromApi(photo) {
  return {
    id: photo.mediaId,
    mediaId: photo.mediaId,
    parrotId: photo.petId,
    title: photo.title || ui.value.snapshotPhoto,
    savedAt: photo.capturedAt || photo.createdAt,
    time: formatBackendDateTime(photo.capturedAt || photo.createdAt),
    fileUrl: photo.fileUrl,
    image: photo.imageBase64,
    thumbnailUrl: photo.thumbnailUrl,
    tags: photo.tags || '',
    mediaType: photo.mediaType || 'photo',
    backend: true,
  }
}

function applyWeightsToSelectedProfile(weights = [], petId = selectedParrot.value.id) {
  const profile = profiles.value.find((item) => item.id === petId)
  if (!profile) return
  // 后端列表按 measuredAt 倒序返回；前端图表统一保存为“最早 → 最新”，
  // 这样横轴从左到右符合时间直觉，同时末项仍然是最新体重。
  const mapped = weights.map(mapWeightFromApi)
    .filter((item) => Number.isFinite(item.value))
    .sort((a, b) => {
      const aTime = weightTimeMs(a)
      const bTime = weightTimeMs(b)
      if (aTime == null) return bTime == null ? 0 : 1
      if (bTime == null) return -1
      return aTime - bTime
    })
  if (!mapped.length) {
    profile.weightHistory = []
    return
  }
  profile.weightHistory = mapped.slice(-12)
  const latest = mapped[mapped.length - 1]
  profile.weight = weightText(latest.value)
  profile.lastWeight = `${String(latest.measuredAt || todayText.value).slice(0, 10)} ${labelText('recordWeight')} ${profile.weight}`
  const parrot = localParrots.value.find((item) => item.id === profile.id)
  if (parrot) parrot.weight = profile.weight
  if (selectedParrot.value.id === profile.id) {
    selectedParrot.value = { ...selectedParrot.value, weight: profile.weight }
  }
}

async function loadCareBootstrap() {
  try {
    const data = await listParrots()
    const remoteProfiles = Array.isArray(data) ? data : []
    careApiReady.value = true

    const parrotsFromApi = remoteProfiles.map(mapProfileFromApi)
    const profilesFromApi = remoteProfiles.map(mapArchiveProfileFromApi)
    localParrots.value = parrotsFromApi
    profiles.value = profilesFromApi
    medicalRecords.value = []
    ledgerRecords.value = []
    basePhotoRecords.value = []

    if (!remoteProfiles.length) {
      selectedParrot.value = { ...EMPTY_REMOTE_PARROT }
      activeArchiveId.value = ''
      return
    }

    const current = parrotsFromApi.find((item) => item.id === selectedParrot.value.id) || parrotsFromApi[0]
    selectedParrot.value = current
    activeArchiveId.value = current.id
    void hydratePetAvatarPhotos()
    await loadPetResources(current.id)
  } catch (error) {
    careApiReady.value = false
    console.warn('鹦鹉照护接口暂不可用，继续使用本地演示数据：', error.message)
  }
}

async function loadPetResources(petId = selectedParrot.value.id) {
  if (!careApiReady.value || !petId) return
  medicalRecords.value = []
  ledgerRecords.value = []
  basePhotoRecords.value = []
  const [weightsResult, medicalResult, ledgerResult, photosResult] = await Promise.allSettled([
    listWeights(petId),
    listMedicalRecords(petId),
    listLedgerRecords(petId),
    listPhotos(petId),
  ])

  // 宠物切换较快时，过期请求不能覆盖新宠物已加载的资源。
  if (petId !== selectedArchivePetId.value) return

  if (weightsResult.status === 'fulfilled' && Array.isArray(weightsResult.value)) {
    applyWeightsToSelectedProfile(weightsResult.value, petId)
  }
  if (medicalResult.status === 'fulfilled' && Array.isArray(medicalResult.value)) {
    medicalRecords.value = medicalResult.value.map(mapMedicalRecordFromApi)
  }
  if (ledgerResult.status === 'fulfilled' && Array.isArray(ledgerResult.value)) {
    ledgerRecords.value = ledgerResult.value.map(mapLedgerRecordFromApi)
  }
  if (photosResult.status === 'fulfilled' && Array.isArray(photosResult.value)) {
    basePhotoRecords.value = photosResult.value.map(mapPhotoFromApi)
    const profile = profiles.value.find((item) => item.id === petId)
    if (profile) profile.photos = `${basePhotoRecords.value.length} 张`
  }
  ;[weightsResult, medicalResult, ledgerResult, photosResult]
    .filter((result) => result.status === 'rejected')
    .forEach((result) => console.warn('鹦鹉照护子资源加载失败：', result.reason?.message || result.reason))
}

function showBackendError(error) {
  openModal('risk', '后端请求失败', { value: error?.message || '请求未完成，请确认后端服务已启动。' })
}


function applyUserPreferences(preferences) {
  if (!preferences || typeof preferences !== 'object') return
  const nextPrefs = { ...systemPrefs.value }
  if (['zh', 'en', 'es', 'ja'].includes(preferences.language)) {
    nextPrefs.language = preferences.language
  }
  if (['light', 'dark'].includes(preferences.theme)) {
    nextPrefs.theme = preferences.theme
  }
  if (typeof preferences.fontFamily === 'string' && preferences.fontFamily.trim()) {
    nextPrefs.fontFamily = preferences.fontFamily.trim()
  }
  const fontSize = Number(preferences.fontSize)
  if (Number.isFinite(fontSize)) {
    nextPrefs.fontSize = Math.min(28, Math.max(12, fontSize))
  }
  if (typeof preferences.fontColor === 'string' && preferences.fontColor.trim()) {
    nextPrefs.fontColor = preferences.fontColor.trim()
  }
  systemPrefs.value = nextPrefs
  if (typeof preferences.notificationEnabled === 'boolean') {
    notificationEnabled.value = preferences.notificationEnabled
  }
  if (typeof preferences.permissionEnabled === 'boolean') {
    permissionEnabled.value = preferences.permissionEnabled
  }
  if (Object.prototype.hasOwnProperty.call(preferences, 'avatarParrotId') && preferences.avatarParrotId) {
    account.value = { ...account.value, avatarParrotId: preferences.avatarParrotId }
    settingsDraft.value = { ...settingsDraft.value, avatarParrotId: preferences.avatarParrotId }
  }
  if (Object.prototype.hasOwnProperty.call(preferences, 'petAvatarMediaMap')) {
    petAvatarMediaMap.value = normalizePetAvatarMediaMap(preferences.petAvatarMediaMap)
    void hydratePetAvatarPhotos()
  }
}

function normalizePetAvatarMediaMap(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
  return Object.fromEntries(Object.entries(value)
    .filter(([petId, mediaId]) => typeof petId === 'string' && petId && typeof mediaId === 'string' && mediaId))
}

function petAvatarSource(pet) {
  const photo = petAvatarPhotoCache.value[pet?.id]
  return photo ? photoSource(photo) : ''
}

function refreshHospitals() {
  const pins = filteredHospitalPins.value
  if (!pins.length) return
  const currentIndex = pins.findIndex((item) => item.id === selectedHospital.value?.id)
  selectedHospital.value = pins[(currentIndex + 1) % pins.length]
}

// 基于中心坐标搜索附近的宠物医院 (15公里内)
function searchNearbyHospitals(centerLatLng) {
  if (!amapInstance || !AMapClass) return

  const placeSearch = new AMapClass.PlaceSearch({
    type: '宠物服务|医疗保健服务',
    pageSize: 15,
    pageIndex: 1
  })

  placeSearch.searchNearBy('宠物医院', centerLatLng, 15000, (status, result) => {
    if (status === 'complete' && result.info === 'OK' && result.poiList && result.poiList.pois) {
      const pois = result.poiList.pois
      
      const realHospitals = pois.map((poi, index) => ({
        id: poi.id || `real-${index}`,
        name: poi.name,
        address: poi.address || '地址详见地图',
        phone: poi.tel || '暂无电话',
        lng: poi.location.lng,
        lat: poi.location.lat,
        website: poi.website || `https://ditu.amap.com/detail/${poi.id}`
      }))

      dynamicHospitalPins.value = realHospitals
      if (realHospitals.length > 0) {
        selectedHospital.value = realHospitals[0]
      }
    } else {
      console.warn('周边宠物医院检索失败或无结果:', result)
    }
  })
}

// 触发关键字搜索 (城市内或全局)
function triggerHospitalSearch(query) {
  if (!amapInstance || !AMapClass) return
  window.clearTimeout(searchTimer)
  const trimmed = query.trim()
  if (!trimmed) {
    // 搜索词为空时，自动恢复当前地图中心的周边检索
    searchNearbyHospitals(amapInstance.getCenter())
    return
  }

  const placeSearch = new AMapClass.PlaceSearch({
    type: '宠物服务|医疗保健服务',
    pageSize: 15,
    pageIndex: 1
  })

  // 直接在当前城市/全国范围内模糊检索该关键词
  placeSearch.search(trimmed, (status, result) => {
    if (status === 'complete' && result.info === 'OK' && result.poiList && result.poiList.pois) {
      const pois = result.poiList.pois
      const realHospitals = pois.map((poi, index) => ({
        id: poi.id || `real-${index}`,
        name: poi.name,
        address: poi.address || '地址详见地图',
        phone: poi.tel || '暂无电话',
        lng: poi.location.lng,
        lat: poi.location.lat,
        website: poi.website || `https://ditu.amap.com/detail/${poi.id}`
      }))

      dynamicHospitalPins.value = realHospitals
      if (realHospitals.length > 0) {
        selectedHospital.value = realHospitals[0]
        amapInstance.setZoomAndCenter(13, [realHospitals[0].lng, realHospitals[0].lat])
      }
    } else {
      // 检索无结果时清空列表
      dynamicHospitalPins.value = []
    }
  })
}

// 监听搜索框输入，防抖触发高德云搜索
let searchTimer = null
watch(
  () => hospitalSearchQuery.value,
  (query) => {
    window.clearTimeout(searchTimer)
    searchTimer = window.setTimeout(() => {
      triggerHospitalSearch(query)
    }, 600)
  }
)

// 初始化高德地图
function initAMap() {
  if (amapInstance) {
    destroyAMap()
  }

  window._AMapSecurityConfig = {
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || '',
  }

  AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY || '',
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Geolocation', 'AMap.PlaceSearch', 'AMap.CitySearch'],
  }).then((AMap) => {
    const container = document.getElementById('amap-container')
    if (!container) return

    AMapClass = AMap
    // 默认定位坐标（如果都失败）为上海徐家汇
    const defaultCenter = [121.4727, 31.2091]

    amapInstance = new AMap.Map('amap-container', {
      viewMode: '3D',
      zoom: 12,
      center: defaultCenter,
    })

    amapInstance.addControl(new AMap.Scale())
    amapInstance.addControl(new AMap.ToolBar())

    // 1. 使用 IP 定位获取用户当前的省份和城市（不需要浏览器授权，必定成功且速度快）
    const citySearch = new AMap.CitySearch()
    citySearch.getLocalCity((status, result) => {
      let searchCenter = new AMap.LngLat(defaultCenter[0], defaultCenter[1])
      
      if (status === 'complete' && result.info === 'OK') {
        const citybounds = result.bounds
        amapInstance.setBounds(citybounds)
        console.log('IP 自动定位成功:', result.city)
        searchCenter = amapInstance.getCenter()
      } else {
        console.warn('IP 定位失败，采用默认中心点')
      }

      // 无论 IP 定位是否成功，都拉取周边医院
      searchNearbyHospitals(searchCenter)

      // 2. 尝试使用浏览器高精度 GPS 定位（需要用户允许权限，允许后将以精确位置覆盖）
      const geolocation = new AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 8000,
        buttonPosition: 'RB',
        buttonOffset: new AMap.Pixel(60, 18),
        zoomToAccuracy: true,
        buttonDom: '<div class="amap-control amap-geolocation" style="bottom: 18px; right: 60px; height: 32px; width: 32px; border-radius: 50%;"><img src="https://a.amap.com/jsapi/static/image/plugin/waite.png" style="display: none;"></div>'
      })
      amapInstance.addControl(geolocation)
      
      geolocation.getCurrentPosition((geoStatus, geoResult) => {
        if (geoStatus === 'complete') {
          console.log('GPS 精确定位成功:', geoResult)
          // 获取更高精度的 GPS 点，重新触发周边搜索
          searchNearbyHospitals(geoResult.position)
        }
      })
    })

    mapLoaded.value = true
  }).catch((e) => {
    console.error('AMap load failed:', e)
  })
}

// 销毁高德地图实例
function destroyAMap() {
  if (amapInstance) {
    amapInstance.destroy()
    amapInstance = null
  }
  amapMarkers = []
  mapLoaded.value = false
}

// 监听选中医院的变化，平移地图中心
watch(
  () => selectedHospital.value,
  (hospital) => {
    if (amapInstance && hospital && hospital.lng) {
      amapInstance.setZoomAndCenter(14, [hospital.lng, hospital.lat])
    }
  }
)

// 监听过滤后的医院列表，更新地图上的标记
watch(
  () => filteredHospitalPins.value,
  (newPins) => {
    if (!amapInstance) return
    // 清除旧的标记
    amapMarkers.forEach(m => amapInstance.remove(m))
    amapMarkers = []

    // 重新绘制标记
    amapMarkers = newPins.map(hospital => {
      const marker = new AMapClass.Marker({
        position: [hospital.lng, hospital.lat],
        title: hospital.name,
        map: amapInstance,
      })

      marker.on('click', () => {
        selectedHospital.value = hospital
      })

      return marker
    })

    // 如果当前选中的医院不在过滤结果中，且过滤结果不为空，默认选中第一个
    if (newPins.length > 0 && (!selectedHospital.value || !newPins.find(p => p.id === selectedHospital.value.id))) {
      selectedHospital.value = newPins[0]
    }
  }
)

async function hydratePetAvatarPhotos() {
  if (!careApiReady.value) return
  const entries = Object.entries(petAvatarMediaMap.value)
    .filter(([petId]) => profiles.value.some((profile) => profile.id === petId))
  if (!entries.length) {
    petAvatarPhotoCache.value = {}
    return
  }
  const resolved = await Promise.all(entries.map(async ([petId, mediaId]) => {
    try {
      const photos = await listPhotos(petId)
      const matched = Array.isArray(photos) && photos.find((photo) => photo.mediaId === mediaId)
      return matched ? [petId, mapPhotoFromApi(matched)] : null
    } catch {
      return null
    }
  }))
  petAvatarPhotoCache.value = Object.fromEntries(resolved.filter(Boolean))
}

function openPetAvatarPicker() {
  openModal('pet-avatar-picker', '选择成长相册照片', selectedArchive.value)
}

async function selectPetAvatarPhoto(photo) {
  const petId = selectedArchive.value?.id
  if (!petId || !photo) return
  let selectedPhoto = photo
  try {
    // 本地截图没有 mediaId 时先补归档，随后再把获得的媒体 ID 写入用户偏好表。
    if (!selectedPhoto.mediaId) {
      if (!careApiReady.value || !selectedPhoto.image) {
        throw new Error('该截图尚未归档，请在后端连接正常后重试')
      }
      const saved = await createPhoto(petId, {
        mediaType: 'screenshot',
        title: selectedPhoto.title || ui.value.snapshotPhoto,
        imageBase64: selectedPhoto.image,
        thumbnailUrl: null,
        tags: '监控,截图,头像',
      })
      selectedPhoto = mapPhotoFromApi(saved)
      capturedPhotos.value = capturedPhotos.value.filter((item) => photoKey(item) !== photoKey(photo))
      basePhotoRecords.value = [selectedPhoto, ...basePhotoRecords.value]
    }
    const nextMap = { ...petAvatarMediaMap.value, [petId]: selectedPhoto.mediaId }
    petAvatarPhotoCache.value = { ...petAvatarPhotoCache.value, [petId]: selectedPhoto }
    await savePreferencePatch({ petAvatarMediaMap: nextMap })
    closeModal()
  } catch (error) {
    showBackendError(error)
  }
}

function applyPreferencePatchLocally(patch) {
  if (!patch || typeof patch !== 'object') return
  const nextPrefs = { ...systemPrefs.value }
  if (patch.language) nextPrefs.language = patch.language
  if (patch.theme) nextPrefs.theme = patch.theme
  if (patch.fontFamily) nextPrefs.fontFamily = patch.fontFamily
  if (patch.fontSize !== undefined && patch.fontSize !== null) nextPrefs.fontSize = Number(patch.fontSize)
  if (patch.fontColor) nextPrefs.fontColor = patch.fontColor
  systemPrefs.value = nextPrefs
  if (typeof patch.notificationEnabled === 'boolean') notificationEnabled.value = patch.notificationEnabled
  if (typeof patch.permissionEnabled === 'boolean') permissionEnabled.value = patch.permissionEnabled
  if (Object.prototype.hasOwnProperty.call(patch, 'avatarParrotId')) {
    account.value = { ...account.value, avatarParrotId: patch.avatarParrotId }
    settingsDraft.value = { ...settingsDraft.value, avatarParrotId: patch.avatarParrotId }
  }
  if (Object.prototype.hasOwnProperty.call(patch, 'petAvatarMediaMap')) {
    petAvatarMediaMap.value = normalizePetAvatarMediaMap(patch.petAvatarMediaMap)
  }
}

async function loadUserPreferences() {
  try {
    const preferences = await getUserPreferences()
    preferenceApiReady.value = true
    applyUserPreferences(preferences)
  } catch (error) {
    preferenceApiReady.value = false
    console.warn('鐢ㄦ埛鍋忓ソ鎺ュ彛鏆備笉鍙敤锛屼繚鐣欏墠绔粯璁よ缃細', error.message)
  }
}

async function savePreferencePatch(patch) {
  applyPreferencePatchLocally(patch)
  try {
    const preferences = await updateUserPreferences(patch)
    preferenceApiReady.value = true
    applyUserPreferences(preferences)
  } catch (error) {
    preferenceApiReady.value = false
    showBackendError(error)
  }
}

function toggleNotificationPreference() {
  savePreferencePatch({ notificationEnabled: !notificationEnabled.value })
}

function togglePermissionPreference() {
  savePreferencePatch({ permissionEnabled: !permissionEnabled.value })
}

function syncAccountFromUser(user) {
  if (!user) return
  loginUser.value = user
  // 后端返回了 avatarImage 时同步到 account，同时刷新 localStorage 缓存
  const serverAvatar = user.avatarImage || ''
  if (serverAvatar) {
    try { localStorage.setItem('parrotUserAvatar', serverAvatar) } catch { /* quota */ }
  }
  account.value = {
    ...account.value,
    userId: user.userId ?? '',
    username: user.username || account.value.username,
    userId: user.userId != null ? String(user.userId) : account.value.userId,
    phone: user.phone || '',
    email: user.email || '',
    location: user.location || '',
    phoneBound: Boolean(user.phone),
    emailBound: Boolean(user.email),
    avatarImage: serverAvatar || account.value.avatarImage || '',
  }
  settingsDraft.value = { ...account.value }
  try {
    localStorage.setItem('parrotAuthUser', JSON.stringify({
      ...account.value,
      userRole: user.userRole || loginUser.value?.userRole,
    }))
  } catch {
    // Local persistence is optional.
  }
}

async function loadUserProfile() {
  try {
    const user = await fetchUserProfile()
    syncAccountFromUser(user)
    return user
  } catch (error) {
    console.warn('获取登录用户资料失败：', error?.message)
    return null
  }
}

async function handleLoginSuccess() {
  isAuthenticated.value = true
  loadUserPreferences()
  loadUserProfile()
  if (!careApiReady.value) {
    loadCareBootstrap()
  }
}

function handleLogout() {
  localStorage.removeItem('parrotAuthToken')
  localStorage.removeItem('parrotAuthUser')
  loginUser.value = null
  isAuthenticated.value = false
  careApiReady.value = false
  preferenceApiReady.value = false
  activeRoute.value = ''
  thirdView.value = ''
  petSwitchOpen.value = false
  modal.value = null
}

function confirmDeleteAccount() {
  openModal('confirm-delete-account', text.value.deleteAccountTitle, {
    username: account.value.username,
    warning: text.value.deleteAccountWarning,
  })
}

function togglePasswordChange() {
  passwordChanging.value = !passwordChanging.value
  passwordMessage.value = ''
  oldPassword.value = ''
  newPassword.value = ''
  newPasswordConfirm.value = ''
}

async function submitPasswordChange() {
  passwordMessage.value = ''
  if (!oldPassword.value || !newPassword.value || !newPasswordConfirm.value) {
    passwordMessage.value = '请填写完整'
    return
  }
  if (newPassword.value.length < 6) {
    passwordMessage.value = '新密码长度至少 6 位'
    return
  }
  if (newPassword.value !== newPasswordConfirm.value) {
    passwordMessage.value = text.value.passwordMismatch
    return
  }
  try {
    await apiChangePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    passwordMessage.value = text.value.passwordChanged
    oldPassword.value = ''
    newPassword.value = ''
    newPasswordConfirm.value = ''
    passwordChanging.value = false
  } catch (error) {
    passwordMessage.value = error?.message || text.value.wrongPassword
  }
}

async function executeDeleteAccount() {
  try {
    await apiDeleteAccount()
    handleLogout()
  } catch (error) {
    openModal('risk', text.value.deleteAccountTitle, {
      value: error?.message || '账号注销失败，请稍后重试',
    })
  }
}

function localizeCurve(curve) {
  const kind = metricCurveKind(curve) || (curve.unit === 'g' ? 'weight' : '')
  const copy = ui.value.curves?.[kind]
  return {
    ...curve,
    label: copy?.[0] || curve.label,
    axis: copy?.[1] || curve.axis,
    value: curve.value === '低' ? labelText('low') : curve.value === '中' ? labelText('mid') : curve.value === '高' ? labelText('high') : curve.value,
  }
}

function localizedXAxis(labels = []) {
  const maps = {
    en: { 周一: 'Mon', 周二: 'Tue', 周三: 'Wed', 周四: 'Thu', 周五: 'Fri', 周六: 'Sat', 周日: 'Sun', 第1周: 'Week 1', 第2周: 'Week 2', 第3周: 'Week 3', 第4周: 'Week 4' },
    es: { 周一: 'Lun', 周二: 'Mar', 周三: 'Mié', 周四: 'Jue', 周五: 'Vie', 周六: 'Sáb', 周日: 'Dom', 第1周: 'Semana 1', 第2周: 'Semana 2', 第3周: 'Semana 3', 第4周: 'Semana 4' },
    ja: { 周一: '月', 周二: '火', 周三: '水', 周四: '木', 周五: '金', 周六: '土', 周日: '日', 第1周: '第1週', 第2周: '第2週', 第3周: '第3週', 第4周: '第4週' },
  }
  return labels.map((label) => maps[systemPrefs.value.language]?.[label] || label)
}

function metricGaugeTitle(item) {
  const gaugeWords = { zh: '仪表盘', en: 'Gauge', es: 'Indicador', ja: 'メーター' }
  return `${item.label} ${gaugeWords[systemPrefs.value.language] || gaugeWords.zh}`
}

function sentenceBreak() {
  return systemPrefs.value.language === 'zh' || systemPrefs.value.language === 'ja' ? '。' : '. '
}

function photoKey(photo) {
  return photo.mediaId || photo.id || `${photo.title}-${photo.time}`
}

function escapeSvgText(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function photoSource(photo) {
  if (photo.thumbnailUrl) return photo.thumbnailUrl
  if (photo.fileUrl && !String(photo.fileUrl).startsWith('local-snapshot://')) return photo.fileUrl
  if (photo.image) return photo.image
  const title = escapeSvgText(photo.title || ui.value.snapshotPhoto)
  const time = escapeSvgText(photo.time || '')
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="960" height="600" viewBox="0 0 960 600">
      <defs>
        <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
          <stop stop-color="#fde6c6"/>
          <stop offset="1" stop-color="#d8eaf5"/>
        </linearGradient>
      </defs>
      <rect width="960" height="600" rx="42" fill="url(#bg)"/>
      <circle cx="760" cy="150" r="54" fill="#fff2a6"/>
      <path d="M165 405h630" stroke="#b98146" stroke-width="34" stroke-linecap="round"/>
      <ellipse cx="480" cy="442" rx="170" ry="34" fill="#8d7969" opacity=".35"/>
      <ellipse cx="450" cy="300" rx="118" ry="78" fill="#ff761f"/>
      <ellipse cx="555" cy="310" rx="145" ry="58" fill="#f36b1d"/>
      <ellipse cx="380" cy="285" rx="60" ry="95" fill="#1f1f1f"/>
      <circle cx="430" cy="245" r="50" fill="#fff"/>
      <circle cx="450" cy="232" r="11" fill="#1d1d1d"/>
      <text x="72" y="92" fill="#5a3214" font-family="Arial, sans-serif" font-size="42" font-weight="800">${title}</text>
      <text x="72" y="144" fill="#805229" font-family="Arial, sans-serif" font-size="28" font-weight="700">${time}</text>
    </svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

function photoFileName(photo) {
  return `${(photo.title || 'parrot-photo').replace(/[\\/:*?"<>|]/g, '-')}-${(photo.time || todayText.value).replace(/[\\/:*?"<>| ]/g, '-')}.png`
}

function downloadPhoto(photo) {
  const link = document.createElement('a')
  link.href = photoSource(photo)
  link.download = photoFileName(photo)
  document.body.appendChild(link)
  link.click()
  link.remove()
}

function exportSelectedPhotos() {
  const photos = selectedPhotoObjects.value
  if (!photos.length) {
    openModal('risk', ui.value.noSelection, { value: ui.value.noSelection })
    return
  }
  photos.forEach((photo, index) => {
    window.setTimeout(() => downloadPhoto(photo), index * 120)
  })
}

async function callBatchDeletePhotos(keys) {
  const photos = selectedPhotoObjects.value.filter((photo) => keys.includes(photoKey(photo)))
  const remotePhotos = photos.filter((photo) => photo.backend && photo.mediaId)
  if (careApiReady.value && remotePhotos.length !== photos.length) {
    showBackendError(new Error('所选相片缺少后端 mediaId，无法同步删除数据库记录。'))
    return false
  }
  if (!remotePhotos.length) return true
  try {
    await Promise.all(remotePhotos.map((photo) => deletePhotoApi(selectedParrot.value.id, photo.mediaId)))
    return true
  } catch (error) {
    showBackendError(error)
    return false
  }
}

function removeLocalPhotos(keys) {
  const deleteSet = new Set(keys)
  const petId = selectedArchivePetId.value
  const selectedAvatarMediaId = petAvatarMediaMap.value[petId]
  capturedPhotos.value = capturedPhotos.value.filter((photo) => !deleteSet.has(photoKey(photo)))
  basePhotoRecords.value = basePhotoRecords.value.filter((photo) => !deleteSet.has(photoKey(photo)))
  localStorage.setItem('parrotArchiveSnapshots', JSON.stringify(capturedPhotos.value))

  const profile = profiles.value.find((item) => item.id === selectedParrot.value.id)
  if (profile) profile.photos = `${archivePhotoRecords.value.length} 张`
  if (selectedAvatarMediaId && deleteSet.has(selectedAvatarMediaId)) {
    const nextMap = { ...petAvatarMediaMap.value }
    delete nextMap[petId]
    petAvatarPhotoCache.value = Object.fromEntries(Object.entries(petAvatarPhotoCache.value)
      .filter(([cachedPetId]) => cachedPetId !== petId))
    void savePreferencePatch({ petAvatarMediaMap: nextMap })
  }
}

async function deletePhotos() {
  const keys = [...selectedPhotoKeys.value]
  if (!keys.length) {
    openModal('risk', ui.value.noSelection, { value: ui.value.noSelection })
    return
  }

  const deleted = await callBatchDeletePhotos(keys)
  if (!deleted) return
  removeLocalPhotos(keys)
  selectedPhotoKeys.value = []
  gallerySelectMode.value = false
}

function toggleGallerySelectMode() {
  gallerySelectMode.value = !gallerySelectMode.value
  if (!gallerySelectMode.value) selectedPhotoKeys.value = []
}

function selectAllGalleryPhotos() {
  gallerySelectMode.value = true
  selectedPhotoKeys.value = localizedArchivePhotoRecords.value.map((photo) => photoKey(photo))
}

function togglePhotoSelection(photo) {
  const key = photoKey(photo)
  selectedPhotoKeys.value = selectedPhotoKeys.value.includes(key)
    ? selectedPhotoKeys.value.filter((item) => item !== key)
    : [...selectedPhotoKeys.value, key]
}

function handlePhotoClick(photo) {
  if (gallerySelectMode.value) {
    togglePhotoSelection(photo)
    return
  }
  openModal('photo-preview', photo.title, photo)
}

function resetDetailState() {
  thirdView.value = ''
  petSwitchOpen.value = false
  gallerySelectMode.value = false
  selectedPhotoKeys.value = []
}

function handleOpen(entry) {
  lastOpenedRoute.value = entry.route
  if (entry.key && notificationBadges.value[entry.key]) {
    notificationBadges.value = { ...notificationBadges.value, [entry.key]: 0 }
    persistReadBadge(entry.key)
  }
  resetDetailState()

  if (entry.route === '/monitor') return
  if (entry.route?.startsWith('/monitor/records')) {
    activeRoute.value = '/growth-report'
    activeReportRange.value = '周报'
    return
  }
  if (detailViews[entry.route]) {
    activeRoute.value = entry.route
  }
}

function togglePetSwitch() {
  petSwitchOpen.value = !petSwitchOpen.value
}

function selectParrot(parrot) {
  selectedParrot.value = parrot
  petSwitchOpen.value = false
  activeArchiveId.value = parrot.id
  if (thirdView.value.startsWith('archive:')) thirdView.value = `archive:${parrot.id}`
  loadPetResources(parrot.id)
}

function goHome() {
  activeRoute.value = ''
  resetDetailState()
}

function goBack() {
  if (thirdView.value) {
    thirdView.value = ''
    return
  }
  goHome()
}

function openThird(view) {
  thirdView.value = view
  petSwitchOpen.value = false
}

async function loadTutorialArticle(id) {
  const tutorial = tutorials.find((item) => item.id === id)
  if (!tutorial?.article) {
    tutorialArticleError.value = '未找到教程内容'
    tutorialArticleHtml.value = ''
    return
  }

  tutorialArticleLoading.value = true
  tutorialArticleError.value = ''
  tutorialArticleHtml.value = ''

  try {
    const res = await axios.get(tutorial.article, { responseType: 'text' })
    const md = typeof res.data === 'string' ? res.data : String(res.data ?? '')
    tutorialArticleHtml.value = parseMarkdown(md)
  } catch (e) {
    tutorialArticleError.value = `教程加载失败：${e.message}`
  } finally {
    tutorialArticleLoading.value = false
  }
}

function openTutorialDetail(id) {
  activeTutorialId.value = id
  thirdView.value = 'tutorial-detail'
  loadTutorialArticle(id)
}

function openModal(type, title, item = null) {
  petSwitchOpen.value = false
  modal.value = { type, title, item }
}

function closeModal() {
  modal.value = null
}

async function openApiKeysModal() {
  apiKeyMessage.value = ''
  apiKeySaving.value = false
  try {
    const data = await http.get('/settings/api-keys')
    apiKeyDraft.value = {
      qwenApiKey: data?.qwenApiKey || '',
      deepseekApiKey: data?.deepseekApiKey || '',
    }
  } catch {
    apiKeyDraft.value = { qwenApiKey: '', deepseekApiKey: '' }
  }
  openModal('api-keys', text.value.apiKeySettings)
}

async function saveApiKeys() {
  apiKeySaving.value = true
  apiKeyMessage.value = ''
  try {
    await http.post('/settings/api-keys', apiKeyDraft.value)
    apiKeyMessage.value = text.value.apiKeySaved
    setTimeout(() => { closeModal() }, 800)
  } catch {
    apiKeyMessage.value = text.value.apiKeySaveError
  } finally {
    apiKeySaving.value = false
  }
}

async function openQqConnectionModal() {
  qqWhitelistMessage.value = ''
  qqWhitelistSaving.value = false
  try {
    const data = await http.get('/settings/qq-whitelist')
    qqWhitelistDraft.value = data?.qqWhitelist || ''
  } catch {
    qqWhitelistDraft.value = ''
  }
  openModal('qq-whitelist', text.value.connectQq)
}

async function saveQqWhitelist() {
  qqWhitelistSaving.value = true
  qqWhitelistMessage.value = ''
  try {
    await http.post('/settings/qq-whitelist', {
      qqWhitelist: qqWhitelistDraft.value
    })
    qqWhitelistMessage.value = text.value.qqWhitelistSaved
    setTimeout(() => { closeModal() }, 800)
  } catch {
    qqWhitelistMessage.value = text.value.qqWhitelistSaveError
  } finally {
    qqWhitelistSaving.value = false
  }
}

function formatPercent(v) {
  if (v === null || v === undefined || Number.isNaN(v)) return '—'
  return `${Math.round(v * 100)}%`
}

function onBirdImageChange(e) {
  const file = e.target.files?.[0]
  birdError.value = ''
  if (birdImagePreview.value) {
    URL.revokeObjectURL(birdImagePreview.value)
    birdImagePreview.value = ''
  }
  if (!file) {
    birdImage.value = null
    return
  }
  birdImage.value = file
  birdImagePreview.value = URL.createObjectURL(file)
}

async function recognizeBird() {
  if (!birdImage.value) {
    birdError.value = labelText('chooseBirdFirst')
    return
  }
  birdLoading.value = true
  birdError.value = ''
  try {
    const data = await recognizeParrotBehavior(birdImage.value)
    openModal('bird', labelText('birdResult'), {
      detected: !!data?.parrotDetected,
      behavior: data?.behavior,
      confidence: data?.behaviorConfidence,
      species: data?.species,
      speciesConfidence: data?.speciesConfidence,
      parrotConfidence: data?.parrotConfidence,
      imageUrl: birdImagePreview.value,
    })
  } catch (e) {
    birdError.value = e.message || labelText('recognizeFail')
  } finally {
    birdLoading.value = false
  }
}

function handleMonitorFullscreenChange(isFullscreen) {
  const wasFullscreen = monitorFullscreen.value
  monitorFullscreen.value = isFullscreen

  // 全屏期间打开的仪表盘在退出全屏时应一并关闭，避免回到主页后再次出现。
  if (wasFullscreen && !isFullscreen && modal.value?.type === 'metric-gauge') {
    closeModal()
  }
}

function getAgeStage(birthday) {
  const match = /^\d{4}-\d{2}-\d{2}$/.test(birthday || '')
  if (!match) return '日期格式应为 xxxx-xx-xx'
  const birth = new Date(`${birthday}T00:00:00`)
  if (Number.isNaN(birth.getTime())) return '日期格式应为 xxxx-xx-xx'
  const ageDays = Math.floor((Date.now() - birth.getTime()) / 86400000)
  if (ageDays < 180) return '幼年'
  if (ageDays < 730) return '青少年'
  if (ageDays < 3650) return '成年'
  return '老年'
}

function submitDiagnosis() {
  const result = triageParrot(diagnosisForm.value)
  openModal('diagnosis', labelText('triageResultTitle'), result)
}


function openCurve(curve) {
  if (isReportGaugeCurve(curve)) {
    openMetricGauge(curveToMetric(curve))
    return
  }
  // 真实数据曲线自带 xAxis（时间桶标签），模拟曲线沿用 reportCurveSet 的 xAxis。
  const fallbackXAxis = reportCurveSet.value?.xAxis || []
  const xAxis = curve.xAxis?.length === curve.points?.length
    ? curve.xAxis
    : localizedXAxis(fallbackXAxis)
  openModal('curve', curve.label, { ...curve, xAxis })
  // 等 DOM 更新后初始化 ECharts
  setTimeout(() => initECharts(curve), 50)
}

function initECharts(curve) {
  if (!echartsRef.value) return
  // 销毁旧实例
  if (window._echartsInstance) window._echartsInstance.dispose()
  const chart = echarts.init(echartsRef.value)
  window._echartsInstance = chart

  const unit = curve.unit || ''
  const isTemp = unit === '℃'

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        if (p.value == null || !Number.isFinite(Number(p.value))) {
          return `${p.name}<br/>-`
        }
        const val = isTemp ? Number(p.value).toFixed(1) : Math.round(p.value)
        return `${p.name}<br/>${val}${unit}`
      }
    },
    grid: { left: 60, right: 30, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      data: curve.fullXAxis || curve.xAxis || [],
      axisLine: { lineStyle: { color: 'rgba(122,75,29,0.6)' } },
      axisLabel: { color: '#805229', fontSize: 13 }
    },
    yAxis: {
      type: 'value',
      scale: true,  // 自动适应数据范围，不强制包含 0
      axisLine: { lineStyle: { color: 'rgba(122,75,29,0.6)' } },
      axisLabel: {
        color: '#805229',
        fontSize: 14,
        formatter: (v) => isTemp ? Number(v).toFixed(1) : Math.round(v)
      },
      splitLine: { lineStyle: { color: 'rgba(122,75,29,0.1)', type: 'dashed' } }
    },
    series: [{
      data: curve.points,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#d4843e' },
      lineStyle: { color: '#d4843e', width: 3 },
      label: {
        show: true,
        position: 'top',
        formatter: (p) => {
          const val = isTemp ? Number(p.value).toFixed(1) : Math.round(p.value)
          return `${val}${unit}`
        },
        color: '#805229',
        fontSize: 12
      }
    }]
  })
}

// 专属推荐 · 推荐食谱饼图（环形）。颜色沿用旧食谱条的紫/绿/橙/金，保持视觉延续。
const DIET_COLORS = ['#9b6fd6', '#4caf50', '#f0ad4e', '#c9a35a']
const DIET_KEYS = ['pellet', 'veg', 'fruit', 'seed']
function initDietChart(dietRate) {
  if (!dietChartRef.value || !dietRate) return
  if (dietChartInstance) dietChartInstance.dispose()
  dietChartInstance = echarts.init(dietChartRef.value)
  const isDark = systemPrefs.value.theme === 'dark'
  const data = DIET_KEYS
    .filter((k) => dietRate[k] != null && dietRate[k] > 0)
    .map((k, i) => ({
      name: dietLabel(k),
      value: dietRate[k],
      itemStyle: { color: DIET_COLORS[i] },
    }))
  dietChartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c}%' },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: isDark ? '#e8dcc8' : '#5c4636', fontSize: 12 },
    },
    series: [{
      type: 'pie',
      radius: ['38%', '64%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: isDark ? '#2d2837' : '#fff', borderWidth: 2 },
      label: {
        show: true,
        formatter: '{d}%',
        color: isDark ? '#e8dcc8' : '#4c3b31',
        fontSize: 12,
        fontWeight: 700,
      },
      labelLine: { show: true },
      data,
    }],
  })
}

function openDustGauge(snapshot) {
  openMetricGauge({
    metric: snapshot.metric || 'dust',
    label: snapshot.label || labelText('dust'),
    value: snapshot.value ?? snapshot.dustValue,
    displayValue: snapshot.displayValue || `${snapshot.dustValue}${snapshot.dustUnit || ''}`,
    unit: snapshot.unit || snapshot.dustUnit || 'ppm',
    level: snapshot.level || snapshot.dustLevel,
    gaugeMax: snapshot.gaugeMax || 120,
    connected: snapshot.connected,
  })
}

function openDustDetail(snapshot) {
  openDustGauge(snapshot)
}

function handleMetricUpdate(metrics) {
  if (modal.value?.type !== 'metric-gauge') return
  const currentMetric = modal.value.item?.metric
  const nextMetric = metrics.find((item) => item.metric === currentMetric)
  if (!nextMetric) return
  modal.value = {
    ...modal.value,
    item: {
      ...modal.value.item,
      ...nextMetric,
    },
  }
}

function handleAlarmNotice(payload) {
  showAlarmToast(payload?.message)
}

function isMetricCurve(curve) {
  return Boolean(metricCurveKind(curve))
}

function isReportGaugeCurve(curve) {
  return false
}

function metricCurveKind(curve) {
  const text = `${curve.label || ''}${curve.axis || ''}${curve.unit || ''}`
  if (text.includes('温') || text.includes('娓') || text.includes('℃') || text.includes('掳C')) return 'temperature'
  if (text.includes('湿') || text.includes('婀') || curve.unit === '%') return 'humidity'
  if (text.includes('粉') || text.includes('尘') || text.includes('绮') || text.includes('μg') || text.includes('ppm') || text.includes('渭g') || text.includes('/m')) return 'dust'
  return ''
}

function curveToMetric(curve) {
  const latest = curve.points?.[curve.points.length - 1] ?? Number.parseFloat(curve.value)
  const kind = metricCurveKind(curve)
  if (kind === 'temperature') {
    return {
      metric: 'temperature',
      label: labelText('temperature'),
      value: latest,
      displayValue: `${latest}${curve.unit || '℃'}`,
      unit: curve.unit || '℃',
      level: latest < 18 ? labelText('lowState') : latest > 30 ? labelText('highState') : labelText('suitable'),
      gaugeMax: 45,
      connected: false,
    }
  }
  if (kind === 'humidity') {
    return {
      metric: 'humidity',
      label: labelText('humidity'),
      value: latest,
      displayValue: `${latest}${curve.unit || '%'}`,
      unit: curve.unit || '%',
      level: latest < 40 ? labelText('lowState') : latest > 70 ? labelText('highState') : labelText('suitable'),
      gaugeMax: 100,
      connected: false,
    }
  }
  return {
    metric: 'dust',
    label: labelText('dust'),
    value: latest,
    displayValue: `${latest}${curve.unit || 'ppm'}`,
    unit: curve.unit || 'ppm',
    level: latest >= 80 ? labelText('high') : latest >= 35 ? labelText('mid') : labelText('low'),
    gaugeMax: 120,
    connected: false,
  }
}

function openMetricGauge(item) {
  openModal('metric-gauge', metricGaugeTitle(item), item)
}

function metricGaugeRatio(item) {
  const number = Number(item?.value ?? item?.dustValue)
  const max = Number(item?.gaugeMax || 100)
  if (!Number.isFinite(number) || max <= 0) return 0
  return Math.min(1, Math.max(0, number / max))
}

function metricNeedleRotation(item) {
  return `${-90 + metricGaugeRatio(item) * 180}deg`
}

function metricGaugeLevel(item) {
  if (item?.level) return item.level
  return dustGaugeLevel(item?.value, item?.dustLevel)
}

function formatStamp(date = new Date()) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatShotTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return formatStamp()
  return formatStamp(date)
}

function sanitizeWeight(value) {
  const cleaned = String(value || '').replace(/[^\d.]/g, '')
  const [integer, ...decimal] = cleaned.split('.')
  return decimal.length ? `${integer}.${decimal.join('').slice(0, 1)}` : integer
}

function parseWeight(value) {
  const number = Number.parseFloat(String(value || '').replace(/[^\d.]/g, ''))
  return Number.isFinite(number) ? number : ''
}

function normalizedWeightBars(history = []) {
  const values = history.map((item) => Number(item.value)).filter(Number.isFinite)
  if (!values.length) return [28]
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  return values.map((value) => Math.round(28 + ((value - min) / range) * 42))
}

async function handleSnapshotCaptured(snapshot) {
  const title = `${ui.value.snapshotPhoto} ${formatShotTime(snapshot.savedAt).slice(5)}`
  if (careApiReady.value) {
    if (!selectedParrot.value.id) {
      showBackendError(new Error('请先新增鹦鹉档案，再保存相片记录。'))
      return
    }
    try {
      const saved = await createPhoto(selectedParrot.value.id, {
        mediaType: 'screenshot',
        title,
        imageBase64: snapshot.image,
        thumbnailUrl: null,
        tags: '监控,截图',
      })
      basePhotoRecords.value = [mapPhotoFromApi(saved), ...basePhotoRecords.value]
      const profile = profiles.value.find((item) => item.id === selectedParrot.value.id)
      if (profile) profile.photos = `${basePhotoRecords.value.length} 张`
    } catch (error) {
      showBackendError(error)
    }
    return
  }

  capturedPhotos.value = [
    {
      ...snapshot,
      parrotId: selectedParrot.value.id,
      title,
      time: formatShotTime(snapshot.savedAt),
    },
    ...capturedPhotos.value,
  ].slice(0, 24)
  const profile = profiles.value.find((item) => item.id === selectedParrot.value.id)
  if (profile) profile.photos = `${archivePhotoRecords.value.length} 张`
}

onMounted(() => {
  try {
    const snapshots = JSON.parse(localStorage.getItem('parrotArchiveSnapshots') || '[]')
    capturedPhotos.value = snapshots.map((snapshot) => ({
      ...snapshot,
      title: `${ui.value.snapshotPhoto} ${formatShotTime(snapshot.savedAt).slice(5)}`,
      time: formatShotTime(snapshot.savedAt),
    }))
  } catch {
    capturedPhotos.value = []
  }
  window.addEventListener('growth-report-ready', handleGrowthReportReady)
  window.setTimeout(() => {
    if (notificationBadges.value.growth) showGrowthReportToast()
  }, 600)
  if (isAuthenticated.value) {
    loadUserPreferences()
    loadUserProfile()
    loadCareBootstrap()
    fetchUserProfile()
      .then((user) => syncAccountFromUser(user))
      .catch((error) => console.warn('刷新用户资料失败：', error?.message))
  }
})

onBeforeUnmount(() => {
  destroyAMap()
  window.clearTimeout(searchTimer)
  window.clearTimeout(reportToastTimer)
  window.clearTimeout(alarmToastTimer)
  window.removeEventListener('growth-report-ready', handleGrowthReportReady)
  stopDashboardPolling()
  stopCareEnvPolling()
  if (dietChartInstance) { dietChartInstance.dispose(); dietChartInstance = null }
})

function openArchiveProfile(profile) {
  activeArchiveId.value = profile.id
  weightDraft.value = String(parseWeight(profile.weight) || '')
  openThird(`archive:${profile.id}`)
  // 从档案列表直接进入时，按当前档案重新加载体重、病历、记账和照片，避免复用上一只宠物的缓存。
  void loadPetResources(profile.id)
}

function openWeightChart() {
  openModal('weight-chart', labelText('weightChart'), selectedArchive.value)
}

async function saveArchiveWeight() {
  const number = Number(sanitizeWeight(weightDraft.value))
  if (!Number.isFinite(number) || number <= 0) return
  const archive = selectedArchive.value
  if (!archive) return

  if (careApiReady.value) {
    if (!archive.id) {
      showBackendError(new Error('请先新增鹦鹉档案，再保存体重。'))
      return
    }
    try {
      const todayRecord = (archive.weightHistory || []).find((item) => (
        item.id && String(item.measuredAt || '').slice(0, 10) === todayText.value
      ))
      const body = {
        weightGrams: number,
        source: 'manual',
        remark: '',
      }
      if (todayRecord) await updateWeightApi(archive.id, todayRecord.id, body)
      else await createWeightApi(archive.id, body)
      const weights = await listWeights(archive.id)
      applyWeightsToSelectedProfile(Array.isArray(weights) ? weights : [], archive.id)
      weightDraft.value = String(number)
      openModal('archive', labelText('weightSaved'), { name: archive.name, note: archive.lastWeight })
    } catch (error) {
      showBackendError(error)
    }
    return
  }

  const dateText = todayText.value
  const shortDate = dateText.slice(5)
  const entry = { time: shortDate, value: number }
  const history = Array.isArray(archive.weightHistory) ? archive.weightHistory : []
  const existingIndex = history.findIndex((item) => item.time === shortDate)
  if (existingIndex >= 0) history.splice(existingIndex, 1, entry)
  else history.push(entry)

  archive.weightHistory = history.slice(-12)
  archive.weight = `${number}g`
  archive.lastWeight = `${dateText} ${labelText('recordWeight')} ${number}g`

  const parrot = localParrots.value.find((item) => item.id === archive.id)
  if (parrot) parrot.weight = archive.weight
  if (selectedParrot.value.id === archive.id) {
    selectedParrot.value = { ...selectedParrot.value, weight: archive.weight }
  }
  weightDraft.value = String(number)
  openModal('archive', labelText('weightSaved'), { name: archive.name, note: archive.lastWeight })
}

function weightChartScale(history = []) {
  const values = history.map((item) => Number(item.value)).filter(Number.isFinite)
  if (!values.length) return { min: 0, max: 1 }
  const rawMin = Math.min(...values)
  const rawMax = Math.max(...values)
  // 上下各留出余量，既保证所有点不贴边，也让纵轴刻度易读。
  const padding = Math.max((rawMax - rawMin) * 0.12, Math.abs(rawMax) * 0.03, 1)
  return {
    min: Math.max(0, Math.floor((rawMin - padding) * 10) / 10),
    max: Math.ceil((rawMax + padding) * 10) / 10,
  }
}

function weightChartTicks(history = []) {
  const { min, max } = weightChartScale(history)
  const range = max - min || 1
  return [36, 85, 134, 183, 232].map((y, index) => ({
    y,
    value: max - (range * index) / 4,
  }))
}

function formatWeightValue(value) {
  const number = Number(value)
  return Number.isFinite(number) ? String(Number(number.toFixed(1))) : '-'
}

function weightHistoryPoints(history = [], width = 520, height = 220) {
  const { min, max } = weightChartScale(history)
  return linePoints(history.map((item) => item.value), width, height, min, max)
}

function translatedWeightPoints(history = []) {
  // 为纵轴数字留出左侧空间，曲线绘制区域固定在 x=72 至 x=536。
  return weightHistoryPoints(history, 464, 212)
    .split(' ')
    .map((pair) => {
      const [x, y] = pair.split(',').map(Number)
      return `${x + 72},${y + 28}`
    })
    .join(' ')
}

function weightPointPosition(history = [], index, axis) {
  const pair = translatedWeightPoints(history).split(' ')[index] || '72,240'
  const [x, y] = pair.split(',').map(Number)
  return axis === 'x' ? x : y
}

function weightChartPointsList(history = [], width = 500, height = 100) {
  const pointsStr = weightHistoryPoints(history, width, height)
  if (!pointsStr) return []
  return pointsStr.split(' ').map((pair) => {
    const [x, y] = pair.split(',').map(Number)
    return { x: x + 10, y: y + 10 }
  })
}

function weightCurvePath(history = []) {
  const list = weightChartPointsList(history)
  if (list.length === 0) return ''
  return 'M ' + list.map((p) => `${p.x},${p.y}`).join(' L ')
}

function weightAreaPath(history = []) {
  const list = weightChartPointsList(history)
  if (list.length === 0) return ''
  const linePath = list.map((p) => `${p.x},${p.y}`).join(' L ')
  const first = list[0]
  const last = list[list.length - 1]
  return `M ${first.x},115 L ${linePath} L ${last.x},115 Z`
}

function dustGaugeRatio(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return 0
  return Math.min(1, Math.max(0, number / 120))
}

function dustNeedleRotation(value) {
  return `${-90 + dustGaugeRatio(value) * 180}deg`
}

function dustGaugeLevel(value, fallback) {
  if (fallback) return fallback
  const number = Number(value)
  if (number >= 80) return '高'
  if (number >= 35) return '中'
  return '低'
}

function linePoints(points, width = 260, height = 92, yMin, yMax) {
  // 真实数据可能含 null（无采样桶），跳过这些点并按原始下标保留 x 间距，避免折线拉回零点。
  const valid = points.map((v, i) => ({ v: Number(v), i })).filter((p) => Number.isFinite(p.v))
  if (!valid.length) return ''
  const min = yMin !== undefined ? yMin : Math.min(...valid.map((p) => p.v))
  const max = yMax !== undefined ? yMax : Math.max(...valid.map((p) => p.v))
  const range = max - min || 1
  const n = points.length
  return valid.map((p) => {
    const x = n === 1 ? width / 2 : (p.i / (n - 1)) * width
    const y = height - ((p.v - min) / range) * (height - 16) - 8
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

function linePointCoordinate(points, index, width = 260, height = 92, yMin, yMax) {
  // 与 linePoints 一致，按原始下标定位（跳过 null 桶），这样 tooltip 圆点落在真实采样位置上。
  const valid = points.map((v, i) => ({ v: Number(v), i })).filter((p) => Number.isFinite(p.v))
  if (!valid.length) return '0,0'
  if (valid.length === 1) {
    return index === valid[0].i ? `${(width / 2).toFixed(1)},${(height / 2).toFixed(1)}` : '0,0'
  }
  const min = yMin !== undefined ? yMin : Math.min(...valid.map((p) => p.v))
  const max = yMax !== undefined ? yMax : Math.max(...valid.map((p) => p.v))
  const range = max - min || 1
  const n = points.length
  const target = valid.find((p) => p.i === index)
  if (!target) return '0,0'
  const x = (target.i / (n - 1)) * width
  const y = height - ((target.v - min) / range) * (height - 16) - 8
  return `${x.toFixed(1)},${y.toFixed(1)}`
}

async function addMedicalRecord() {
  const content = String(newMedicalDraft.value.content || '').trim()
  if (!content) return
  const body = medicalRecordDraftToRequest(newMedicalDraft.value)
  if (careApiReady.value) {
    if (!selectedParrot.value.id) {
      showBackendError(new Error('请先新增鹦鹉档案，再新增病历。'))
      return
    }
    try {
      const saved = await createMedicalRecordApi(selectedParrot.value.id, body)
      medicalRecords.value.unshift(mapMedicalRecordFromApi(saved))
      newMedicalDraft.value = emptyMedicalDraft()
      medFormMoreOpen.value = false
    } catch (error) {
      showBackendError(error)
    }
    return
  }
  medicalRecords.value.unshift({
    id: `m-${Date.now()}`,
    recordId: null,
    recordDate: body.recordDate,
    recordType: body.recordType,
    title: null,
    content: body.content,
    hospitalName: body.hospitalName,
    hospitalPhone: body.hospitalPhone,
    attachments: [],
  })
  newMedicalDraft.value = emptyMedicalDraft()
  medFormMoreOpen.value = false
}

function startEditMedical(record) {
  editingMedicalId.value = record.id
  const dateRaw = record.recordDate || (record.text || '').match(/^\d{4}-\d{2}-\d{2}/)?.[0] || todayText.value
  editingMedicalDraft.value = {
    recordDate: dateRaw,
    recordType: record.recordType || 'symptom',
    content: record.content || medRecordText(record),
    hospitalName: record.hospitalName || '',
    hospitalPhone: record.hospitalPhone || '',
  }
}

function cancelEditMedical() {
  editingMedicalId.value = ''
  editingMedicalDraft.value = emptyMedicalDraft()
}

async function saveMedicalRecord(record) {
  const content = String(editingMedicalDraft.value.content || '').trim()
  if (!content) return
  const body = medicalRecordDraftToRequest(editingMedicalDraft.value)
  if (careApiReady.value && !record.recordId) {
    showBackendError(new Error('该病历不是后端记录，无法同步修改数据库。'))
    return
  }
  if (careApiReady.value && record.recordId) {
    try {
      const saved = await updateMedicalRecordApi(selectedParrot.value.id, record.recordId, body)
      Object.assign(record, mapMedicalRecordFromApi(saved))
      editingMedicalId.value = ''
      editingMedicalDraft.value = emptyMedicalDraft()
    } catch (error) {
      showBackendError(error)
    }
    return
  }
  Object.assign(record, {
    recordDate: body.recordDate,
    recordType: body.recordType,
    title: null,
    content: body.content,
    hospitalName: body.hospitalName,
    hospitalPhone: body.hospitalPhone,
  })
  editingMedicalId.value = ''
  editingMedicalDraft.value = emptyMedicalDraft()
}

async function deleteMedicalRecord(record) {
  if (!window.confirm('确定要删除该病历记录吗？')) return
  if (careApiReady.value && record.recordId) {
    try {
      await deleteMedicalRecordApi(selectedParrot.value.id, record.recordId)
      medicalRecords.value = medicalRecords.value.filter(r => r.recordId !== record.recordId)
    } catch (error) {
      showBackendError(error)
    }
  } else {
    medicalRecords.value = medicalRecords.value.filter(r => r.id !== record.id)
  }
}

async function addLedgerRecord() {
  const description = ledgerDraft.value.description.trim()
  const amount = Number(ledgerDraft.value.amount)
  ledgerFormError.value = ''
  if (!description) {
    ledgerFormError.value = '请填写支出说明。'
    return
  }
  if (!Number.isFinite(amount) || amount <= 0) {
    ledgerFormError.value = '金额必须大于 0。'
    return
  }
  const body = {
    expenseDate: ledgerDraft.value.time || todayText.value,
    category: ledgerDraft.value.tag || '其他',
    description: `${selectedParrot.value.shortName} · ${description}`,
    amount,
    currency: 'CNY',
  }
  if (careApiReady.value) {
    if (!selectedParrot.value.id) {
      showBackendError(new Error('请先新增鹦鹉档案，再新增账本记录。'))
      return
    }
    ledgerSaving.value = true
    try {
      const saved = await createLedgerRecordApi(selectedParrot.value.id, body)
      ledgerRecords.value.unshift(mapLedgerRecordFromApi(saved))
      ledgerDraft.value = {
        time: todayText.value,
        tag: '食物',
        description: '',
        amount: '',
      }
      closeModal()
      ledgerFeedback.value = '支出记录已保存'
      window.setTimeout(() => { ledgerFeedback.value = '' }, 2600)
    } catch (error) {
      showBackendError(error)
    } finally {
      ledgerSaving.value = false
    }
    return
  }
  ledgerRecords.value.unshift({
    id: `l-${Date.now()}`,
    time: body.expenseDate,
    createdAt: formatStamp(),
    updatedAt: '',
    tag: body.category,
    description: body.description,
    amount,
    system: false,
    tagSystem: false,
  })
  ledgerDraft.value = {
    time: todayText.value,
    tag: '食物',
    description: '',
    amount: '',
  }
  closeModal()
  ledgerFeedback.value = '支出记录已保存'
  window.setTimeout(() => { ledgerFeedback.value = '' }, 2600)
}

function startEditLedger(record) {
  editingLedgerId.value = record.id
  editingLedgerDraft.value = { ...record }
}

function confirmDeleteLedger(record) {
  if (!record) return
  openModal('confirm-delete-ledger', '确认删除账本记录', record)
}

async function executeDeleteLedger() {
  const record = modal.value?.item
  if (!record || ledgerDeleting.value) return
  ledgerDeleting.value = true
  try {
    if (careApiReady.value) {
      if (!record.ledgerId) throw new Error('该记录尚未同步到数据库，无法执行删除。')
      await deleteLedgerRecordApi(selectedParrot.value.id, record.ledgerId)
    }
    ledgerRecords.value = ledgerRecords.value.filter((item) => item.id !== record.id)
    closeModal()
    ledgerFeedback.value = '账本记录已删除'
    window.setTimeout(() => { ledgerFeedback.value = '' }, 2600)
  } catch (error) {
    closeModal()
    showBackendError(error)
  } finally {
    ledgerDeleting.value = false
  }
}

async function saveLedgerRecord(record) {
  if (!editingLedgerDraft.value) return
  const description = editingLedgerDraft.value.description.trim()
  const amount = Number(editingLedgerDraft.value.amount)
  if (!description || !Number.isFinite(amount) || amount <= 0) return
  const body = {
    expenseDate: editingLedgerDraft.value.time || todayText.value,
    category: editingLedgerDraft.value.tag || '其他',
    description,
    amount,
    currency: editingLedgerDraft.value.currency || 'CNY',
  }
  if (careApiReady.value && !record.ledgerId) {
    showBackendError(new Error('该账本记录不是后端记录，无法同步修改数据库。'))
    return
  }
  if (careApiReady.value && record.ledgerId) {
    try {
      const saved = await updateLedgerRecordApi(selectedParrot.value.id, record.ledgerId, body)
      Object.assign(record, mapLedgerRecordFromApi(saved))
      editingLedgerId.value = ''
      editingLedgerDraft.value = null
    } catch (error) {
      showBackendError(error)
    }
    return
  }
  Object.assign(record, {
    time: body.expenseDate,
    updatedAt: formatStamp(),
    tag: body.category,
    description: body.description,
    amount,
    system: false,
    tagSystem: false,
  })
  editingLedgerId.value = ''
  editingLedgerDraft.value = null
}

async function ensureDeviceOptions() {
  try {
    const data = await listDevices()
    const list = (Array.isArray(data) ? data : data?.list || [])
    availableDevices.value = list
  } catch (error) {
    console.warn('加载设备列表失败：', error?.message)
  }
}

function openCreateProfile() {
  profileEditId.value = ''
  profileForm.value = {
    species: '小太阳',
    name: '',
    birthday: '2024-05-18',
    weight: '',
    sex: '未知',
    currentStatus: '站立',
    deviceId: 'device-001',
  }
  ensureDeviceOptions()
  openModal('archive-create', labelText('addProfile'))
}

function confirmDeleteProfile(profile) {
  if (!profile) return
  openModal('confirm-delete-profile', text.value.deleteProfileTitle, {
    name: profile.name,
    warning: text.value.deleteProfileWarning,
  })
}

async function executeDeleteProfile() {
  const petId = selectedArchive.value?.petId
  if (!petId) {
    closeModal()
    return
  }
  try {
    await deleteParrotApi(petId)
    closeModal()
    const remaining = profiles.value.filter((item) => item.id !== selectedArchive.value.id)
    profiles.value = remaining
    localParrots.value = localParrots.value.filter((item) => item.id !== selectedArchive.value.id)
    if (remaining.length) {
      selectedParrot.value = remaining[0]
      activeArchiveId.value = remaining[0].id
      thirdView.value = ''
    } else {
      selectedParrot.value = { ...EMPTY_REMOTE_PARROT }
      activeArchiveId.value = ''
      thirdView.value = ''
    }
  } catch (error) {
    closeModal()
    openModal('risk', text.value.deleteProfileTitle, {
      value: error?.message || '删除失败，请稍后重试',
    })
  }
}

function openEditProfile(profile) {
  if (!profile) return
  profileEditId.value = profile.id
  profileForm.value = {
    species: profile.species || '小太阳',
    name: profile.name || '',
    birthday: profile.birthday || '',
    weight: '',
    sex: profile.sex || '未知',
    currentStatus: statusFromApi(profile.apiRaw?.currentStatus) || profile.currentStatus || String(profile.status || '').replace(/^当前状态/, '') || '站立',
    deviceId: profile.deviceId || profile.apiRaw?.deviceId || '',
  }
  ensureDeviceOptions()
  openModal('archive-edit', labelText('editProfile'), profile)
}

function profileFormToRequest({ includeInitialWeight = false } = {}) {
  const body = {
    name: profileForm.value.name.trim(),
    species: speciesToApi(profileForm.value.species),
    birthday: profileForm.value.birthday || null,
    sex: sexToApi(profileForm.value.sex),
    deviceId: profileForm.value.deviceId || null,
    currentStatus: statusToApi(profileForm.value.currentStatus),
  }
  if (includeInitialWeight) body.initialWeightGrams = parseWeight(profileForm.value.weight) || undefined
  return body
}

function applySavedProfile(saved) {
  const parrot = mapProfileFromApi(saved)
  const previousProfile = profiles.value.find((item) => item.id === parrot.id)
  const profile = {
    ...mapArchiveProfileFromApi(saved),
    photos: previousProfile?.photos || '0 张',
    lastWeight: previousProfile?.lastWeight || mapArchiveProfileFromApi(saved).lastWeight,
    weightHistory: previousProfile?.weightHistory || mapArchiveProfileFromApi(saved).weightHistory,
  }
  const parrotIndex = localParrots.value.findIndex((item) => item.id === parrot.id)
  if (parrotIndex >= 0) localParrots.value.splice(parrotIndex, 1, parrot)
  else localParrots.value.push(parrot)

  const profileIndex = profiles.value.findIndex((item) => item.id === profile.id)
  if (profileIndex >= 0) profiles.value.splice(profileIndex, 1, profile)
  else profiles.value.push(profile)

  if (!selectedParrot.value.id || selectedParrot.value.id === parrot.id) selectedParrot.value = parrot
  activeArchiveId.value = parrot.id
  return { parrot, profile }
}

async function saveNewProfile() {
  const name = profileForm.value.name.trim() || `新鹦鹉${localParrots.value.length + 1}`
  const weight = profileForm.value.weight.trim() || '未录入'
  const ageStage = getAgeStage(profileForm.value.birthday)

  if (careApiReady.value) {
    try {
      const saved = await createParrot({ ...profileFormToRequest({ includeInitialWeight: true }), name })
      const { parrot } = applySavedProfile(saved)
      selectParrot(parrot)
      closeModal()
    } catch (error) {
      showBackendError(error)
    }
    return
  }

  const id = `parrot-${Date.now()}`
  const parrot = {
    id,
    name,
    shortName: name,
    avatarType: 'avatar-orange',
    species: profileForm.value.species,
    birthday: profileForm.value.birthday,
    weight,
    sex: profileForm.value.sex,
    status: '站立',
    ageStage,
    route: '/archive',
  }
  localParrots.value.push(parrot)
  profiles.value.push({
    ...parrot,
    status: '当前状态站立',
    device: '未绑定设备',
    photos: '0 张',
    lastWeight: `2026-07-03 录入 ${weight}`,
    weightHistory: [{ time: '今日', value: Number.parseFloat(weight) || 0 }],
  })
  selectedParrot.value = parrot
  closeModal()
}

async function saveProfileEdit() {
  if (!profileEditId.value) return
  if (careApiReady.value) {
    try {
      const saved = await updateParrot(profileEditId.value, profileFormToRequest())
      applySavedProfile(saved)
      closeModal()
    } catch (error) {
      showBackendError(error)
    }
    return
  }

  const profile = profiles.value.find((item) => item.id === profileEditId.value)
  const parrot = localParrots.value.find((item) => item.id === profileEditId.value)
  if (!profile || !parrot) return
  const patch = {
    name: profileForm.value.name.trim() || profile.name,
    shortName: profileForm.value.name.trim() || profile.shortName,
    species: profileForm.value.species,
    birthday: profileForm.value.birthday,
    sex: profileForm.value.sex,
    status: profileForm.value.currentStatus,
    ageStage: getAgeStage(profileForm.value.birthday),
    deviceId: profileForm.value.deviceId,
  }
  Object.assign(parrot, patch)
  Object.assign(profile, { ...patch, status: `当前状态${patch.status}`, device: patch.deviceId || profile.device })
  if (selectedParrot.value.id === parrot.id) selectedParrot.value = { ...selectedParrot.value, ...patch }
  closeModal()
}

async function toggleSettingsEdit() {
  if (isSettingsEditing.value) {
    const avatarParrotId = settingsDraft.value.avatarParrotId || ''
    const avatarImage = settingsDraft.value.avatarImage || ''
    const body = {
      username: String(settingsDraft.value.username || '').trim(),
      phone: settingsDraft.value.phoneBound ? String(settingsDraft.value.phone || '').trim() : null,
      email: settingsDraft.value.emailBound ? String(settingsDraft.value.email || '').trim() : null,
      location: String(settingsDraft.value.location || '').trim() || null,
      avatarImage: settingsDraft.value.avatarImage || null,
    }
    try {
      const saved = await apiUpdateUserProfile(body)
      syncAccountFromUser(saved)
      account.value = { ...account.value, avatarImage }
      settingsDraft.value = { ...settingsDraft.value, avatarImage }
      if (avatarImage) {
        localStorage.setItem('parrotUserAvatar', avatarImage)
      } else {
        localStorage.removeItem('parrotUserAvatar')
      }
      await savePreferencePatch({ avatarParrotId })
      isSettingsEditing.value = false
        // phoneChanging.value = false
        // emailChanging.value = false
    } catch (error) {
      // 保存失败时保留草稿和编辑态，避免用户重新填写。
      showBackendError(error)
    }
    return
  }
  settingsDraft.value = { ...account.value }
  isSettingsEditing.value = true
}

function sanitizeDigits(value) {
  return String(value || '').replace(/\D/g, '').slice(0, 11)
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

async function onUserAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  // 读文件为 data URL，打开裁剪弹窗而不是直接保存
  const dataUrl = await fileToDataUrl(file)
  pendingAvatarSrc.value = dataUrl
  showAvatarCropDialog.value = true
  event.target.value = ''
}

async function confirmAvatarCrop() {
  const cropper = avatarCropperRef.value
  if (!cropper) { showAvatarCropDialog.value = false; return }
  // getResult() 同步返回 { canvas, coordinates, image }；需 canvas=true
  let result = null
  try { result = cropper.getResult() } catch { result = null }
  const canvas = result?.canvas
  if (!canvas) { showAvatarCropDialog.value = false; return }
  // 压缩为 JPEG，和截图的 compressCanvasToJpeg 同样模式
  const maxSize = 256
  const scale = Math.min(1, maxSize / Math.max(canvas.width, canvas.height))
  const target = document.createElement('canvas')
  target.width = Math.max(1, Math.round(canvas.width * scale))
  target.height = Math.max(1, Math.round(canvas.height * scale))
  target.getContext('2d').drawImage(canvas, 0, 0, target.width, target.height)
  const dataUrl = target.toDataURL('image/jpeg', 0.6)
  settingsDraft.value.avatarImage = dataUrl
  showAvatarCropDialog.value = false
  pendingAvatarSrc.value = ''
}

function cancelAvatarCrop() {
  showAvatarCropDialog.value = false
  pendingAvatarSrc.value = ''
}

function openSettingsInfo(type) {
  const pack = {
    zh: {
      about: ['鹦鹉智能看护系统面向小型家养鹦鹉，围绕粉尘浓度、温度、湿度、视频看护、成长报告、宠物档案和饲养记录，帮助主人更及时地了解鹦鹉生活状态。', '项目由原智慧烟感系统改编，重点把烟雾检测能力转化为鹦鹉笼羽粉/粉尘风险监测。'],
      system: ['前端：Vue 3 + Vite + 原生 CSS。', '后端：Spring Boot + JPA，提供烟雾/粉尘实时数据、历史数据、告警和系统设置接口。', '当前粉尘浓度已接入 /api/smoke/realtime；温度、湿度字段已预留。'],
      version: ['ParrotCare Desktop Preview v0.8.7', '构建日期：2026-07-05'],
    },
    en: {
      about: ['ParrotCare is designed for small pet parrots. It tracks dust, temperature, humidity, video care, reports, profiles and expenses to help owners understand daily conditions.', 'The project adapts the previous smart smoke system into a cage dust and care-monitoring experience.'],
      system: ['Frontend: Vue 3, Vite and native CSS.', 'Backend: Spring Boot and JPA, providing realtime dust, history, alarms and settings APIs.', 'Dust is connected through /api/smoke/realtime; temperature and humidity fields are reserved.'],
      version: ['ParrotCare Desktop Preview v0.8.7', 'Build date: 2026-07-05'],
    },
    es: {
      about: ['ParrotCare está diseñado para loros domésticos pequeños. Supervisa polvo, temperatura, humedad, video, informes, perfiles y gastos.', 'El proyecto adapta el sistema de humo inteligente a un sistema de cuidado y polvo de jaula.'],
      system: ['Frontend: Vue 3, Vite y CSS nativo.', 'Backend: Spring Boot y JPA, con APIs de polvo en tiempo real, historial, alarmas y ajustes.', 'El polvo usa /api/smoke/realtime; temperatura y humedad están reservadas.'],
      version: ['ParrotCare Desktop Preview v0.8.7', 'Fecha de compilación: 2026-07-05'],
    },
    ja: {
      about: ['ParrotCare は小型の家庭用インコ向けの見守りシステムです。粉じん、温度、湿度、映像、レポート、プロフィール、支出を管理します。', '以前のスマート煙感知システムを、ケージ粉じんと飼育ケア向けに改編しました。'],
      system: ['フロントエンド：Vue 3、Vite、ネイティブ CSS。', 'バックエンド：Spring Boot と JPA。リアルタイム粉じん、履歴、警報、設定 API を提供します。', '粉じんは /api/smoke/realtime に接続済み。温度と湿度は予約フィールドです。'],
      version: ['ParrotCare Desktop Preview v0.8.7', 'ビルド日：2026-07-05'],
    },
  }
  const lines = (pack[systemPrefs.value.language] || pack.zh)[type]
  const title = type === 'about' ? text.value.about : type === 'system' ? text.value.system : text.value.version
  const info = { title, lines }
  openModal('settings-info', info.title, info)
}
</script>

<template>
  <LoginView
    v-if="!isAuthenticated"
    @login-success="handleLoginSuccess"
  />

  <main
    v-else
    class="app-shell"
    :class="[themeClass, languageClass]"
    :style="{ '--user-font-size': `${systemPrefs.fontSize}px` }"
  >
    <transition name="report-toast">
      <div v-if="reportToastVisible" class="growth-report-toast" role="status">
        {{ ui.reportToast }}
      </div>
    </transition>
    <transition name="alarm-toast">
      <div v-if="alarmToast" class="alarm-top-toast" role="alert">
        {{ alarmToast }}
      </div>
    </transition>

    <section v-if="!activeView" class="dashboard" aria-label="基于智慧烟感的宠物安全系统首页">
      <div class="column left-column">
        <EntryCard :card="localizedEntryCards.archive" size="archive" @open="handleOpen" />
        <EntryCard :card="localizedEntryCards.growth" size="growth" @open="handleOpen" />
      </div>

      <div class="column center-column">
        <div class="current-zone">
          <CurrentBirdCard :parrot="selectedParrot" :avatar-src="petAvatarSource(selectedParrot)" :label="text.currentParrot" @open="togglePetSwitch" />
          <section v-if="petSwitchOpen" class="pet-switch-panel" aria-label="宠物切换面板">
            <button
              v-for="parrot in localParrots"
              :key="parrot.id"
              class="pet-option"
              :class="{ active: selectedParrot.id === parrot.id }"
              type="button"
              @click="selectParrot(parrot)"
            >
              <span class="pet-mini-avatar">
                <img v-if="petAvatarSource(parrot)" class="pet-avatar-photo" :src="petAvatarSource(parrot)" :alt="parrot.name" />
                <ParrotVisual v-else :type="parrot.avatarType" />
              </span>
              <span>
                <strong>{{ parrot.name }}</strong>
                <em>{{ valueText(parrot.species) }} · {{ parrot.weight }} · {{ valueText(parrot.status) }}</em>
              </span>
            </button>
          </section>
        </div>
<MonitorCard
  :card="localizedPrimaryCards.monitor"
  :device-id="selectedParrot.deviceId"
  :parrot-id="selectedParrot.id"
  :locale="systemPrefs.language"
  @open="handleOpen"
  @dust-detail="openDustDetail"
  @metric-update="handleMetricUpdate"
  @alarm-notify="handleAlarmNotice"
  @snapshot-captured="handleSnapshotCaptured"
  @fullscreen-change="handleMonitorFullscreenChange"
/>
        <EntryCard :card="localizedEntryCards.ledger" size="ledger" @open="handleOpen" />
      </div>

      <div class="column right-column">
        <EntryCard :card="localizedEntryCards.settings" size="settings" @open="handleOpen" />
        <EntryCard :card="localizedEntryCards.medical" size="medical" @open="handleOpen" />
        <EntryCard :card="localizedEntryCards.handbook" size="handbook" @open="handleOpen" />
      </div>

      <span class="route-probe" aria-hidden="true">{{ lastOpenedRoute }}</span>
    </section>

    <section
      v-else
      class="detail-shell clean-detail"
      :class="[`detail-${activeView.theme}`, `detail-kind-${activeView.kind}`]"
      :aria-label="activeView.title + '详情页'"
    >
      <header class="detail-header" :class="{ 'is-care-profile': thirdView === 'care-profile' }">
        <button class="back-button" type="button" aria-label="返回" @click="goBack">
          <span aria-hidden="true"></span>
        </button>
        <div class="detail-title-block">
          <h1>{{ localizedActiveTitle }}</h1>
        </div>
        <!-- 专属推荐：品种切换 pill，顶栏右侧；默认「我养的」排第一，下拉预览其他品种 -->
        <label v-if="thirdView === 'care-profile'" class="care-species-pill">
          <select
            class="care-species-select"
            :value="careActiveSpecies"
            @change="selectCareSpecies($event.target.value)"
          >
            <option v-for="sp in careSpeciesOptions" :key="sp" :value="sp">{{ sp }}</option>
          </select>
        </label>
        <div class="detail-avatar">
          <img v-if="activeView.kind === 'handbook'" class="detail-avatar-img" :src="handbookIcon" alt="饲养手册" />
          <img v-else-if="activeView.kind === 'medical'" class="detail-avatar-img" :src="medicalIcon" alt="医疗助手" />
          <img v-else-if="activeView.kind === 'archive'" class="detail-avatar-img" :src="archiveIcon" alt="宠物档案" />
          <img v-else-if="activeView.kind === 'settings'" class="detail-avatar-img" :src="settingsIcon" alt="用户设置" />
          <img v-else-if="petAvatarSource(selectedParrot)" class="pet-avatar-photo" :src="petAvatarSource(selectedParrot)" :alt="selectedParrot.name" />
          <ParrotVisual v-else :type="selectedParrot.avatarType" />
        </div>
      </header>

      <template v-if="activeView.kind === 'report'">
        <section v-if="!thirdView" class="report-page report-dashboard">
          <div class="report-toolbar clean-report-toolbar dashboard-toolbar">
            <div class="dashboard-title">
              <h2>今日概览</h2>
            </div>

            <div class="history-range-tabs">
              <div
                v-for="range in reportRanges"
                :key="range.value"
                class="history-range-item"
                @mouseenter="ensureHistoryHoverDate(range.value)"
                @mouseleave="historyHoverRange = ''"
              >
                <button
                  type="button"
                  :class="{ active: historyHoverRange === range.value }"
                >
                  {{ range.label }}
                </button>
                <section class="history-date-panel" aria-label="选择历史日期">
                  <div v-if="range.value === '日报' || range.value === '周报'" class="calendar-picker">
                    <header class="calendar-header">
                      <button type="button" @click.stop="changeHistoryCalendarMonth(range.value, -1)">‹</button>
                      <span>{{ historyCalendarMonth[range.value].year }}年{{ monthLabel(historyCalendarMonth[range.value].month) }}</span>
                      <button type="button" @click.stop="changeHistoryCalendarMonth(range.value, 1)">›</button>
                    </header>
                    <div class="calendar-weekdays">
                      <span v-for="wd in ['日','一','二','三','四','五','六']" :key="wd">{{ wd }}</span>
                    </div>
                    <div class="calendar-days">
                      <button
                        v-for="day in calendarDays(historyCalendarMonth[range.value].year, historyCalendarMonth[range.value].month)"
                        :key="formatDate(day)"
                        type="button"
                        class="calendar-day"
                        :class="{
                          'other-month': day.getMonth() !== historyCalendarMonth[range.value].month,
                          'selected': range.value === '日报'
                            ? isSameDay(day, new Date(`${historyHoverDate[range.value]}T00:00:00`))
                            : isBeforeOrSameDay(startOfWeek(day), new Date(`${historyHoverDate[range.value]}T00:00:00`))
                              && isBeforeOrSameDay(new Date(`${historyHoverDate[range.value]}T00:00:00`), endOfWeek(day)),
                          'disabled': day > new Date(`${defaultReportDate(range.value)}T23:59:59`),
                        }"
                        @click.stop="selectHistoryDay(range.value, day)"
                      >
                        {{ day.getDate() }}
                      </button>
                    </div>
                    <div class="history-date-actions">
                      <button type="button" class="primary" @click.stop="confirmHistoryDate(range.value)">查看报告</button>
                    </div>
                  </div>

                  <div v-else class="month-picker">
                    <header class="calendar-header">
                      <button type="button" @click.stop="historyCalendarMonth[range.value].year--">‹</button>
                      <span>{{ historyCalendarMonth[range.value].year }}年</span>
                      <button type="button" @click.stop="historyCalendarMonth[range.value].year++">›</button>
                    </header>
                    <div class="month-grid">
                      <button
                        v-for="m in 12"
                        :key="m"
                        type="button"
                        class="month-cell"
                        :class="{
                          'selected': historyHoverDate[range.value] && new Date(`${historyHoverDate[range.value]}T00:00:00`).getFullYear() === historyCalendarMonth[range.value].year && new Date(`${historyHoverDate[range.value]}T00:00:00`).getMonth() === m - 1,
                          'disabled': endOfMonth(historyCalendarMonth[range.value].year, m - 1) > new Date(`${defaultReportDate(range.value)}T23:59:59`),
                        }"
                        @click.stop="selectHistoryMonth(historyCalendarMonth[range.value].year, m - 1)"
                      >
                        {{ monthLabel(m - 1) }}
                      </button>
                    </div>
                    <div class="history-date-actions">
                      <button type="button" class="primary" @click.stop="confirmHistoryDate(range.value)">查看报告</button>
                    </div>
                  </div>
                </section>
              </div>
            </div>

            <div class="report-parrot-switch">
              <button class="parrot-switch-button" type="button" @click="togglePetSwitch">
                {{ selectedParrot.shortName }}
                <span aria-hidden="true"></span>
              </button>
              <section v-if="petSwitchOpen" class="report-pet-panel" aria-label="报告鹦鹉切换">
                <button v-for="parrot in localParrots" :key="parrot.id" type="button" @click="selectParrot(parrot)">
                  {{ parrot.shortName }} · {{ valueText(parrot.species) }}
                </button>
              </section>
            </div>
          </div>

          <!-- 重新设计的健康评分大卡片 + 其它统计网格 -->
          <!-- 重新设计的健康评分大卡片 + 环境评分大卡片 + 其它统计网格 -->
          <div class="report-stat-grid-fancy">
            <!-- 左侧：健康评分大卡片 -->
            <article class="report-health-score-card">
              <div class="health-card-header">
                <span class="health-card-badge">📊 今日健康报告</span>
                <h2>健康综合评分</h2>
              </div>
              <div class="health-card-body">
                <div class="health-circle-wrapper">
                  <svg class="health-circle-svg" viewBox="0 0 100 100">
                    <circle class="circle-bg" cx="50" cy="50" r="42" />
                    <circle 
                      class="circle-fg" 
                      cx="50" 
                      cy="50" 
                      r="42" 
                      :style="{ 
                        stroke: getScoreColor(dashboardHealthScore),
                        strokeDasharray: `${2 * Math.PI * 42}`, 
                        strokeDashoffset: `${2 * Math.PI * 42 * (1 - dashboardHealthScore / 100)}` 
                      }" 
                    />
                  </svg>
                  <div class="health-score-value">
                    <strong>{{ dashboardHealthScore }}</strong>
                    <span>分</span>
                  </div>
                </div>
                <div class="health-score-text">
                  <p class="health-evaluation">{{ dashboardHealthScore >= 90 ? '棒极了！开心的鹦鹉在跳舞' : dashboardHealthScore >= 80 ? '状态不错，小太阳感到舒适' : '环境不太完美，要多留意哦' }}</p>
                  <p class="health-desc">结合羽粉浓度、温湿度舒适区间以及宠物称重频率自动评估。</p>
                </div>
              </div>
            </article>

            <!-- 中间：环境评分大卡片，数据来自 envMatch.total -->
            <article class="report-health-score-card report-env-score-card">
              <div class="health-card-header">
                <span class="health-card-badge">🏡 专属环境适配</span>
                <h2>环境综合评分</h2>
              </div>
              <div class="health-card-body">
                <div class="health-circle-wrapper">
                  <svg class="health-circle-svg" viewBox="0 0 100 100">
                    <circle class="circle-bg" cx="50" cy="50" r="42" />
                    <circle 
                      class="circle-fg" 
                      cx="50" 
                      cy="50" 
                      r="42" 
                      :style="{ 
                        stroke: getScoreColor(envMatch.total ?? 100),
                        strokeDasharray: `${2 * Math.PI * 42}`, 
                        strokeDashoffset: `${2 * Math.PI * 42 * (1 - (envMatch.total ?? 100) / 100)}` 
                      }" 
                    />
                  </svg>
                  <div class="health-score-value">
                    <strong>{{ envMatch.total ?? '--' }}</strong>
                    <span v-if="envMatch.total != null">分</span>
                  </div>
                </div>
                <div class="health-score-text">
                  <p class="health-evaluation">
                    {{ envMatch.total == null ? '未接入传感器数据' : envMatch.total >= 85 ? '优！环境配置非常理想' : envMatch.total >= 70 ? '良！环境基本适宜' : '警告！请及时调整环境' }}
                  </p>
                  <p class="health-desc">根据当前品种专属饲养方案对温湿度及粉尘浓度综合适配得出。</p>
                </div>
              </div>
            </article>

            <!-- 右侧：小项指标网格 -->
            <div class="report-small-stats-grid">
              <article 
                v-for="stat in dashboardStats.filter(s => s.key !== 'health')" 
                :key="stat.key" 
                class="report-stat-card-fancy"
                :class="[`stat-type-${stat.key}`, { 'stat-empty': stat.value === '-' }]"
              >
                <div class="stat-card-top">
                  <span class="stat-card-icon">
                    <span v-if="stat.key === 'weight'">⚖️</span>
                    <span v-else-if="stat.key === 'calls'">🗣️</span>
                    <span v-else-if="stat.key === 'meals'">🌾</span>
                    <span v-else-if="stat.key === 'droppings'">💩</span>
                    <span v-else>📊</span>
                  </span>
                  <span class="stat-card-label">{{ stat.label }}</span>
                </div>
                <strong class="stat-card-val">{{ stat.value }}</strong>
                <p class="stat-card-tip">{{ stat.tip }}</p>
              </article>
            </div>
          </div>

          <!-- 重新设计的实时环境卡片（含今日趋势折线图） -->
          <section class="report-env-grid-fancy" aria-label="实时环境监控">
            <article
              v-for="env in dashboardRealtimeEnv"
              :key="env.key"
              class="report-env-card-fancy"
              :class="`env-type-${env.key}`"
            >
              <div class="env-card-left">
                <span class="env-label">{{ env.label }}</span>
                <strong class="env-gauge-val">{{ env.displayValue }}</strong>
                <span class="env-status-badge" :data-level="env.level" :class="`badge-level-${env.level}`">
                  {{ env.level }}
                </span>
              </div>
              <div class="env-card-right">
                <span class="env-sparkline-title">📈 今日波动趋势</span>
                <div class="env-sparkline-wrap">
                  <svg v-if="getTodayPoints(env.key).length >= 2" class="env-sparkline" viewBox="0 0 160 50">
                    <defs>
                      <linearGradient :id="`grad-${env.key}`" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" :stop-color="getSparklineColor(env.key)" stop-opacity="0.3" />
                        <stop offset="100%" :stop-color="getSparklineColor(env.key)" stop-opacity="0.0" />
                      </linearGradient>
                    </defs>
                    <path :d="getTodaySparkline(env.key, 'area')" :fill="`url(#grad-${env.key})`" />
                    <path :d="getTodaySparkline(env.key, 'line')" fill="none" :stroke="getSparklineColor(env.key)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <div v-else class="sparkline-placeholder">暂无今日趋势数据</div>
                </div>
              </div>
            </article>
          </section>

          <!-- 重新设计的照片与录音记录卡片 -->
          <section class="record-grid-fancy" aria-label="今日成长记录">
            <button class="record-tile-button photo-tile" type="button" @click="thirdView = 'report-photos'">
              <div class="tile-icon-bg">📸</div>
              <div class="tile-content">
                <h3>相册瞬间</h3>
                <p>已捕捉 {{ archivePhotoRecords.length }} 张日常瞬间</p>
              </div>
              <span class="tile-arrow">→</span>
            </button>
            <button class="record-tile-button audio-tile" type="button" @click="thirdView = 'report-recordings'">
              <div class="tile-icon-bg">🎵</div>
              <div class="tile-content">
                <h3>学舌与叫声</h3>
                <p>已录制 {{ recordingRecords.length }} 段音频片段</p>
              </div>
              <span class="tile-arrow">→</span>
            </button>
          </section>
        </section>

        <section v-else-if="thirdView === 'daily-detail' || thirdView === 'weekly-detail' || thirdView === 'monthly-detail'" class="third-page report-detail-page">
          <p v-if="environmentLoading" class="report-status-hint">加载中…</p>
          <p v-else-if="environmentHistory.length === 0" class="report-status-hint">该周期暂无数据</p>
          <template v-else>
            <!-- 统计卡片 -->
            <section class="report-stat-grid" aria-label="报告关键指标">
              <article class="highlight-card">
                <span>健康评分</span>
                <strong>{{ computeHealthScore() }}</strong>
              </article>
              <article class="highlight-card">
                <span>进食次数</span>
                <strong>{{ behaviorCountOf('进食') || '-' }}</strong>
              </article>
              <article class="highlight-card">
                <span>排泄次数</span>
                <strong>-</strong>
              </article>
              <article class="highlight-card">
                <span>鸣叫次数</span>
                <strong>-</strong>
              </article>
            </section>
            <!-- 曲线 -->
            <section class="curve-grid">
              <button
                v-for="curve in reportCurves"
                :key="curve.label"
                class="curve-card curve-button"
                type="button"
                @click="openCurve(curve)"
              >
                <header><h2>{{ curve.label }}</h2><strong>{{ curve.value }}</strong></header>
                <svg class="mini-line-chart" viewBox="0 0 260 92"><polyline :points="linePoints(curve.points)" /></svg>
              </button>
            </section>
            <!-- 照片和录音 -->
            <section class="record-grid" style="margin-top: 20px;">
              <button class="module-card compact report-record-card gold-card" type="button" @click="thirdView = 'report-photos'">
                <h2>照片记录</h2>
                <p>{{ archivePhotoRecords.length }} 张照片</p>
              </button>
              <button class="module-card compact report-record-card gold-card" type="button" @click="thirdView = 'report-recordings'">
                <h2>录音</h2>
                <p>{{ recordingRecords.length }} 段录音</p>
              </button>
            </section>
          </template>
        </section>

        <section v-else-if="thirdView === 'report-photos'" class="third-page gallery-page">
          <header class="gallery-toolbar">
            <button type="button" @click="toggleGallerySelectMode">{{ gallerySelectMode ? ui.cancelSelect : ui.selectPhotos }}</button>
            <button v-if="gallerySelectMode" type="button" @click="exportSelectedPhotos">{{ ui.exportSelected }} {{ selectedPhotoKeys.length }}</button>
            <button v-if="gallerySelectMode" type="button" class="danger-action" @click="deletePhotos">{{ ui.deletePhotos }}</button>
            <button v-if="gallerySelectMode" type="button" @click="selectAllGalleryPhotos">{{ ui.selectAll }}</button>
          </header>
          <article
            v-for="photo in localizedArchivePhotoRecords"
            :key="photo.id || photo.title"
            class="photo-record-card"
            :class="{ selected: selectedPhotoKeys.includes(photoKey(photo)) }"
            tabindex="0"
            @click="handlePhotoClick(photo)"
            @keydown.enter="handlePhotoClick(photo)"
          >
            <span v-if="gallerySelectMode" class="photo-check" aria-hidden="true">{{ selectedPhotoKeys.includes(photoKey(photo)) ? '✓' : '' }}</span>
            <img class="photo-thumb" :src="photoSource(photo)" :alt="photo.title" />
            <strong>{{ photo.title }}</strong>
            <em>{{ photo.time }}</em>
          </article>
        </section>

        <section v-else-if="thirdView === 'report-recordings'" class="third-page records-page">
          <article v-for="recording in recordingRecords" :key="recording.title" class="audio-record-card">
            <button type="button" aria-label="播放录音"><span aria-hidden="true"></span></button>
            <div>
              <strong>{{ recording.title }}</strong>
              <em>{{ recording.time }} · {{ recording.length }}</em>
            </div>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'archive'">
        <section v-if="!thirdView" class="archive-page">
          <div class="archive-profile-list">
            <button
              v-for="(profile, index) in profiles"
              :key="profile.id"
              class="profile-card profile-card-fancy"
              :class="`profile-card-theme-${index % 5}`"
              type="button"
              @click="openArchiveProfile(profile)"
            >
              <div class="profile-avatar-wrapper">
                <span class="profile-avatar">
                  <img v-if="petAvatarSource(profile)" class="pet-avatar-photo" :src="petAvatarSource(profile)" :alt="profile.name" />
                  <ParrotVisual v-else :type="profile.avatarType || 'avatar-orange'" />
                </span>
                <span class="profile-gender-badge" :class="profile.sex === '公' ? 'gender-male' : 'gender-female'">
                  {{ profile.sex === '公' ? '♂' : '♀' }}
                </span>
              </div>
              
              <div class="profile-info-content">
                <div class="profile-name-row">
                  <strong class="profile-name">{{ profile.name }}</strong>
                  <span class="profile-age-pill" :class="'age-stage-' + (profile.ageStage === '成年' ? 'adult' : profile.ageStage === '幼年' ? 'child' : 'teen')">
                    {{ valueText(profile.ageStage) }}
                  </span>
                </div>
                
                <div class="profile-meta-tags">
                  <span class="meta-tag tag-species">
                    <span class="tag-icon">🐦</span>
                    <span class="tag-text">{{ valueText(profile.species) }}</span>
                  </span>
                  <span class="meta-tag tag-birthday">
                    <span class="tag-icon">📅</span>
                    <span class="tag-text">{{ profile.birthday }}</span>
                  </span>
                  <span class="meta-tag tag-weight">
                    <span class="tag-icon">⚖️</span>
                    <span class="tag-text">{{ profile.weight }}</span>
                  </span>
                </div>
              </div>
              
              <div class="profile-card-arrow">
                <span class="arrow-symbol">→</span>
              </div>
            </button>
          </div>
          <aside class="archive-actions">
            <button class="add-profile-fancy-btn" type="button" @click="openCreateProfile">
              <span class="btn-plus-icon">+</span>
              <span>{{ labelText('addProfile') }}</span>
            </button>
            <section class="archive-overview-card archive-overview-fancy">
              <div class="overview-header">
                <strong>档案数据概览</strong>
              </div>
              <div class="overview-stats-grid">
                <div class="overview-stat-item">
                  <span class="stat-num">{{ profiles.length }}</span>
                  <span class="stat-label">已建档案</span>
                </div>
                <div class="overview-stat-divider"></div>
                <div class="overview-stat-item">
                  <span class="stat-name-val">{{ selectedParrot.name }}</span>
                  <span class="stat-label">当前看护</span>
                </div>
              </div>
              <div class="overview-tip-banner">
                <span class="tip-icon">💡</span>
                <span class="tip-text">点击宠物卡片可快速修改资料、更换头像及删除档案。</span>
              </div>
            </section>
          </aside>
        </section>

        <section v-else-if="thirdView === 'archive-gallery'" class="third-page archive-gallery-page">
          <header class="gallery-toolbar">
            <button type="button" @click="toggleGallerySelectMode">{{ gallerySelectMode ? ui.cancelSelect : ui.selectPhotos }}</button>
            <button v-if="gallerySelectMode" type="button" @click="exportSelectedPhotos">{{ ui.exportSelected }} {{ selectedPhotoKeys.length }}</button>
            <button v-if="gallerySelectMode" type="button" class="danger-action" @click="deletePhotos">{{ ui.deletePhotos }}</button>
            <button v-if="gallerySelectMode" type="button" @click="selectAllGalleryPhotos">{{ ui.selectAll }}</button>
          </header>
          <article
            v-for="photo in localizedArchivePhotoRecords"
            :key="`archive-${photo.id || photo.title}`"
            class="archive-photo-tile"
            :class="{ selected: selectedPhotoKeys.includes(photoKey(photo)) }"
            tabindex="0"
            @click="handlePhotoClick(photo)"
            @keydown.enter="handlePhotoClick(photo)"
          >
            <span v-if="gallerySelectMode" class="photo-check" aria-hidden="true">{{ selectedPhotoKeys.includes(photoKey(photo)) ? '✓' : '' }}</span>
            <img class="photo-thumb" :src="photoSource(photo)" :alt="photo.title" />
            <strong>{{ photo.title }}</strong>
            <em>{{ selectedArchive.name }} · {{ photo.time }}</em>
          </article>
        </section>

        <section v-else class="third-page archive-third">
          <div class="archive-left-col">
            <!-- 宠物萌拍名片 -->
            <article class="profile-card profile-card-large cute-style">
              <!-- Cute Polaroid/Circular Avatar Wrapper with double rings -->
              <div class="profile-avatar-column">
                <span class="profile-avatar cute-avatar-ring">
                  <img v-if="petAvatarSource(selectedArchive)" class="pet-avatar-photo" :src="petAvatarSource(selectedArchive)" :alt="selectedArchive.name" />
                  <ParrotVisual v-else :type="selectedArchive.avatarType || 'avatar-orange'" />
                </span>
                <button type="button" class="avatar-edit-button" @click="openPetAvatarPicker">更换头像</button>
              </div>
              <div class="profile-info-text">
                <span class="profile-age">{{ valueText(selectedArchive.ageStage) }}</span>
                <strong>{{ selectedArchive.name }}</strong>
                <em>{{ profileMeta(selectedArchive, true) }}</em>
              </div>
              <div class="profile-card-actions">
                <button type="button" @click="openEditProfile(selectedArchive)">{{ text.edit }}</button>
                <button type="button" class="profile-delete-button" @click="confirmDeleteProfile(selectedArchive)">{{ text.deleteProfile }}</button>
              </div>
            </article>

            <!-- 趣味成长勋章 -->
            <article class="module-card profile-badges-card">
              <h2>✨ 鹦鹉成长趣闻勋章</h2>
              <div class="badges-wrapper">
                <span class="badge-tag tag-species-badge">🐦 {{ valueText(selectedArchive.species) || '小鹦鹉' }}家族</span>
                <span class="badge-tag tag-age-badge" :class="'age-' + selectedArchive.ageStage">🎂 {{ valueText(selectedArchive.ageStage) || '成长中' }}</span>
                <span class="badge-tag tag-weight-badge" v-if="parseWeight(selectedArchive.weight) > 0">
                  ⚖️ {{ parseWeight(selectedArchive.weight) > 90 ? '干饭王' : '健美达人' }}
                </span>
                <span class="badge-tag tag-gender-badge" :class="selectedArchive.sex === '公' ? 'male' : 'female'">
                  {{ selectedArchive.sex === '公' ? '♂ 帅气小男生' : selectedArchive.sex === '母' ? '♀ 温柔小女生' : '❔ 神秘宝贝' }}
                </span>
                <span class="badge-tag tag-device-badge" v-if="selectedArchive.deviceId">🤖 智能守护中</span>
              </div>
            </article>
          </div>

          <div class="archive-right-col">
            <!-- 体重监测与录入 -->
            <article class="module-card weight-input-card cute-input-card">
              <h2>⚖️ {{ labelText('recordWeight') }}</h2>
              <div class="weight-input-row">
                <label class="weight-number-field">
                  <span>{{ labelText('todayWeight') }}</span>
                  <div class="unit-input">
                    <input
                      :value="weightDraft"
                      inputmode="decimal"
                      type="text"
                      :placeholder="String(parseWeight(selectedArchive.weight) || '')"
                      @input="weightDraft = sanitizeWeight($event.target.value)"
                    />
                    <b>g</b>
                  </div>
                </label>
                <button type="button" class="save-weight-btn" @click="saveArchiveWeight">{{ text.save }}</button>
              </div>
            </article>

            <!-- 体重历史折线面积图 (SVG 可视化) -->
            <article class="module-card archive-action-module archive-weight-record cute-chart-card" @click="openWeightChart">
              <header class="chart-header">
                <h2>📈 {{ labelText('weightRecord') }}</h2>
                <span class="chart-note">{{ localizedWeightNote(selectedArchive.lastWeight) }}</span>
              </header>
              <div class="mini-chart-container">
                <svg class="mini-area-chart" viewBox="0 0 520 120" v-if="selectedArchive.weightHistory && selectedArchive.weightHistory.length">
                  <defs>
                    <linearGradient id="weightGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#f2b66e" stop-opacity="0.6"/>
                      <stop offset="100%" stop-color="#f2b66e" stop-opacity="0"/>
                    </linearGradient>
                  </defs>
                  <!-- Area path under the curve -->
                  <path 
                    :d="weightAreaPath(selectedArchive.weightHistory)" 
                    fill="url(#weightGrad)"
                  />
                  <!-- Curve line path -->
                  <path 
                    :d="weightCurvePath(selectedArchive.weightHistory)" 
                    fill="none" 
                    stroke="#d68c3d" 
                    stroke-width="5" 
                    stroke-linecap="round" 
                    stroke-linejoin="round"
                  />
                  <!-- Dots on points -->
                  <circle 
                    v-for="(pt, idx) in weightChartPointsList(selectedArchive.weightHistory)" 
                    :key="idx" 
                    :cx="pt.x" 
                    :cy="pt.y" 
                    r="6" 
                    fill="#fff" 
                    stroke="#d68c3d" 
                    stroke-width="3"
                  />
                </svg>
                <div v-else class="chart-empty-state">暂无体重数据</div>
              </div>
            </article>

            <!-- 成长相册 (拍立得手账风格) -->
            <article class="module-card archive-action-module archive-growth-album cute-album-card" @click="openThird('archive-gallery')">
              <h2>📸 {{ labelText('growthAlbum') }}</h2>
              <p class="album-subtitle">{{ photoCountText(archivePhotoRecords.length) }}，{{ labelText('autoArchive') }}</p>
              <div class="photo-polaroid-strip">
                <div
                  v-for="photo in archivePhotoPreview"
                  :key="`archive-preview-${photoKey(photo)}`"
                  class="photo-polaroid-frame"
                >
                  <img :src="photoSource(photo)" :alt="photo.title || ui.snapshotPhoto" />
                  <span class="polaroid-label">{{ photo.title || '日常瞬间' }}</span>
                </div>
                <div
                  v-for="index in archivePhotoPlaceholderCount"
                  :key="`archive-preview-placeholder-${index}`"
                  class="photo-polaroid-frame photo-placeholder-frame"
                >
                  <span class="photo-placeholder-inner"></span>
                  <span class="polaroid-label">虚位以待</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'medical'">
        <section v-if="!thirdView" class="module-list">
          <button
            v-for="module in localizedMedicalModules"
            :key="module.key"
            class="module-row"
            :class="`module-row-${module.key}`"
            type="button"
            @click="openThird(module.key)"
          >
            <span class="module-row-icon" aria-hidden="true">
              <img class="module-row-img" :src="medicalIcon" alt="" />
            </span>
            <span class="module-row-body">
              <h2 class="module-row-title">{{ module.title }}</h2>
              <p class="module-row-note">{{ module.note }}</p>
            </span>
            <span class="module-row-chevron" aria-hidden="true">›</span>
          </button>
        </section>

        <section v-else-if="thirdView === 'diagnosis'" class="third-page form-page">
          <article class="triage-card">
            <header class="triage-header">
              <div class="triage-title-block">
                <h2>{{ labelText('diagnosisTitle') }}</h2>
                <p class="triage-subtitle">{{ labelText('triageSubtitle') }}</p>
              </div>
              <div class="triage-risk-badge" :data-level="liveRisk.level" role="status" :aria-label="labelText('triageLiveRisk')">
                <span class="triage-risk-label">{{ labelText('triageLiveRisk') }}</span>
                <strong class="triage-risk-level">{{ liveRisk.label }}</strong>
              </div>
            </header>
            <div class="triage-dims">
              <section v-for="field in diagnosisFields" :key="field.key" class="triage-dim" :class="`triage-dim-${field.key}`">
                <div class="triage-dim-head">
                  <h3 class="triage-dim-title">{{ field.label }}</h3>
                  <p class="triage-dim-hint">{{ field.dimHint }}</p>
                </div>
                <div class="triage-chips" role="radiogroup" :aria-label="field.label">
                  <button
                    v-for="opt in field.options"
                    :key="opt.value"
                    type="button"
                    class="triage-chip"
                    :class="`triage-chip-l${opt.score}`"
                    :data-level="opt.score"
                    role="radio"
                    :aria-checked="diagnosisForm[field.key] === opt.value"
                    @click="diagnosisForm[field.key] = opt.value"
                  >
                    <span class="triage-chip-dot" aria-hidden="true"></span>
                    <span class="triage-chip-text">{{ opt.text }}</span>
                    <span class="triage-chip-hint">{{ opt.hint }}</span>
                  </button>
                </div>
              </section>
            </div>
            <footer class="triage-foot">
              <button type="button" class="triage-submit" @click="submitDiagnosis">{{ labelText('submit') }}</button>
              <p class="triage-disclaimer-mini">{{ labelText('triageDisclaimer') }}</p>
            </footer>
          </article>
        </section>

        <section v-else-if="thirdView === 'hospitals'" class="third-page map-page">
          <article class="map-card">
            <div id="amap-container" class="map-canvas" aria-label="附近医院地图"></div>
            <aside class="hospital-info" style="display: flex; flex-direction: column; height: 100%;">
              <div class="search-row" style="display: flex; gap: 10px; width: 100%; margin-bottom: 16px;">
                <input
                  v-model="hospitalSearchQuery"
                  class="search-input"
                  style="flex: 1; margin: 0;"
                  placeholder="搜索医院名称/地址..."
                  @keyup.enter="triggerHospitalSearch(hospitalSearchQuery)"
                />
                <button
                  class="refresh-button-inline"
                  type="button"
                  @click="refreshHospitals"
                  style="padding: 0 16px; height: 50px; border: 0; border-radius: 999px; background: linear-gradient(145deg, #a23b5d, #bd5378); color: #fff; font-size: 14px; font-weight: 800; cursor: pointer; transition: all 0.2s ease; box-shadow: 0 4px 10px rgba(162, 59, 93, 0.2);"
                >
                  {{ labelText('refresh') }}
                </button>
              </div>
              <div class="hospital-list-container">
                <div
                  v-for="hospital in filteredHospitalPins"
                  :key="hospital.id"
                  class="hospital-list-item"
                  :class="{ active: selectedHospital && selectedHospital.id === hospital.id }"
                  @click="selectedHospital = hospital"
                >
                  <h3>{{ hospitalName(hospital) }}</h3>
                  <p class="hospital-addr">{{ hospitalAddress(hospital) }}</p>
                </div>
                <div v-if="filteredHospitalPins.length === 0" class="no-hospital-results">
                  未找到匹配的医院
                </div>
              </div>
              
              <div v-if="selectedHospital" class="selected-hospital-pane" style="margin-top: auto;">
                <div class="hospital-divider"></div>
                <p><strong>联系电话:</strong> {{ selectedHospital.phone }}</p>
                <a
                  v-if="selectedHospital.website"
                  :href="selectedHospital.website"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="hospital-action-link"
                >
                  访问官方网站 / 挂号预约
                </a>
              </div>
            </aside>
          </article>
        </section>

        <section v-else-if="thirdView === 'health'" class="third-page form-page">
          <MedicalCharts
            :records="medicalRecords"
            :dark="systemPrefs.theme === 'dark'"
            :types="localizedMedRecordTypes"
            :labels="medHealthLabels"
          />
        </section>

        <section v-else class="third-page records-page">
          <input v-model="medicalRecordSearch" class="search-input" :placeholder="labelText('searchRecord')" />
          <article class="med-record-form">
            <div class="med-record-form-row">
              <label class="med-record-field med-field-date">
                <span>{{ labelText('medDateLabel') }}</span>
                <input v-model="newMedicalDraft.recordDate" type="date" :max="todayText" />
              </label>
              <label class="med-record-field med-field-type">
                <span>{{ labelText('medTypeLabel') }}</span>
                <select v-model="newMedicalDraft.recordType">
                  <option v-for="t in localizedMedRecordTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
                </select>
              </label>
            </div>
            <input v-model="newMedicalDraft.content" class="med-record-form-content" :placeholder="labelText('medContentPlaceholder')" />
            <button v-if="!medFormMoreOpen" type="button" class="med-record-more-toggle" @click="medFormMoreOpen = true">{{ labelText('medMore') }}</button>
            <div v-else class="med-record-form-more">
              <input v-model="newMedicalDraft.hospitalName" :placeholder="labelText('medHospitalPlaceholder')" />
              <input v-model="newMedicalDraft.hospitalPhone" :placeholder="labelText('medPhonePlaceholder')" />
              <button type="button" class="med-record-more-toggle" @click="medFormMoreOpen = false">{{ labelText('medLess') }}</button>
            </div>
            <button type="button" class="med-record-form-add" @click="addMedicalRecord">{{ labelText('medAdd') }}</button>
          </article>
          <article
            v-for="record in filteredMedicalRecords"
            :key="record.id"
            class="memo-card editable-memo med-record-card"
            :class="{ 'is-editing': editingMedicalId === record.id }"
          >
            <div class="med-record-date" aria-hidden="true">
              <span class="med-record-month">{{ medRecordMonth(record) }}</span>
              <span class="med-record-day">{{ medRecordDay(record) }}</span>
            </div>
            <div class="med-record-body">
              <template v-if="editingMedicalId === record.id">
                <div class="med-record-edit-grid">
                  <label class="med-record-field med-field-date">
                    <span>{{ labelText('medDateLabel') }}</span>
                    <input v-model="editingMedicalDraft.recordDate" type="date" :max="todayText" />
                  </label>
                  <label class="med-record-field med-field-type">
                    <span>{{ labelText('medTypeLabel') }}</span>
                    <select v-model="editingMedicalDraft.recordType">
                      <option v-for="t in localizedMedRecordTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
                    </select>
                  </label>
                </div>
                <textarea v-model="editingMedicalDraft.content" class="med-record-edit-content" :placeholder="labelText('medContentPlaceholder')" rows="2"></textarea>
                <input v-model="editingMedicalDraft.hospitalName" :placeholder="labelText('medHospitalPlaceholder')" />
                <input v-model="editingMedicalDraft.hospitalPhone" :placeholder="labelText('medPhonePlaceholder')" />
              </template>
              <template v-else>
                <span class="med-type-tag" :data-type="record.recordType || 'other'">{{ medRecordTypeLabel(record.recordType) }}</span>
                <p class="med-record-text">{{ medRecordText(record) }}</p>
                <p v-if="record.hospitalName" class="med-record-hospital">
                  {{ record.hospitalName }}<template v-if="record.hospitalPhone"> · {{ record.hospitalPhone }}</template>
                </p>
              </template>
            </div>
            <div class="med-record-actions">
              <template v-if="editingMedicalId === record.id">
                <button type="button" class="med-record-action is-primary" @click="saveMedicalRecord(record)">{{ text.save }}</button>
                <button type="button" class="med-record-action" @click="cancelEditMedical">{{ labelText('medCancel') }}</button>
              </template>
              <template v-else>
                <button type="button" class="med-record-action" @click="startEditMedical(record)">{{ labelText('modify') }}</button>
                <button type="button" class="med-record-action" @click="deleteMedicalRecord(record)">{{ labelText('delete') }}</button>
              </template>
            </div>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'handbook'">
        <section v-if="!thirdView" class="module-list">
          <button
            v-for="module in localizedHandbookModules"
            :key="module.key"
            class="module-row"
            :class="`module-row-${module.key}`"
            type="button"
            @click="openThird(module.key)"
          >
            <span class="module-row-icon" aria-hidden="true">
              <img class="module-row-img" :src="handbookIcon" alt="" />
            </span>
            <span class="module-row-body">
              <h2 class="module-row-title">{{ module.title }}</h2>
              <p class="module-row-note">{{ module.note }}</p>
            </span>
            <span class="module-row-chevron" aria-hidden="true">›</span>
          </button>
        </section>

        <section v-else-if="thirdView === 'care-profile'" class="third-page care-profile-page">
          <!-- 品种速览 -->
          <article class="memo-card care-card">
            <h2 class="care-card-title">{{ currentSpeciesCare.name }}</h2>
            <p v-if="currentSpeciesCare.fallback" class="care-hint">{{ labelText('careProfileFallback') }}</p>
            <ul class="care-overview">
              <li><span>{{ labelText('careProfileOrigin') }}</span><strong>{{ currentSpeciesCare.origin }}</strong></li>
              <li><span>{{ labelText('careProfileBodyLength') }}</span><strong>{{ currentSpeciesCare.bodyLength }}</strong></li>
              <li><span>{{ labelText('careProfileWeight') }}</span><strong>{{ currentSpeciesCare.weight }}</strong></li>
              <li><span>{{ labelText('careProfileLifespan') }}</span><strong>{{ currentSpeciesCare.lifespan }}</strong></li>
              <li class="care-overview-full"><span>{{ labelText('careProfileTalking') }}</span><strong>{{ currentSpeciesCare.talkingAbility }}</strong></li>
            </ul>
            <p class="care-temperament">{{ currentSpeciesCare.temperament }}</p>
          </article>

          <!-- 环境适配度评分 -->
          <article class="memo-card care-card env-score-card" :class="`env-level-${envMatch.levelKey}`">
            <h2 class="care-card-title">{{ labelText('envScoreTitle') }}</h2>
            <p class="care-card-sub">{{ labelText('envScoreSubtitle') }}</p>
            <!-- 预览别的品种时：传感器数据是共享的，只是比对区间换了，提示一下 -->
            <p v-if="carePreviewingOther" class="care-preview-note">{{ labelText('careProfileEnvPreview') }}</p>
            <!-- 首次加载中（刚点进页面、网络请求还没返回） -->
            <div v-if="!realtimeEverLoaded" class="env-score-offline">
              <p>{{ labelText('envLoading') }}</p>
            </div>
            <!-- 加载过了但确实无数据 -->
            <div v-else-if="envMatch.total == null" class="env-score-offline">
              <p>{{ labelText('envNotConnected') }}</p>
              <button type="button" @click="refreshEnvSnapshot">{{ labelText('envRefresh') }}</button>
            </div>
            <template v-else>
              <div class="env-score-total">
                <strong>{{ envMatch.total }}</strong><span>/100</span>
              </div>
              <p v-if="envMatch.partial" class="env-score-partial">{{ labelText('envPartialNote') }}</p>
              <div class="env-score-items">
                <div v-for="item in envMatch.items" :key="item.key" class="env-score-item">
                  <div class="env-score-item-head">
                    <span>
                      {{ item.label }}
                      <em v-if="item.stale" class="env-stale-tag">{{ labelText('envLastRecord') }}</em>
                    </span>
                    <strong>{{ item.value == null ? '--' : Number(item.value).toFixed(1) }}{{ item.value == null ? '' : item.unit }}</strong>
                  </div>
                  <div class="env-score-bar">
                    <i :class="`env-level-${envLevelKey(item.score)}`" :style="{ width: (item.score == null ? 0 : item.score) + '%' }"></i>
                  </div>
                  <div class="env-score-item-foot">
                    <span>{{ labelText('envRange') }}：{{ item.rangeText }}</span>
                    <em :class="`env-level-${envLevelKey(item.score)}`">{{ item.advice }}</em>
                  </div>
                </div>
              </div>
            </template>
          </article>

          <!-- 专属环境与风险 -->
          <article class="memo-card care-card">
            <h2 class="care-card-title">{{ labelText('careProfileEnv') }}</h2>
            <ul class="care-overview">
              <li><span>{{ labelText('careProfileTempRange') }}</span><strong>{{ currentSpeciesCare.tempRange[0] }}–{{ currentSpeciesCare.tempRange[1] }} ℃</strong></li>
              <li><span>{{ labelText('careProfileHumidityRange') }}</span><strong>{{ currentSpeciesCare.humidityRange[0] }}–{{ currentSpeciesCare.humidityRange[1] }} %</strong></li>
              <li><span>{{ labelText('careProfileDustLevel') }}</span><strong>{{ currentSpeciesCare.dustLevel }}</strong></li>
              <li><span>{{ labelText('careProfileDustTolerance') }}</span><strong>{{ dustToleranceText(currentSpeciesCare.dustTolerance) }}</strong></li>
            </ul>
            <h3 class="care-section-sub">{{ labelText('careProfileRisks') }}</h3>
            <div class="food-chips">
              <span v-for="risk in currentSpeciesCare.risks" :key="risk" class="food-chip risk-chip">{{ risk }}</span>
            </div>
          </article>

          <!-- 推荐食谱 -->
          <article class="memo-card care-card">
            <h2 class="care-card-title">{{ labelText('careProfileDiet') }}</h2>
            <!-- 食谱配比饼图：进入页面/切换品种时由 initDietChart 绘制 -->
            <div ref="dietChartRef" class="diet-chart"></div>
            <h3 class="care-section-sub">{{ labelText('careProfileRecommended') }}</h3>
            <div class="food-chips">
              <span v-for="food in currentSpeciesCare.recommendedFoods" :key="food" class="food-chip safe-chip">{{ food }}</span>
            </div>
            <h3 class="care-section-sub">{{ labelText('careProfileToxic') }}</h3>
            <div class="food-chips">
              <span
                v-for="food in currentSpeciesCare.toxicFoods"
                :key="food"
                class="food-chip toxic-chip"
                :data-reason="getToxicReason(food)"
                tabindex="0"
              >{{ food }}</span>
            </div>
          </article>

          <!-- 专属护理建议 -->
          <article class="memo-card care-card">
            <h2 class="care-card-title">{{ labelText('careProfileAdvice') }}</h2>
            <ul class="care-advice-list">
              <li v-for="(item, idx) in currentSpeciesCare.careAdvice" :key="idx" class="care-advice-item">
                <strong class="care-advice-title">{{ item.title }}</strong>
                <span class="care-advice-text">{{ item.text }}</span>
              </li>
            </ul>
          </article>
        </section>

        <section v-else-if="thirdView === 'tutorials'" class="third-page records-page tutorial-list-page">
          <header class="tutorial-list-header">
            <h2>{{ labelText('tutorialListTitle') }}</h2>
            <span class="tutorial-list-count">{{ labelText('tutorialCount').replace('{n}', filteredTutorials.length) }}</span>
          </header>
          <input v-model="tutorialKeyword" class="search-input" :placeholder="labelText('tutorialSearch')" />
          <article
            v-for="tutorial in filteredTutorials"
            :key="tutorial.id"
            class="memo-card tutorial-list-card"
            tabindex="0"
            :style="{ '--cat-color': tutorialCategory(tutorial.tag).color }"
            @click="openTutorialDetail(tutorial.id)"
            @keydown.enter="openTutorialDetail(tutorial.id)"
          >
            <span class="tutorial-cat-icon" aria-hidden="true">{{ tutorialCategory(tutorial.tag).icon }}</span>
            <div class="tutorial-list-body">
              <div class="tutorial-list-head">
                <strong class="tutorial-list-title">{{ tutorial.title }}</strong>
                <span class="tutorial-tag-chip">{{ tutorial.tag }}</span>
              </div>
              <p class="tutorial-list-summary">{{ tutorial.summary }}</p>
              <div class="tutorial-list-foot">
                <span class="tutorial-read-time">⏱ {{ tutorial.minutes }}</span>
                <span class="tutorial-go" aria-hidden="true">{{ labelText('tutorialRead') }} →</span>
              </div>
            </div>
          </article>
          <p v-if="!filteredTutorials.length" class="tutorial-empty">{{ labelText('tutorialEmpty') }}</p>
        </section>

        <section v-else-if="thirdView === 'tutorial-detail'" class="third-page tutorial-detail-page">
          <button class="back-to-list" type="button" @click="thirdView = 'tutorials'">
            ← {{ labelText('tutorialBack') }}
          </button>
          <article v-if="activeTutorial" class="tutorial-hero memo-card">
            <span class="tutorial-meta">{{ activeTutorial.tag }} · {{ activeTutorial.minutes }}</span>
            <h2>{{ activeTutorial.title }}</h2>
            <p>{{ activeTutorial.summary }}</p>
          </article>
          <p v-if="tutorialArticleLoading" class="tutorial-status">教程加载中…</p>
          <p v-else-if="tutorialArticleError" class="tutorial-status tutorial-error">{{ tutorialArticleError }}</p>
          <article
            v-else-if="tutorialArticleHtml"
            class="tutorial-article memo-card"
            v-html="tutorialArticleHtml"
          ></article>
        </section>

        <section v-else class="third-page form-page">
          <article class="questionnaire-card">
            <h2>{{ labelText('birdTitle') }}</h2>
            <label class="bird-file-field">
              <span>{{ labelText('choosePhoto') }}</span>
              <span class="bird-file-picker">
                <span class="bird-file-button">{{ labelText('chooseFile') }}</span>
                <strong>{{ birdImage?.name || labelText('noFile') }}</strong>
              </span>
              <input class="bird-file-input" type="file" accept="image/*" capture="environment" @change="onBirdImageChange" />
            </label>
            <figure v-if="birdImagePreview" class="bird-preview">
              <img :src="birdImagePreview" :alt="labelText('birdAlt')" />
            </figure>
            <p v-if="birdError" class="bird-error">{{ birdError }}</p>
            <button type="button" :disabled="birdLoading" @click="recognizeBird">{{ birdLoading ? labelText('recognizing') : labelText('recognize') }}</button>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'ledger'">
        <section class="third-page records-page ledger-page ledger-page-v2">
          <header class="ledger-summary-grid">
            <article class="ledger-summary-card ledger-summary-primary">
              <span>本月支出</span>
              <strong>¥{{ formatLedgerAmount(ledgerMonthTotal) }}</strong>
              <small>{{ currentMonthText.replace('-', ' 年 ') }} 月</small>
            </article>
            <article class="ledger-summary-card">
              <span>{{ labelText('ledgerTotal') }}</span>
              <strong>¥{{ formatLedgerAmount(ledgerTotal) }}</strong>
              <small>当前宠物的全部记录</small>
            </article>
            <article class="ledger-summary-card">
              <span>记账笔数</span>
              <strong>{{ ledgerRecordCount }}</strong>
              <small>每一笔照护都有记录</small>
            </article>
          </header>
          <LedgerCharts :records="ledgerRecords" :dark="systemPrefs.theme === 'dark'" />
          <div class="ledger-toolbar">
            <label class="ledger-search-field">
              <span aria-hidden="true">⌕</span>
              <input v-model="ledgerKeyword" class="search-input" :placeholder="labelText('searchLedger')" />
            </label>
            <label class="ledger-category-filter">
              <span>分类</span>
              <select v-model="ledgerCategoryFilter">
                <option value="全部">全部分类</option>
                <option v-for="category in LEDGER_CATEGORIES" :key="category" :value="category">{{ category }}</option>
              </select>
            </label>
            <button class="ledger-create-button" type="button" @click="openLedgerCreate">
              <span aria-hidden="true">＋</span>记一笔
            </button>
          </div>
          <p v-if="ledgerFeedback" class="ledger-feedback" role="status">{{ ledgerFeedback }}</p>
          <div class="ledger-table-head" aria-hidden="true">
            <span>{{ labelText('ledgerDate') }}</span>
            <span>{{ labelText('ledgerTag') }}</span>
            <span>{{ labelText('ledgerDescription') }}</span>
            <span>{{ labelText('ledgerAmount') }}</span>
            <span>{{ labelText('action') }}</span>
          </div>
          <article v-for="record in filteredLedgerRecords" :key="record.id" class="ledger-record-card">
            <template v-if="editingLedgerId === record.id && editingLedgerDraft">
              <input v-model="editingLedgerDraft.time" type="date" :max="todayText" />
              <select v-model="editingLedgerDraft.tag">
                <option v-for="category in LEDGER_CATEGORIES" :key="category" :value="category">{{ category }}</option>
              </select>
              <input v-model="editingLedgerDraft.description" />
              <input v-model.number="editingLedgerDraft.amount" type="number" min="0" step="0.01" />
              <button type="button" @click="saveLedgerRecord(record)">{{ text.save }}</button>
            </template>
            <template v-else>
              <span>{{ record.time }}</span>
              <strong>{{ ledgerTagText(record) }}</strong>
              <div class="ledger-description-cell">
                <p>{{ ledgerDescriptionText(record) }}</p>
                <small>{{ record.updatedAt ? `${labelText('updated')} ${record.updatedAt}` : `${labelText('created')} ${record.createdAt}` }}</small>
              </div>
              <em>¥{{ formatLedgerAmount(record.amount) }}</em>
              <div class="ledger-row-actions">
                <button type="button" @click="startEditLedger(record)">{{ text.edit }}</button>
                <button class="ledger-delete-button" type="button" @click="confirmDeleteLedger(record)">删除</button>
              </div>
            </template>
          </article>
          <div v-if="!filteredLedgerRecords.length" class="ledger-empty-state">
            <span aria-hidden="true">¥</span>
            <strong>{{ ledgerKeyword || ledgerCategoryFilter !== '全部' ? '没有找到匹配的消费记录' : '还没有消费记录' }}</strong>
            <p>{{ ledgerKeyword || ledgerCategoryFilter !== '全部' ? '尝试更换关键词或分类再次搜索。' : '记录宠物的第一笔照护支出吧。' }}</p>
            <button v-if="!ledgerKeyword && ledgerCategoryFilter === '全部'" type="button" @click="openLedgerCreate">记下第一笔</button>
          </div>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'settings'">
        <section class="settings-page settings-system-page">
          <article class="settings-profile-card">
            <div class="settings-profile-actions">
              <button class="settings-edit-button" type="button" @click="toggleSettingsEdit">
                {{ isSettingsEditing ? text.save : text.edit }}
              </button>
              <button class="settings-logout-button" type="button" @click="handleLogout">
                {{ text.logout }}
              </button>
              <button class="settings-delete-account-button" type="button" @click="confirmDeleteAccount">
                {{ text.deleteAccount }}
              </button>
            </div>
            <div class="settings-avatar-wrap">
              <span class="settings-avatar">
                <ParrotVisual :type="settingsAvatarType" />
              </span>
              <label v-if="isSettingsEditing" class="avatar-upload-button">
                上传
                <input type="file" accept="image/*" @change="onUserAvatarChange" />
              </label>
            </div>
            <label class="settings-name-row">
              <span>{{ text.username }}</span>
              <input v-if="isSettingsEditing" v-model="settingsDraft.username" />
              <strong v-else>{{ account.username }}</strong>
            </label>
            <p v-if="loginUser?.userRole" class="settings-user-role">
              {{ loginUser.userRole }}
            </p>
            <p class="settings-user-id">{{ text.userId }}：{{ account.userId }}</p>
            <label class="settings-location">
              <span>{{ text.location }}</span>
              <input v-if="isSettingsEditing" v-model="settingsDraft.location" :placeholder="text.location" />
              <strong v-else>{{ account.location || text.unbound }}</strong>
            </label>
            <div class="settings-phone-row">
              <span>{{ text.phone }}</span>
              <strong v-if="!isSettingsEditing">{{ account.phoneBound ? account.phone : text.unbound }}</strong>
              <input v-else v-model="settingsDraft.phone" inputmode="numeric" maxlength="11" :placeholder="text.inputPhone" />
            </div>
            <div class="settings-phone-row settings-email-row">
              <span>{{ text.email }}</span>
              <strong v-if="!isSettingsEditing">{{ account.emailBound ? account.email : text.unbound }}</strong>
              <input v-else v-model="settingsDraft.email" type="email" :placeholder="text.inputEmail" />
            </div>
            <div v-if="isSettingsEditing" class="settings-password-section">
              <template v-if="!passwordChanging">
                <button type="button" class="settings-change-password-button" @click="togglePasswordChange">{{ text.changePassword }}</button>
              </template>
              <template v-else>
                <div class="settings-password-fields">
                  <input v-model="oldPassword" type="password" :placeholder="text.currentPassword" autocomplete="current-password" />
                  <input v-model="newPassword" type="password" :placeholder="text.newPassword" autocomplete="new-password" />
                  <input v-model="newPasswordConfirm" type="password" :placeholder="text.confirmNewPassword" autocomplete="new-password" />
                  <div class="settings-password-actions">
                    <button type="button" class="settings-password-cancel" @click="togglePasswordChange">{{ text.cancel }}</button>
                    <button type="button" class="settings-password-submit" @click="submitPasswordChange">{{ text.confirm }}</button>
                  </div>
                  <p v-if="passwordMessage" :class="passwordMessage === text.passwordChanged ? 'password-message-success' : 'password-message-error'">{{ passwordMessage }}</p>
                </div>
              </template>
            </div>
          </article>
          <section class="settings-system-card" aria-label="系统设置">
            <article class="settings-option-row">
              <span>{{ text.language }}</span>
              <div class="settings-segmented">
                <button type="button" :class="{ active: systemPrefs.language === 'zh' }" @click="savePreferencePatch({ language: 'zh' })">{{ text.chinese }}</button>
                <button type="button" :class="{ active: systemPrefs.language === 'en' }" @click="savePreferencePatch({ language: 'en' })">{{ text.english }}</button>
                <button type="button" :class="{ active: systemPrefs.language === 'es' }" @click="savePreferencePatch({ language: 'es' })">{{ text.spanish }}</button>
                <button type="button" :class="{ active: systemPrefs.language === 'ja' }" @click="savePreferencePatch({ language: 'ja' })">{{ text.japanese }}</button>
              </div>
            </article>
            <article class="settings-option-row">
              <span>{{ text.theme }}</span>
              <div class="settings-segmented">
                <button type="button" :class="{ active: systemPrefs.theme === 'light' }" @click="savePreferencePatch({ theme: 'light' })">{{ text.day }}</button>
                <button type="button" :class="{ active: systemPrefs.theme === 'dark' }" @click="savePreferencePatch({ theme: 'dark' })">{{ text.night }}</button>
              </div>
            </article>
            <article class="settings-option-row">
              <span>{{ text.font }}</span>
              <strong>{{ text.defaultFont }}</strong>
            </article>
            <article class="settings-option-row">
              <span>{{ text.fontSize }}</span>
              <input v-model.number="systemPrefs.fontSize" type="range" min="12" max="28" step="1" @change="savePreferencePatch({ fontSize: systemPrefs.fontSize })" />
              <strong>{{ systemPrefs.fontSize }}pt</strong>
            </article>
            <article class="settings-option-row">
              <span>{{ text.color }}</span>
              <strong>{{ settingsColorLabel }}</strong>
            </article>
            <button class="settings-info-button" type="button" @click="openApiKeysModal">{{ text.apiKeySettings }}</button>
            <button class="settings-info-button" type="button" @click="openQqConnectionModal">{{ text.connectQq }}</button>
            <button class="settings-info-button" type="button" @click="openModal('setting-toggles', text.permissions)">{{ text.permissions }}</button>
            <button class="settings-info-button" type="button" @click="openSettingsInfo('about')">{{ text.about }}</button>
            <button class="settings-info-button" type="button" @click="openSettingsInfo('system')">{{ text.system }}</button>
            <button class="settings-info-button" type="button" @click="openSettingsInfo('version')">{{ text.version }}</button>
          </section>
          <!-- 头像裁剪弹窗 -->
          <Teleport to="body">
            <div v-if="showAvatarCropDialog" class="avatar-crop-overlay" @click.self="cancelAvatarCrop">
              <div class="avatar-crop-dialog">
                <h3>裁剪头像</h3>
                <div class="avatar-crop-area">
                  <Cropper
                    ref="avatarCropperRef"
                    :src="pendingAvatarSrc"
                    :stencil-component="CircleStencil"
                    :stencil-props="{ aspectRatio: 1 }"
                    :canvas="true"
                    image-restriction="stencil"
                    class="avatar-cropper"
                  />
                </div>
                <div class="avatar-crop-actions">
                  <button type="button" class="avatar-crop-cancel" @click="cancelAvatarCrop">取消</button>
                  <button type="button" class="avatar-crop-confirm" @click="confirmAvatarCrop">确认裁剪</button>
                </div>
              </div>
            </div>
          </Teleport>
        </section>
      </template>
    </section>

    <!-- 非全屏时保持原挂载位置；监控全屏时将弹窗移入 MonitorCard 的全屏 DOM。 -->
    <Teleport :to="monitorFullscreen ? '#monitor-modal-host' : 'body'" :disabled="!monitorFullscreen">
    <div v-if="modal" class="modal-backdrop" role="presentation" @click.self="closeModal">
      <section class="edit-modal" :class="`modal-${modal.type}`" role="dialog" aria-modal="true" :aria-label="modal.title">
        <header>
          <h2>{{ modal.title }}</h2>
          <button type="button" aria-label="关闭弹窗" @click="closeModal">×</button>
        </header>
        <div class="modal-body">
          <template v-if="modal.type === 'archive-create' || modal.type === 'archive-edit'">
            <label>
              <span>{{ labelText('parrotSpecies') }}</span>
              <select v-model="profileForm.species">
                <option v-for="species in parrotSpeciesOptions" :key="species" :value="species">{{ valueText(species) }}</option>
              </select>
            </label>
            <label><span>{{ labelText('parrotName') }}</span><input v-model="profileForm.name" placeholder="例如：农药" /></label>
            <label><span>{{ labelText('birthday') }}</span><input v-model="profileForm.birthday" placeholder="xxxx-xx-xx" /></label>
            <label><span>{{ labelText('ageStage') }}</span><input :value="valueText(profileFormAgeStage)" readonly /></label>
            <label>
              <span>性别</span>
              <select v-model="profileForm.sex">
                <option value="未知">未知</option>
                <option value="公">公</option>
                <option value="母">母</option>
              </select>
            </label>
            <label>
              <span>当前状态</span>
              <select v-model="profileForm.currentStatus">
                <option value="站立">站立</option>
                <option value="吃东西">吃东西</option>
                <option value="睡觉">睡觉</option>
                <option value="大叫">大叫</option>
                <option value="扇翅膀">扇翅膀</option>
              </select>
            </label>
            <label>
              <span>{{ text.bindDevice }}</span>
              <select v-model="profileForm.deviceId">
                <option value="">{{ text.noDevice }}</option>
                <option v-for="device in availableDevices" :key="device.deviceId" :value="device.deviceId">
                  {{ device.name || device.deviceId }}{{ device.location ? ` · ${device.location}` : '' }}
                </option>
              </select>
            </label>
            <label v-if="modal.type === 'archive-create'"><span>{{ labelText('currentWeight') }}</span><input v-model="profileForm.weight" placeholder="例如：78g" /></label>
          </template>
          <template v-else-if="modal.type === 'setting-toggles'">
            <div class="setting-toggle-row">
              <span>{{ labelText('notifications') }}</span>
              <button type="button" :class="{ active: notificationEnabled }" @click="toggleNotificationPreference"></button>
            </div>
            <div class="setting-toggle-row">
              <span>{{ labelText('devicePermissions') }}</span>
              <button type="button" :class="{ active: permissionEnabled }" @click="togglePermissionPreference"></button>
            </div>
          </template>
          <template v-else-if="modal.type === 'diagnosis'">
            <div class="triage-result">
              <div class="triage-result-badge" :data-level="modal.item.level">
                <span class="triage-result-level">{{ modal.item.levelLabel }}</span>
              </div>
              <p class="triage-result-summary">{{ modal.item.summary }}</p>
              <ul v-if="modal.item.redFlags.length" class="triage-result-flags triage-result-red">
                <li v-for="(flag, i) in modal.item.redFlags" :key="`rf-${i}`">
                  <span class="triage-flag-mark" aria-hidden="true">!</span>
                  <span>{{ flag }}</span>
                </li>
              </ul>
              <ul v-if="modal.item.flags.length" class="triage-result-flags">
                <li v-for="(flag, i) in modal.item.flags" :key="`f-${i}`">
                  <span class="triage-flag-mark" aria-hidden="true">·</span>
                  <span><strong>{{ flag.dim }} · {{ flag.text }}</strong> — {{ flag.hint }}</span>
                </li>
              </ul>
              <div class="triage-result-advice">
                <span class="triage-result-advice-label">{{ labelText('triageAdviceLabel') }}</span>
                <p>{{ modal.item.advice }}</p>
              </div>
              <p class="triage-result-disclaimer">{{ modal.item.disclaimer }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'bird'">
            <figure v-if="modal.item.imageUrl" class="bird-result-preview">
              <img :src="modal.item.imageUrl" :alt="labelText('birdResult')" />
            </figure>
            <p v-if="modal.item.detected">{{ labelText('detectedParrot') }}（{{ labelText('confidence') }} {{ formatPercent(modal.item.parrotConfidence) }}）</p>
            <p v-else>{{ labelText('noParrot') }}</p>
            <p v-if="modal.item.species">{{ labelText('species') }}：<strong>{{ modal.item.species }}</strong>（{{ formatPercent(modal.item.speciesConfidence) }}）</p>
            <p v-if="modal.item.behavior">{{ labelText('behavior') }}：<strong>{{ modal.item.behavior }}</strong>（{{ labelText('confidence') }} {{ formatPercent(modal.item.confidence) }}）</p>
            <p v-else-if="modal.item.detected">{{ labelText('behaviorUnavailable') }}</p>
          </template>
          <template v-else-if="modal.type === 'risk'">
            <p>{{ modal.item.value }}</p>
          </template>
          <template v-else-if="modal.type === 'metric-gauge'">
            <div class="dust-gauge-panel">
              <div class="dust-gauge" :style="{ '--needle-angle': metricNeedleRotation(modal.item) }">
                <div class="gauge-scale" aria-hidden="true">
                  <span class="tick tick-low">{{ labelText('low') }}</span>
                  <span class="tick tick-mid">{{ labelText('mid') }}</span>
                  <span class="tick tick-high">{{ labelText('high') }}</span>
                </div>
                <span class="gauge-needle"></span>
                <span class="gauge-hub"></span>
              </div>
              <div class="dust-gauge-readout">
                <strong>{{ modal.item.displayValue || `${modal.item.value}${modal.item.unit}` }}</strong>
                <span>{{ text.currentLevel }}：{{ metricGaugeLevel(modal.item) }}</span>
                <em>{{ modal.item.connected ? text.connected : text.fallback }}</em>
              </div>
            </div>
          </template>
          <template v-else-if="modal.type === 'api-keys'">
            <div class="api-keys-form">
              <label>
                <span>{{ text.qwenApiKey }}</span>
                <input v-model="apiKeyDraft.qwenApiKey" type="password" placeholder="sk-..." autocomplete="off" />
              </label>
              <label>
                <span>{{ text.deepseekApiKey }}</span>
                <input v-model="apiKeyDraft.deepseekApiKey" type="password" placeholder="sk-..." autocomplete="off" />
              </label>
              <p v-if="apiKeyMessage" :class="apiKeyMessage === text.apiKeySaved ? 'api-key-success' : 'api-key-error'">{{ apiKeyMessage }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'qq-whitelist'">
            <div class="api-keys-form">
              <label>
                <span>{{ text.qqWhitelistLabel }}</span>
                <input v-model="qqWhitelistDraft" type="text" :placeholder="text.qqWhitelistPlaceholder" autocomplete="off" />
              </label>
              <p v-if="qqWhitelistMessage" :class="qqWhitelistMessage === text.qqWhitelistSaved ? 'api-key-success' : 'api-key-error'">{{ qqWhitelistMessage }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'settings-info'">
            <div class="settings-info-modal">
              <p v-for="line in modal.item.lines" :key="line">{{ line }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'ledger-create'">
            <div class="ledger-create-form">
              <label>
                <span>支出日期</span>
                <input v-model="ledgerDraft.time" type="date" :max="todayText" />
              </label>
              <label>
                <span>支出分类</span>
                <select v-model="ledgerDraft.tag">
                  <option v-for="category in LEDGER_CATEGORIES" :key="category" :value="category">{{ category }}</option>
                </select>
              </label>
              <label class="ledger-form-wide">
                <span>支出说明</span>
                <input v-model="ledgerDraft.description" placeholder="例如：主粮补充装" maxlength="255" />
              </label>
              <label class="ledger-form-wide">
                <span>金额</span>
                <span class="ledger-amount-field">
                  <b>¥</b>
                  <input v-model.number="ledgerDraft.amount" type="number" min="0.01" step="0.01" placeholder="0.00" />
                </span>
              </label>
              <p v-if="ledgerFormError" class="ledger-form-error" role="alert">{{ ledgerFormError }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'confirm-delete-account'">
            <div class="delete-account-modal">
              <p class="delete-account-username">{{ text.username }}：{{ modal.item.username }}</p>
              <p class="delete-account-warning">{{ modal.item.warning }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'confirm-delete-profile'">
            <div class="delete-account-modal">
              <p class="delete-account-username">{{ labelText('parrotName') }}：{{ modal.item.name }}</p>
              <p class="delete-account-warning">{{ modal.item.warning }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'confirm-delete-ledger'">
            <div class="ledger-delete-confirm">
              <div>
                <span>{{ normalizeLedgerCategory(modal.item.tag) }}</span>
                <strong>¥{{ formatLedgerAmount(modal.item.amount) }}</strong>
              </div>
              <p>{{ ledgerDescriptionText(modal.item) }}</p>
              <small>{{ modal.item.time }} · 删除后无法恢复</small>
            </div>
          </template>
          <template v-else-if="modal.type === 'weight-chart'">
            <div class="weight-chart-panel">
              <div class="weight-chart-meta">
                <strong>{{ modal.item.name }}</strong>
                <span>{{ labelText('weightAxis') }}</span>
              </div>
              <svg class="weight-detail-chart" viewBox="0 0 560 280" aria-label="体重变化折线图">
                <g class="chart-grid">
                  <line v-for="tick in weightChartTicks(modal.item.weightHistory || [])" :key="`wy-${tick.y}`" x1="72" :y1="tick.y" x2="536" :y2="tick.y" />
                  <line v-for="x in [72, 164, 256, 348, 440, 532]" :key="`wx-${x}`" :x1="x" y1="28" :x2="x" y2="232" />
                </g>
                <polyline :points="translatedWeightPoints(modal.item.weightHistory || [])" />
                <text
                  v-for="tick in weightChartTicks(modal.item.weightHistory || [])"
                  :key="`weight-tick-${tick.y}`"
                  class="weight-axis-tick"
                  x="64"
                  :y="tick.y + 4"
                  text-anchor="end"
                >{{ formatWeightValue(tick.value) }}g</text>
                <g
                  v-for="(point, index) in modal.item.weightHistory || []"
                  :key="`${modal.item.id}-weight-${point.time}`"
                  class="chart-point chart-point-large weight-chart-point"
                  tabindex="0"
                >
                  <title>{{ `${formatWeightValue(point.value)}g` }}</title>
                  <circle
                    :cx="weightPointPosition(modal.item.weightHistory || [], index, 'x')"
                    :cy="weightPointPosition(modal.item.weightHistory || [], index, 'y')"
                    r="6"
                  />
                  <text
                    class="chart-point-tooltip"
                    :x="weightPointPosition(modal.item.weightHistory || [], index, 'x')"
                    :y="weightPointPosition(modal.item.weightHistory || [], index, 'y') - 16"
                    text-anchor="middle"
                  >{{ formatWeightValue(point.value) }}g</text>
                </g>
                <text x="72" y="266">{{ labelText('editTime') }}</text>
                <text class="weight-axis-title" x="8" y="20">{{ labelText('grams') }}</text>
              </svg>
              <div class="weight-label-row">
                <span v-for="item in modal.item.weightHistory || []" :key="item.time">{{ item.time }}</span>
              </div>
            </div>
          </template>
          <template v-else-if="modal.type === 'report-date'">
            <div class="report-date-modal">
              <p class="report-date-hint">选择要查看的{{ reportPickerRange === '日报' ? '日期' : reportPickerRange === '周报' ? '周（选该周任意一天）' : '月（选该月任意一天）' }}：</p>
              <input class="report-date-input" type="date" v-model="reportPickerDate" :max="defaultReportDate(reportPickerRange)" />
              <div class="report-date-actions">
                <button type="button" @click="closeModal()">取消</button>
                <button type="button" class="primary" @click="confirmReportDate()">查看报告</button>
              </div>
            </div>
          </template>
          <template v-else-if="modal.type === 'curve'">
            <div class="detail-line-chart">
              <span class="axis-y">{{ modal.item.axis }} / {{ modal.item.unit }}</span>
              <div ref="echartsRef" class="echarts-container"></div>
              <span class="axis-x">{{ activeReportRange === '日报' ? labelText('hourlyTrend') : `${rangeText(activeReportRange)} ${labelText('trend')}` }}</span>
            </div>
          </template>
          <template v-else-if="modal.type === 'photo-preview'">
            <figure class="photo-preview">
              <img :src="photoSource(modal.item)" :alt="modal.item.title" />
              <figcaption>{{ modal.item.title }} · {{ modal.item.time }}</figcaption>
            </figure>
          </template>
          <template v-else-if="modal.type === 'pet-avatar-picker'">
            <div v-if="avatarSelectablePhotos.length" class="pet-avatar-picker">
              <button
                v-for="photo in avatarSelectablePhotos"
                :key="photo.mediaId"
                type="button"
                :class="{ active: petAvatarMediaMap[selectedArchive.id] === photo.mediaId }"
                @click="selectPetAvatarPhoto(photo)"
              >
                <img :src="photoSource(photo)" :alt="photo.title" />
                <span>{{ photo.title }}</span>
              </button>
            </div>
            <p v-else>当前宠物还没有可用的成长相册照片，暂时保留默认头像。</p>
          </template>
          <template v-else>
            <label>
              <span>{{ labelText('name') }}</span>
              <input :value="modal.item?.title || modal.item?.name || selectedParrot.shortName" />
            </label>
            <label>
              <span>{{ labelText('description') }}</span>
              <textarea :value="modal.item?.note || labelText('editPlaceholder')"></textarea>
            </label>
          </template>
        </div>
        <footer>
          <button type="button" class="ghost-button" @click="closeModal">{{ text.cancel || '取消' }}</button>
          <button v-if="modal.type === 'archive-create'" type="button" class="save-button" @click="saveNewProfile">{{ text.save }}</button>
          <button v-else-if="modal.type === 'archive-edit'" type="button" class="save-button" @click="saveProfileEdit">{{ text.save }}</button>
          <button v-else-if="modal.type === 'ledger-create'" type="button" class="save-button" :disabled="ledgerSaving" @click="addLedgerRecord">{{ ledgerSaving ? '保存中…' : '保存记录' }}</button>
          <button v-else-if="modal.type === 'photo-preview'" type="button" class="save-button" @click="downloadPhoto(modal.item)">{{ ui.savePhoto }}</button>
          <button v-else-if="modal.type === 'api-keys'" type="button" class="save-button" :disabled="apiKeySaving" @click="saveApiKeys">{{ apiKeySaving ? '保存中…' : text.save }}</button>
          <button v-else-if="modal.type === 'qq-whitelist'" type="button" class="save-button" :disabled="qqWhitelistSaving" @click="saveQqWhitelist">{{ qqWhitelistSaving ? '保存中…' : text.save }}</button>
          <button v-else-if="modal.type === 'confirm-delete-account'" type="button" class="save-button delete-account-confirm" @click="executeDeleteAccount">{{ text.deleteAccountConfirm }}</button>
          <button v-else-if="modal.type === 'confirm-delete-profile'" type="button" class="save-button delete-account-confirm" @click="executeDeleteProfile">{{ text.deleteProfileConfirm }}</button>
          <button v-else-if="modal.type === 'confirm-delete-ledger'" type="button" class="save-button ledger-delete-confirm-button" :disabled="ledgerDeleting" @click="executeDeleteLedger">{{ ledgerDeleting ? '删除中…' : '确认删除' }}</button>
          <button v-else type="button" class="save-button" @click="closeModal">{{ text.confirm }}</button>
        </footer>
      </section>
    </div>
    </Teleport>
  </main>
</template>
