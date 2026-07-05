<script setup>
import { computed, onMounted, ref } from 'vue'
import CurrentBirdCard from './components/CurrentBirdCard.vue'
import EntryCard from './components/EntryCard.vue'
import MonitorCard from './components/MonitorCard.vue'
import ParrotVisual from './components/ParrotVisual.vue'
import {
  archiveProfiles,
  currentParrot,
  detailViews,
  entryCards,
  foodCategories,
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
  tutorialCards,
  userProfile,
} from './data/mockDashboard'

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
const selectedParrot = ref(currentParrot)
const activeArchiveId = ref(archiveProfiles[0]?.id || '')
const activeReportRange = ref('月报')
const modal = ref(null)
const selectedHospital = ref(hospitalPins[0])
const diagnosisForm = ref({
  energy: '精神一般',
  appetite: '正常进食',
  breathing: '无异常',
  droppings: '正常',
})
const foodQuery = ref('')
const foodCategory = ref('水果')
const tutorialKeyword = ref('')
const birdKeyword = ref('')
const medicalRecordSearch = ref('')
const newMedicalRecord = ref('')
const ledgerKeyword = ref('')
const ledgerDraft = ref({
  time: '2026-07-04',
  tag: '日常用品',
  description: '',
  amount: '',
})
const editingMedicalId = ref('')
const editingMedicalText = ref('')
const editingLedgerId = ref('')
const editingLedgerDraft = ref(null)
const medicalRecords = ref([
  { id: 'm1', text: '2026-07-01 羽粉偏高，通风后恢复' },
  { id: 'm2', text: '2026-06-20 体重 77.5g，精神正常' },
  { id: 'm3', text: '2026-06-02 药浴后保温 2 小时' },
])
const ledgerRecords = ref([
  { id: 'l1', time: '2026-07-03', createdAt: '2026-07-03 09:18', updatedAt: '', tag: '主粮', description: '啾啾 · 主粮补充装', amount: 88 },
  { id: 'l2', time: '2026-07-01', createdAt: '2026-07-01 18:42', updatedAt: '', tag: '用品', description: '豆豆 · 磨爪站杆', amount: 36 },
  { id: 'l3', time: '2026-06-28', createdAt: '2026-06-28 10:07', updatedAt: '2026-06-29 11:30', tag: '医疗', description: '奶油 · 体检挂号', amount: 120 },
])
const profileForm = ref({
  species: '小太阳',
  name: '',
  birthday: '2024-05-18',
  weight: '',
  sex: '未知',
})
const account = ref({
  phone: '13823070420',
  email: 'wenderella@example.com',
  emailBound: true,
  ...userProfile,
})
const isSettingsEditing = ref(false)
const settingsDraft = ref({ ...account.value })
const phoneChanging = ref(false)
const phoneDraft = ref('')
const emailChanging = ref(false)
const emailDraft = ref('')
const weightDraft = ref('')
const capturedPhotos = ref([])
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
      handbook: ['饲养手册', '教程库、食物安全、拍照识鸟'],
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
    phone: '手机绑定',
    email: '绑定邮箱',
    bound: '已绑定',
    unbound: '未绑定',
    change: '更换',
    confirm: '确定',
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
  },
  en: {
    cards: {
      archive: ['Pet Profiles', 'Avatar, profile, weight and album'],
      growth: ['Growth Report', 'Daily, weekly, monthly health curves'],
      settings: ['User Settings', 'Avatar, account, location and permissions'],
      medical: ['Medical Helper', 'Triage, nearby hospitals and records'],
      ledger: ['Ledger', 'Track parrot-care expenses'],
      handbook: ['Care Handbook', 'Tutorials, food safety and bird ID'],
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
    phone: 'Phone',
    email: 'Email',
    bound: 'Bound',
    unbound: 'Hidden',
    change: 'Change',
    confirm: 'Confirm',
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
  },
  es: {
    cards: {
      archive: ['Perfiles', 'Avatar, datos, peso y álbum'],
      growth: ['Informe', 'Curvas diarias, semanales y mensuales'],
      settings: ['Ajustes', 'Avatar, cuenta, ubicación y permisos'],
      medical: ['Asistente médico', 'Consulta, hospitales y registros'],
      ledger: ['Gastos', 'Registra gastos de cuidado'],
      handbook: ['Manual', 'Tutoriales, alimentos e identificación'],
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
    phone: 'Teléfono',
    email: 'Correo',
    bound: 'Vinculado',
    unbound: 'Oculto',
    change: 'Cambiar',
    confirm: 'Confirmar',
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
  },
  ja: {
    cards: {
      archive: ['ペット記録', 'アバター、情報、体重、アルバム'],
      growth: ['成長レポート', '日報・週報・月報と健康曲線'],
      settings: ['ユーザー設定', 'アバター、アカウント、位置、権限'],
      medical: ['医療サポート', '問診、近くの病院、記録'],
      ledger: ['家計簿', '飼育費用を記録'],
      handbook: ['飼育ガイド', '教程、食品安全、鳥識別'],
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
    phone: '電話番号',
    email: 'メール',
    bound: '連携済み',
    unbound: '非表示',
    change: '変更',
    confirm: '確定',
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
  },
}

const activeView = computed(() => detailViews[activeRoute.value])
const reportCurveSet = computed(() => reportCurveSets[activeReportRange.value] || reportCurveSets.月报)
const reportCurves = computed(() => reportCurveSet.value.curves)
const text = computed(() => i18n[systemPrefs.value.language] || i18n.zh)
const languageClass = computed(() => `lang-${systemPrefs.value.language}`)
const themeClass = computed(() => (systemPrefs.value.theme === 'dark' ? 'night-theme' : 'day-theme'))
const settingsColorLabel = computed(() => (systemPrefs.value.theme === 'dark' ? '白色' : text.value.black))
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
  const match = Object.values(localizedEntryCards.value).find((card) => card.route === activeRoute.value)
  return match?.title || activeView.value.title
})
const reportRanges = computed(() => [
  { value: '日报', label: text.value.daily },
  { value: '周报', label: text.value.weekly },
  { value: '月报', label: text.value.monthly },
])
const selectedArchive = computed(() => {
  const id = thirdView.value.startsWith('archive:') ? thirdView.value.replace('archive:', '') : activeArchiveId.value
  return profiles.value.find((profile) => profile.id === id) || profiles.value[0]
})
const selectedAvatarParrot = computed(() => (
  localParrots.value.find((parrot) => parrot.id === account.value.avatarParrotId) || localParrots.value[0]
))
const profileFormAgeStage = computed(() => getAgeStage(profileForm.value.birthday))
const filteredTutorials = computed(() => {
  const keyword = tutorialKeyword.value.trim()
  if (!keyword) return tutorialCards
  return tutorialCards.filter((item) => `${item.title}${item.tag}`.includes(keyword))
})
const filteredMedicalRecords = computed(() => {
  const keyword = medicalRecordSearch.value.trim()
  if (!keyword) return medicalRecords.value
  return medicalRecords.value.filter((item) => item.text.includes(keyword))
})
const filteredLedgerRecords = computed(() => {
  const keyword = ledgerKeyword.value.trim()
  if (!keyword) return ledgerRecords.value
  return ledgerRecords.value.filter((item) => (
    `${item.time}${item.tag}${item.description}${item.amount}`.includes(keyword)
  ))
})
const ledgerTotal = computed(() => (
  ledgerRecords.value.reduce((total, item) => total + Number(item.amount || 0), 0)
))
const todayText = computed(() => new Date().toISOString().slice(0, 10))
const archivePhotoRecords = computed(() => [
  ...capturedPhotos.value,
  ...photoRecords,
])

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

function resetDetailState() {
  thirdView.value = ''
  petSwitchOpen.value = false
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

function openModal(type, title, item = null) {
  petSwitchOpen.value = false
  modal.value = { type, title, item }
}

function closeModal() {
  modal.value = null
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
  openModal('diagnosis', '可能的疾病 + 治疗建议', {
    summary: '可能为轻度呼吸道刺激或环境粉尘偏高',
    advice: '先通风 20 分钟、观察鼻孔和呼吸频率；若持续张口呼吸或精神萎靡，请尽快联系异宠医院。',
  })
}

function refreshHospitals() {
  const currentIndex = hospitalPins.findIndex((item) => item.id === selectedHospital.value.id)
  selectedHospital.value = hospitalPins[(currentIndex + 1) % hospitalPins.length]
}

function queryFood() {
  const name = foodQuery.value.trim() || '苹果'
  openModal('food', '食物查询结果', {
    name,
    category: foodCategory.value,
    family: foodCategory.value === '水果' ? '蔷薇科或常见浆果类' : '常见鹦鹉辅食类别',
    result: foodCategory.value === '肉类' ? '不建议作为日常食物' : '可少量食用',
    advice: '首次喂食请少量尝试，避开盐、糖、油和调味料。',
  })
}

function openCurve(curve) {
  if (isReportGaugeCurve(curve)) {
    openMetricGauge(curveToMetric(curve))
    return
  }
  openModal('curve', curve.label, { ...curve, xAxis: reportCurveSet.value.xAxis })
}

function openDustGauge(snapshot) {
  openMetricGauge({
    metric: snapshot.metric || 'dust',
    label: snapshot.label || '粉尘浓度',
    value: snapshot.value ?? snapshot.dustValue,
    displayValue: snapshot.displayValue || `${snapshot.dustValue}${snapshot.dustUnit || ''}`,
    unit: snapshot.unit || snapshot.dustUnit || 'μg/m³',
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

function isMetricCurve(curve) {
  return Boolean(metricCurveKind(curve))
}

function isReportGaugeCurve(curve) {
  return activeReportRange.value === '月报' && isMetricCurve(curve)
}

function metricCurveKind(curve) {
  const text = `${curve.label || ''}${curve.axis || ''}${curve.unit || ''}`
  if (text.includes('温') || text.includes('娓') || text.includes('℃') || text.includes('掳C')) return 'temperature'
  if (text.includes('湿') || text.includes('婀') || curve.unit === '%') return 'humidity'
  if (text.includes('粉') || text.includes('尘') || text.includes('绮') || text.includes('μg') || text.includes('渭g') || text.includes('/m')) return 'dust'
  return ''
}

function curveToMetric(curve) {
  const latest = curve.points?.[curve.points.length - 1] ?? Number.parseFloat(curve.value)
  const kind = metricCurveKind(curve)
  if (kind === 'temperature') {
    return {
      metric: 'temperature',
      label: '温度',
      value: latest,
      displayValue: `${latest}${curve.unit || '℃'}`,
      unit: curve.unit || '℃',
      level: latest < 18 ? '偏低' : latest > 30 ? '偏高' : '适宜',
      gaugeMax: 45,
      connected: false,
    }
  }
  if (kind === 'humidity') {
    return {
      metric: 'humidity',
      label: '湿度',
      value: latest,
      displayValue: `${latest}${curve.unit || '%'}`,
      unit: curve.unit || '%',
      level: latest < 40 ? '偏低' : latest > 70 ? '偏高' : '适宜',
      gaugeMax: 100,
      connected: false,
    }
  }
  return {
    metric: 'dust',
    label: '粉尘浓度',
    value: latest,
    displayValue: `${latest}${curve.unit || 'μg/m³'}`,
    unit: curve.unit || 'μg/m³',
    level: latest >= 80 ? '高' : latest >= 35 ? '中' : '低',
    gaugeMax: 120,
    connected: false,
  }
}

function openMetricGauge(item) {
  openModal('metric-gauge', `${item.label}仪表盘`, item)
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

function handleSnapshotCaptured(snapshot) {
  const title = `监控截图 ${formatShotTime(snapshot.savedAt).slice(5)}`
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
      title: `监控截图 ${formatShotTime(snapshot.savedAt).slice(5)}`,
      time: formatShotTime(snapshot.savedAt),
    }))
  } catch {
    capturedPhotos.value = []
  }
})

function openArchiveProfile(profile) {
  activeArchiveId.value = profile.id
  weightDraft.value = String(parseWeight(profile.weight) || '')
  openThird(`archive:${profile.id}`)
}

function openWeightChart() {
  openModal('weight-chart', '体重记录曲线', selectedArchive.value)
}

function saveArchiveWeight() {
  const number = Number(sanitizeWeight(weightDraft.value))
  if (!Number.isFinite(number) || number <= 0) return
  const archive = selectedArchive.value
  if (!archive) return

  const dateText = todayText.value
  const shortDate = dateText.slice(5)
  const entry = { time: shortDate, value: number }
  const history = Array.isArray(archive.weightHistory) ? archive.weightHistory : []
  const existingIndex = history.findIndex((item) => item.time === shortDate)
  if (existingIndex >= 0) history.splice(existingIndex, 1, entry)
  else history.push(entry)

  archive.weightHistory = history.slice(-12)
  archive.weight = `${number}g`
  archive.lastWeight = `${dateText} 录入 ${number}g`

  const parrot = localParrots.value.find((item) => item.id === archive.id)
  if (parrot) parrot.weight = archive.weight
  if (selectedParrot.value.id === archive.id) {
    selectedParrot.value = { ...selectedParrot.value, weight: archive.weight }
  }
  weightDraft.value = String(number)
  openModal('archive', '体重已保存', { name: archive.name, note: archive.lastWeight })
}

function weightHistoryPoints(history = [], width = 520, height = 220) {
  return linePoints(history.map((item) => item.value), width, height)
}

function translatedWeightPoints(history = []) {
  return weightHistoryPoints(history, 494, 212)
    .split(' ')
    .map((pair) => {
      const [x, y] = pair.split(',').map(Number)
      return `${x + 42},${y + 28}`
    })
    .join(' ')
}

function weightPointPosition(history = [], index, axis) {
  const pair = translatedWeightPoints(history).split(' ')[index] || '42,240'
  const [x, y] = pair.split(',').map(Number)
  return axis === 'x' ? x : y
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

function linePoints(points, width = 260, height = 92) {
  const values = points.map(Number)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  return values.map((value, index) => {
    const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width
    const y = height - ((value - min) / range) * (height - 16) - 8
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

function addMedicalRecord() {
  const content = newMedicalRecord.value.trim()
  if (!content) return
  medicalRecords.value.unshift({ id: `m-${Date.now()}`, text: `2026-07-03 ${content}` })
  newMedicalRecord.value = ''
}

function startEditMedical(record) {
  editingMedicalId.value = record.id
  editingMedicalText.value = record.text
}

function saveMedicalRecord(record) {
  const content = editingMedicalText.value.trim()
  if (!content) return
  record.text = content
  editingMedicalId.value = ''
  editingMedicalText.value = ''
}

function addLedgerRecord() {
  const description = ledgerDraft.value.description.trim()
  const amount = Number(ledgerDraft.value.amount)
  if (!description || !Number.isFinite(amount) || amount <= 0) return
  ledgerRecords.value.unshift({
    id: `l-${Date.now()}`,
    time: ledgerDraft.value.time || todayText.value,
    createdAt: formatStamp(),
    updatedAt: '',
    tag: ledgerDraft.value.tag || '其他',
    description: `${selectedParrot.value.shortName} · ${description}`,
    amount,
  })
  ledgerDraft.value = {
    time: todayText.value,
    tag: '日常用品',
    description: '',
    amount: '',
  }
}

function startEditLedger(record) {
  editingLedgerId.value = record.id
  editingLedgerDraft.value = { ...record }
}

function saveLedgerRecord(record) {
  if (!editingLedgerDraft.value) return
  const description = editingLedgerDraft.value.description.trim()
  const amount = Number(editingLedgerDraft.value.amount)
  if (!description || !Number.isFinite(amount) || amount <= 0) return
  Object.assign(record, {
    time: editingLedgerDraft.value.time || todayText.value,
    updatedAt: formatStamp(),
    tag: editingLedgerDraft.value.tag || '其他',
    description,
    amount,
  })
  editingLedgerId.value = ''
  editingLedgerDraft.value = null
}

function openCreateProfile() {
  profileForm.value = {
    species: '小太阳',
    name: '',
    birthday: '2024-05-18',
    weight: '',
    sex: '未知',
  }
  openModal('archive-create', '新增鹦鹉档案')
}

function saveNewProfile() {
  const name = profileForm.value.name.trim() || `新鹦鹉${localParrots.value.length + 1}`
  const weight = profileForm.value.weight.trim() || '未录入'
  const ageStage = getAgeStage(profileForm.value.birthday)
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

function toggleSettingsEdit() {
  if (isSettingsEditing.value) {
    account.value = { ...account.value, ...settingsDraft.value }
    isSettingsEditing.value = false
    phoneChanging.value = false
    emailChanging.value = false
    return
  }
  settingsDraft.value = { ...account.value }
  phoneDraft.value = sanitizeDigits(account.value.phone || '')
  emailDraft.value = account.value.email || ''
  phoneChanging.value = false
  emailChanging.value = false
  isSettingsEditing.value = true
}

function sanitizeDigits(value) {
  return String(value || '').replace(/\D/g, '').slice(0, 11)
}

function updatePhoneDraft(value) {
  phoneDraft.value = sanitizeDigits(value)
}

function startPhoneChange() {
  if (!isSettingsEditing.value) return
  phoneDraft.value = sanitizeDigits(settingsDraft.value.phone || '')
  phoneChanging.value = true
}

function confirmPhoneChange() {
  const phone = sanitizeDigits(phoneDraft.value)
  if (!/^\d{11}$/.test(phone)) return
  settingsDraft.value.phone = phone
  settingsDraft.value.phoneBound = true
  phoneChanging.value = false
}

function startEmailChange() {
  if (!isSettingsEditing.value) return
  emailDraft.value = settingsDraft.value.email || ''
  emailChanging.value = true
}

function confirmEmailChange() {
  const email = emailDraft.value.trim()
  if (!email || !email.includes('@')) return
  settingsDraft.value.email = email
  settingsDraft.value.emailBound = true
  emailChanging.value = false
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
  <main
    class="app-shell"
    :class="[themeClass, languageClass]"
    :style="{ '--user-font-size': `${systemPrefs.fontSize}px` }"
  >
    <section v-if="!activeView" class="dashboard" aria-label="基于智慧烟感的宠物安全系统首页">
      <div class="column left-column">
        <EntryCard :card="localizedEntryCards.archive" size="archive" @open="handleOpen" />
        <EntryCard :card="localizedEntryCards.growth" size="growth" @open="handleOpen" />
      </div>

      <div class="column center-column">
        <div class="current-zone">
          <CurrentBirdCard :parrot="selectedParrot" :label="text.currentParrot" @open="togglePetSwitch" />
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
                <ParrotVisual :type="parrot.avatarType" />
              </span>
              <span>
                <strong>{{ parrot.name }}</strong>
                <em>{{ parrot.species }} · {{ parrot.weight }} · {{ parrot.status }}</em>
              </span>
            </button>
          </section>
        </div>
<MonitorCard
  :card="localizedPrimaryCards.monitor"
  :device-id="selectedParrot.deviceId"
  :parrot-id="selectedParrot.id"
  @open="handleOpen"
  @dust-detail="openDustDetail"
  @metric-update="handleMetricUpdate"
  @snapshot-captured="handleSnapshotCaptured"
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
      :aria-label="`${activeView.title}界面`"
    >
      <header class="detail-header">
        <button class="back-button" type="button" aria-label="返回" @click="goBack">
          <span aria-hidden="true"></span>
        </button>
        <div class="detail-title-block">
          <h1>{{ localizedActiveTitle }}</h1>
        </div>
        <div class="detail-avatar">
          <ParrotVisual :type="selectedParrot.avatarType" />
        </div>
      </header>

      <template v-if="activeView.kind === 'report'">
        <section v-if="!thirdView" class="report-page">
          <div class="report-toolbar clean-report-toolbar">
            <div class="range-tabs">
              <button
                v-for="range in reportRanges"
                :key="range.value"
                :class="{ active: activeReportRange === range.value }"
                type="button"
                @click="activeReportRange = range.value"
              >
                {{ range.label }}
              </button>
            </div>
            <div class="report-parrot-switch">
              <button class="parrot-switch-button" type="button" @click="togglePetSwitch">
                {{ selectedParrot.shortName }}
                <span aria-hidden="true"></span>
              </button>
              <section v-if="petSwitchOpen" class="report-pet-panel" aria-label="报告鹦鹉切换">
                <button v-for="parrot in localParrots" :key="parrot.id" type="button" @click="selectParrot(parrot)">
                  {{ parrot.shortName }} · {{ parrot.species }}
                </button>
              </section>
            </div>
          </div>

          <section class="report-stat-grid" aria-label="报告关键指标">
            <article v-for="stat in reportStats" :key="stat.label" class="highlight-card">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ activeReportRange }}变化：{{ stat.trend }}</p>
            </article>
          </section>

          <section class="curve-grid" aria-label="曲线区域">
            <button
              v-for="curve in reportCurves"
              :key="curve.label"
              class="curve-card curve-button"
              :class="{ 'metric-gauge-card': isReportGaugeCurve(curve) }"
              type="button"
              @click="openCurve(curve)"
            >
              <header>
                <h2>{{ curve.label }}</h2>
                <strong>{{ curve.value }}</strong>
              </header>
              <span v-if="isReportGaugeCurve(curve)" class="inline-gauge-arc report-gauge-arc" aria-hidden="true">
                <i :style="{ transform: metricNeedleRotation(curveToMetric(curve)) }"></i>
              </span>
              <em v-if="isReportGaugeCurve(curve)" class="metric-gauge-name">{{ text.gaugeHint }}</em>
              <svg v-else class="mini-line-chart" viewBox="0 0 260 92" aria-hidden="true">
                <polyline :points="linePoints(curve.points)" />
                <circle
                  v-for="(point, index) in curve.points"
                  :key="`${curve.label}-${index}`"
                  :cx="linePoints(curve.points).split(' ')[index].split(',')[0]"
                  :cy="linePoints(curve.points).split(' ')[index].split(',')[1]"
                  r="4"
                />
              </svg>
            </button>
          </section>

          <section class="record-grid" aria-label="照片和录音记录">
            <button
              v-for="record in reportRecords"
              :key="record.type"
              class="module-card compact report-record-card"
              type="button"
              @click="record.action === 'risk' ? openModal('risk', record.type, record) : openThird(`report-${record.action}`)"
            >
              <h2>{{ record.type }}</h2>
              <p>{{ record.value }}</p>
            </button>
          </section>
        </section>

        <section v-else-if="thirdView === 'report-photos'" class="third-page gallery-page">
          <article v-for="photo in archivePhotoRecords" :key="photo.id || photo.title" class="photo-record-card">
            <span v-if="photo.image" class="photo-thumb" :style="{ backgroundImage: `url(${photo.image})` }" aria-hidden="true"></span>
            <span v-else aria-hidden="true"></span>
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
          <div class="archive-actions">
            <button type="button" @click="openCreateProfile">增加档案</button>
          </div>
          <button
            v-for="profile in profiles"
            :key="profile.id"
            class="profile-card"
            type="button"
            @click="openArchiveProfile(profile)"
          >
            <span class="profile-avatar"><ParrotVisual :type="profile.avatarType || 'avatar-orange'" /></span>
            <span class="profile-age">{{ profile.ageStage }}</span>
            <strong>{{ profile.name }}</strong>
            <em>{{ profile.species }} · 出生 {{ profile.birthday }} · {{ profile.weight }} · {{ profile.sex }}</em>
          </button>
        </section>

        <section v-else-if="thirdView === 'archive-gallery'" class="third-page archive-gallery-page">
          <article v-for="photo in archivePhotoRecords" :key="`archive-${photo.id || photo.title}`" class="archive-photo-tile">
            <span v-if="photo.image" class="photo-thumb" :style="{ backgroundImage: `url(${photo.image})` }" aria-hidden="true"></span>
            <span v-else aria-hidden="true"></span>
            <strong>{{ photo.title }}</strong>
            <em>{{ selectedArchive.name }} · {{ photo.time }}</em>
          </article>
        </section>

        <section v-else class="third-page archive-third">
          <article class="profile-card profile-card-large">
            <span class="profile-avatar"><ParrotVisual :type="selectedArchive.avatarType || 'avatar-orange'" /></span>
            <span class="profile-age">{{ selectedArchive.ageStage }}</span>
            <strong>{{ selectedArchive.name }}</strong>
            <em>{{ selectedArchive.species }} · 出生 {{ selectedArchive.birthday }} · {{ selectedArchive.weight }} · {{ selectedArchive.sex }} · {{ selectedArchive.status }}</em>
            <button type="button" @click="openModal('archive', '编辑基本资料', selectedArchive)">编辑</button>
          </article>
          <button class="module-card archive-action-module" type="button" @click="openWeightChart">
            <h2>体重记录</h2>
            <p>{{ selectedArchive.lastWeight }}</p>
            <div class="large-line-chart" aria-hidden="true">
              <i
                v-for="(point, index) in normalizedWeightBars(selectedArchive.weightHistory || [])"
                :key="`${selectedArchive.id}-weight-bar-${index}`"
                :style="{ height: `${point}%` }"
              ></i>
            </div>
          </button>
          <button class="module-card archive-action-module" type="button" @click="openThird('archive-gallery')">
            <h2>成长相册</h2>
            <p>{{ selectedArchive.photos }}，截图和睡眠照片会自动归档。</p>
            <div class="photo-strip" aria-hidden="true"><span></span><span></span><span></span></div>
          </button>
          <article class="module-card weight-input-card">
            <h2>录入体重</h2>
            <label class="weight-number-field">
              <span>今日体重</span>
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
            <button type="button" @click="saveArchiveWeight">保存</button>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'medical'">
        <section v-if="!thirdView" class="module-only-grid">
          <button v-for="module in medicalModules" :key="module.key" class="module-card action-card" type="button" @click="openThird(module.key)">
            <h2>{{ module.title }}</h2>
            <p>{{ module.note }}</p>
          </button>
        </section>

        <section v-else-if="thirdView === 'diagnosis'" class="third-page form-page">
          <article class="questionnaire-card">
            <h2>外在表现问卷</h2>
            <label><span>精神状态</span><select v-model="diagnosisForm.energy"><option>精神很好</option><option>精神一般</option><option>明显萎靡</option></select></label>
            <label><span>进食情况</span><select v-model="diagnosisForm.appetite"><option>正常进食</option><option>食量下降</option><option>拒食</option></select></label>
            <label><span>呼吸表现</span><select v-model="diagnosisForm.breathing"><option>无异常</option><option>偶尔张口</option><option>持续张口呼吸</option></select></label>
            <label><span>排泄情况</span><select v-model="diagnosisForm.droppings"><option>正常</option><option>偏稀</option><option>颜色异常</option></select></label>
            <button type="button" @click="submitDiagnosis">提交</button>
          </article>
        </section>

        <section v-else-if="thirdView === 'hospitals'" class="third-page map-page">
          <article class="map-card">
            <div class="map-canvas" aria-label="附近医院地图">
              <span class="self-pin">我的位置</span>
              <button
                v-for="hospital in hospitalPins"
                :key="hospital.id"
                class="hospital-pin"
                :class="{ active: selectedHospital.id === hospital.id }"
                type="button"
                :style="{ left: `${hospital.x}%`, top: `${hospital.y}%` }"
                @click="selectedHospital = hospital"
              ></button>
            </div>
            <aside class="hospital-info">
              <h2>{{ selectedHospital.name }}</h2>
              <p>{{ selectedHospital.address }}</p>
              <p>{{ selectedHospital.phone }}</p>
            </aside>
            <button class="refresh-button" type="button" @click="refreshHospitals">刷新</button>
          </article>
        </section>

        <section v-else class="third-page records-page">
          <input v-model="medicalRecordSearch" class="search-input" placeholder="搜索病历关键字" />
          <div class="record-editor">
            <input v-model="newMedicalRecord" placeholder="填写一条新的病历记录" />
            <button type="button" @click="addMedicalRecord">新增</button>
          </div>
          <article v-for="record in filteredMedicalRecords" :key="record.id" class="memo-card editable-memo">
            <input v-if="editingMedicalId === record.id" v-model="editingMedicalText" />
            <span v-else>{{ record.text }}</span>
            <button v-if="editingMedicalId === record.id" type="button" @click="saveMedicalRecord(record)">保存</button>
            <button v-else type="button" @click="startEditMedical(record)">修改</button>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'handbook'">
        <section v-if="!thirdView" class="module-only-grid">
          <button v-for="module in handbookModules" :key="module.key" class="module-card action-card" type="button" @click="openThird(module.key)">
            <h2>{{ module.title }}</h2>
            <p>{{ module.note }}</p>
          </button>
        </section>

        <section v-else-if="thirdView === 'food'" class="third-page form-page">
          <article class="questionnaire-card">
            <h2>食物查询</h2>
            <label><span>食物名称</span><input v-model="foodQuery" placeholder="例如：苹果" /></label>
            <label><span>食物种类</span><select v-model="foodCategory"><option v-for="category in foodCategories" :key="category">{{ category }}</option></select></label>
            <button type="button" @click="queryFood">查询</button>
          </article>
        </section>

        <section v-else-if="thirdView === 'tutorials'" class="third-page records-page">
          <input v-model="tutorialKeyword" class="search-input" placeholder="搜索教程关键字" />
          <article v-for="tutorial in filteredTutorials" :key="tutorial.title" class="memo-card">
            <strong>{{ tutorial.title }}</strong>
            <span>{{ tutorial.tag }} · {{ tutorial.minutes }}</span>
          </article>
        </section>

        <section v-else class="third-page form-page">
          <article class="questionnaire-card">
            <h2>拍照识鸟</h2>
            <label><span>图片或线索</span><input v-model="birdKeyword" placeholder="例如：绿色身体、红色尾羽" /></label>
            <button type="button" @click="openModal('bird', '识鸟结果', { name: birdKeyword || '虎皮鹦鹉', advice: '可能为常见小型鹦鹉；大型鹦鹉也可在此功能中识别并展示介绍。' })">识别</button>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'ledger'">
        <section class="third-page records-page ledger-page">
          <header class="ledger-summary-card">
            <span>总开销</span>
            <strong>¥{{ ledgerTotal }}</strong>
          </header>
          <input v-model="ledgerKeyword" class="search-input" placeholder="搜索消费记录" />
          <div class="record-editor">
            <input v-model="ledgerDraft.time" type="date" :max="todayText" />
            <input v-model="ledgerDraft.tag" placeholder="标签：主粮/医疗/用品" />
            <input v-model="ledgerDraft.description" placeholder="描述：玩具铃铛" />
            <input v-model.number="ledgerDraft.amount" type="number" min="0" step="0.01" placeholder="金额：29" />
            <button type="button" @click="addLedgerRecord">新增</button>
          </div>
          <div class="ledger-table-head" aria-hidden="true">
            <span>日期</span>
            <span>创建时间</span>
            <span>属性</span>
            <span>描述</span>
            <span>金额</span>
            <span>更新时间</span>
            <span>操作</span>
          </div>
          <article v-for="record in filteredLedgerRecords" :key="record.id" class="ledger-record-card">
            <template v-if="editingLedgerId === record.id && editingLedgerDraft">
              <input v-model="editingLedgerDraft.time" type="date" :max="todayText" />
              <input v-model="editingLedgerDraft.tag" />
              <input v-model="editingLedgerDraft.description" />
              <input v-model.number="editingLedgerDraft.amount" type="number" min="0" step="0.01" />
              <button type="button" @click="saveLedgerRecord(record)">保存</button>
            </template>
            <template v-else>
              <span>{{ record.time }}</span>
              <small>创建 {{ record.createdAt }}</small>
              <strong>{{ record.tag }}</strong>
              <p>{{ record.description }}</p>
              <em>¥{{ record.amount }}</em>
              <i :class="{ empty: !record.updatedAt }">{{ record.updatedAt ? `更新 ${record.updatedAt}` : '未编辑' }}</i>
              <button type="button" @click="startEditLedger(record)">编辑</button>
            </template>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'settings'">
        <section class="settings-page settings-system-page">
          <article class="settings-profile-card">
            <button class="settings-edit-button" type="button" @click="toggleSettingsEdit">
              {{ isSettingsEditing ? text.save : text.edit }}
            </button>
            <div class="settings-avatar-wrap">
              <span class="settings-avatar">
                <ParrotVisual :type="selectedAvatarParrot.avatarType" />
              </span>
              <select v-if="isSettingsEditing" v-model="settingsDraft.avatarParrotId" aria-label="选择头像鹦鹉">
                <option v-for="parrot in localParrots" :key="parrot.id" :value="parrot.id">{{ parrot.name }}</option>
              </select>
            </div>
            <label class="settings-name-row">
              <span>{{ text.username }}</span>
              <input v-if="isSettingsEditing" v-model="settingsDraft.username" />
              <strong v-else>{{ account.username }}</strong>
            </label>
            <p class="settings-user-id">{{ text.userId }}：{{ account.userId }}</p>
            <p class="settings-location">{{ text.location }}：{{ account.location }}</p>
            <div class="settings-phone-row">
              <span>{{ text.phone }}</span>
              <strong v-if="!isSettingsEditing">{{ account.phoneBound ? account.phone : text.unbound }}</strong>
              <template v-else-if="!phoneChanging">
                <strong v-if="settingsDraft.phoneBound">{{ settingsDraft.phone }}</strong>
                <strong v-else>{{ text.unbound }}</strong>
                <button type="button" @click="startPhoneChange">{{ text.change }}</button>
              </template>
              <template v-else>
                <input :value="phoneDraft" inputmode="numeric" maxlength="11" :placeholder="text.inputPhone" @input="updatePhoneDraft($event.target.value)" />
                <button type="button" @click="confirmPhoneChange">{{ text.confirm }}</button>
              </template>
            </div>
            <div class="settings-phone-row settings-email-row">
              <span>{{ text.email }}</span>
              <strong v-if="!isSettingsEditing">{{ account.emailBound ? account.email : text.unbound }}</strong>
              <template v-else-if="!emailChanging">
                <strong v-if="settingsDraft.emailBound">{{ settingsDraft.email }}</strong>
                <strong v-else>{{ text.unbound }}</strong>
                <button type="button" @click="startEmailChange">{{ text.change }}</button>
              </template>
              <template v-else>
                <input v-model="emailDraft" type="email" :placeholder="text.inputEmail" />
                <button type="button" @click="confirmEmailChange">{{ text.confirm }}</button>
              </template>
            </div>
          </article>
          <section class="settings-system-card" aria-label="系统设置">
            <article class="settings-option-row">
              <span>{{ text.language }}</span>
              <div class="settings-segmented">
                <button type="button" :class="{ active: systemPrefs.language === 'zh' }" @click="systemPrefs.language = 'zh'">{{ text.chinese }}</button>
                <button type="button" :class="{ active: systemPrefs.language === 'en' }" @click="systemPrefs.language = 'en'">{{ text.english }}</button>
                <button type="button" :class="{ active: systemPrefs.language === 'es' }" @click="systemPrefs.language = 'es'">{{ text.spanish }}</button>
                <button type="button" :class="{ active: systemPrefs.language === 'ja' }" @click="systemPrefs.language = 'ja'">{{ text.japanese }}</button>
              </div>
            </article>
            <article class="settings-option-row">
              <span>{{ text.theme }}</span>
              <div class="settings-segmented">
                <button type="button" :class="{ active: systemPrefs.theme === 'light' }" @click="systemPrefs.theme = 'light'">{{ text.day }}</button>
                <button type="button" :class="{ active: systemPrefs.theme === 'dark' }" @click="systemPrefs.theme = 'dark'">{{ text.night }}</button>
              </div>
            </article>
            <article class="settings-option-row">
              <span>{{ text.font }}</span>
              <strong>{{ text.defaultFont }}</strong>
            </article>
            <article class="settings-option-row">
              <span>{{ text.fontSize }}</span>
              <input v-model.number="systemPrefs.fontSize" type="range" min="12" max="28" step="1" />
              <strong>{{ systemPrefs.fontSize }}pt</strong>
            </article>
            <article class="settings-option-row">
              <span>{{ text.color }}</span>
              <strong>{{ settingsColorLabel }}</strong>
            </article>
            <button class="settings-info-button" type="button" @click="openModal('setting-toggles', text.permissions)">{{ text.permissions }}</button>
            <button class="settings-info-button" type="button" @click="openSettingsInfo('about')">{{ text.about }}</button>
            <button class="settings-info-button" type="button" @click="openSettingsInfo('system')">{{ text.system }}</button>
            <button class="settings-info-button" type="button" @click="openSettingsInfo('version')">{{ text.version }}</button>
          </section>
        </section>
      </template>
    </section>

    <div v-if="modal" class="modal-backdrop" role="presentation" @click.self="closeModal">
      <section class="edit-modal" :class="`modal-${modal.type}`" role="dialog" aria-modal="true" :aria-label="modal.title">
        <header>
          <h2>{{ modal.title }}</h2>
          <button type="button" aria-label="关闭弹窗" @click="closeModal">×</button>
        </header>
        <div class="modal-body">
          <template v-if="modal.type === 'archive-create'">
            <label>
              <span>鹦鹉种类</span>
              <select v-model="profileForm.species">
                <option v-for="species in parrotSpeciesOptions" :key="species">{{ species }}</option>
              </select>
            </label>
            <label><span>鹦鹉名字</span><input v-model="profileForm.name" placeholder="例如：啾啾" /></label>
            <label><span>出生日期</span><input v-model="profileForm.birthday" placeholder="xxxx-xx-xx" /></label>
            <label><span>年龄标识</span><input :value="profileFormAgeStage" readonly /></label>
            <label><span>当前体重</span><input v-model="profileForm.weight" placeholder="例如：78g" /></label>
          </template>
          <template v-else-if="modal.type === 'setting-toggles'">
            <div class="setting-toggle-row">
              <span>通知设置</span>
              <button type="button" :class="{ active: notificationEnabled }" @click="notificationEnabled = !notificationEnabled"></button>
            </div>
            <div class="setting-toggle-row">
              <span>设备权限</span>
              <button type="button" :class="{ active: permissionEnabled }" @click="permissionEnabled = !permissionEnabled"></button>
            </div>
          </template>
          <template v-else-if="modal.type === 'diagnosis'">
            <p><strong>{{ modal.item.summary }}</strong></p>
            <p>{{ modal.item.advice }}</p>
          </template>
          <template v-else-if="modal.type === 'food'">
            <p>{{ modal.item.name }} · {{ modal.item.category }} · {{ modal.item.family }}</p>
            <p>{{ modal.item.result }}。{{ modal.item.advice }}</p>
          </template>
          <template v-else-if="modal.type === 'bird'">
            <p>可能种类：{{ modal.item.name }}</p>
            <p>{{ modal.item.advice }}</p>
          </template>
          <template v-else-if="modal.type === 'risk'">
            <p>{{ modal.item.value }}</p>
          </template>
          <template v-else-if="modal.type === 'metric-gauge'">
            <div class="dust-gauge-panel">
              <div class="dust-gauge" :style="{ '--needle-angle': metricNeedleRotation(modal.item) }">
                <div class="gauge-scale" aria-hidden="true">
                  <span class="tick tick-low">低</span>
                  <span class="tick tick-mid">中</span>
                  <span class="tick tick-high">高</span>
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
          <template v-else-if="modal.type === 'settings-info'">
            <div class="settings-info-modal">
              <p v-for="line in modal.item.lines" :key="line">{{ line }}</p>
            </div>
          </template>
          <template v-else-if="modal.type === 'weight-chart'">
            <div class="weight-chart-panel">
              <div class="weight-chart-meta">
                <strong>{{ modal.item.name }}</strong>
                <span>体重 / g</span>
              </div>
              <svg class="weight-detail-chart" viewBox="0 0 560 280" aria-label="体重变化折线图">
                <g class="chart-grid">
                  <line v-for="y in [40, 90, 140, 190, 240]" :key="`wy-${y}`" x1="42" :y1="y" x2="536" :y2="y" />
                  <line v-for="x in [42, 140, 238, 336, 434, 532]" :key="`wx-${x}`" :x1="x" y1="28" :x2="x" y2="240" />
                </g>
                <polyline :points="translatedWeightPoints(modal.item.weightHistory || [])" />
                <circle
                  v-for="(point, index) in modal.item.weightHistory || []"
                  :key="`${modal.item.id}-weight-${point.time}`"
                  :cx="weightPointPosition(modal.item.weightHistory || [], index, 'x')"
                  :cy="weightPointPosition(modal.item.weightHistory || [], index, 'y')"
                  r="6"
                />
                <text x="42" y="266">编辑时间</text>
                <text x="6" y="36">克数</text>
              </svg>
              <div class="weight-label-row">
                <span v-for="item in modal.item.weightHistory || []" :key="item.time">{{ item.time }}</span>
              </div>
            </div>
          </template>
          <template v-else-if="modal.type === 'curve'">
            <div class="detail-line-chart">
              <span class="axis-y">{{ modal.item.axis }} / {{ modal.item.unit }}</span>
              <svg class="modal-line-chart" viewBox="0 0 520 260" aria-hidden="true">
                <polyline :points="linePoints(modal.item.points, 520, 220)" />
                <circle
                  v-for="(point, index) in modal.item.points"
                  :key="`${modal.item.label}-detail-${index}`"
                  :cx="linePoints(modal.item.points, 520, 220).split(' ')[index].split(',')[0]"
                  :cy="linePoints(modal.item.points, 520, 220).split(' ')[index].split(',')[1]"
                  r="6"
                />
              </svg>
              <div class="chart-label-row">
                <span v-for="label in modal.item.xAxis" :key="label">{{ label }}</span>
              </div>
              <span class="axis-x">{{ activeReportRange === '日报' ? '小时趋势' : `${activeReportRange}趋势` }}</span>
            </div>
          </template>
          <template v-else>
            <label>
              <span>名称</span>
              <input :value="modal.item?.title || modal.item?.name || selectedParrot.shortName" />
            </label>
            <label>
              <span>说明</span>
              <textarea :value="modal.item?.note || '这里填写需要修改的信息。'"></textarea>
            </label>
          </template>
        </div>
        <footer>
          <button type="button" class="ghost-button" @click="closeModal">取消</button>
          <button v-if="modal.type === 'archive-create'" type="button" class="save-button" @click="saveNewProfile">保存</button>
          <button v-else type="button" class="save-button" @click="closeModal">确定</button>
        </footer>
      </section>
    </div>
  </main>
</template>
