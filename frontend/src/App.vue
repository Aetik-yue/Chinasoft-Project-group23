<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import CurrentBirdCard from './components/CurrentBirdCard.vue'
import EntryCard from './components/EntryCard.vue'
import MonitorCard from './components/MonitorCard.vue'
import ParrotVisual from './components/ParrotVisual.vue'
import { recognizeParrotBehavior } from './api/parrot'
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
const monitorFullscreen = ref(false)
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
const birdImage = ref(null)
const birdImagePreview = ref('')
const birdLoading = ref(false)
const birdError = ref('')
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
  { id: 'l1', time: '2026-07-03', createdAt: '2026-07-03 09:18', updatedAt: '', tag: '主粮', description: '老爹 · 主粮补充装', amount: 88 },
  { id: 'l2', time: '2026-07-01', createdAt: '2026-07-01 18:42', updatedAt: '', tag: '用品', description: '刀哥 · 磨爪站杆', amount: 36 },
  { id: 'l3', time: '2026-06-28', createdAt: '2026-06-28 10:07', updatedAt: '2026-06-29 11:30', tag: '医疗', description: '农药 · 体检挂号', amount: 120 },
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
const basePhotoRecords = ref([...photoRecords])
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
      tutorials: ['教程库', '新手喂养、剪羽、药浴、清洁教程'],
      food: ['食物安全', '输入食物名称查询是否适合鹦鹉'],
      'bird-id': ['拍照识鹦鹉', '上传或拍照识别种类与行为'],
    },
    reportStats: ['健康评分', '睡眠时长', '鸣叫次数', '进食次数', '排泄次数'],
    reportRecords: {
      photos: ['照片记录', '最兴奋照片 4 张，睡觉照片 6 张'],
      recordings: ['录音', '学舌 5 段，歌曲练习 3 次'],
      risk: ['健康风险提醒', '下午羽粉偏高，建议通风 20 分钟'],
    },
    tutorials: [
      ['新手到家 7 天照护', '新手喂养', '8 分钟'],
      ['安全剪羽与替代训练', '剪羽教程', '12 分钟'],
      ['药浴前后的保温要点', '药浴教程', '6 分钟'],
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
      foodQuery: '食物查询', foodName: '食物名称', foodCategory: '食物种类', foodPlaceholder: '例如：苹果', query: '查询',
      tutorialSearch: '搜索教程关键字', birdTitle: '鹦鹉识别（种类+行为）', choosePhoto: '选择 / 拍照',
      chooseFile: '选择文件', noFile: '未选择文件', recognize: '识别行为', recognizing: '识别中…',
      birdAlt: '待识别鹦鹉', birdResult: '识别结果', chooseBirdFirst: '请先选择或拍摄一张鹦鹉图片',
      recognizeFail: '识别失败', detectedParrot: '检测到鹦鹉', noParrot: '未检测到鹦鹉',
      species: '种类', behavior: '行为', confidence: '置信度', behaviorUnavailable: '行为识别未启用或未出结果',
      foodResult: '食物查询结果', foodFamilyFruit: '蔷薇科或常见浆果类', foodFamilyCommon: '常见鹦鹉辅食类别',
      foodUnsafe: '不建议作为日常食物', foodSafe: '可少量食用', foodAdvice: '首次喂食请少量尝试，避开盐、糖、油和调味料。',
      submit: '提交', refresh: '刷新', searchRecord: '搜索病历关键字', newRecord: '填写一条新的病历记录',
      add: '新增', modify: '修改', playRecording: '播放录音',
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
      tutorials: ['Tutorial Library', 'Beginner care, trimming, bath and cleaning guides'],
      food: ['Food Safety', 'Check whether a food is suitable for parrots'],
      'bird-id': ['Bird ID', 'Upload or take a photo to identify species'],
    },
    reportStats: ['Health Score', 'Sleep Duration', 'Calls', 'Meals', 'Droppings'],
    reportRecords: {
      photos: ['Photo Records', '4 excited photos, 6 sleep photos'],
      recordings: ['Recordings', '5 mimicry clips, 3 song practices'],
      risk: ['Health Risk Alert', 'Dust is high this afternoon; ventilate for 20 minutes'],
    },
    tutorials: [
      ['First 7 Days at Home', 'Beginner care', '8 min'],
      ['Safe Wing Trimming Alternatives', 'Training guide', '12 min'],
      ['Warmth Before and After Medicated Bath', 'Bath care', '6 min'],
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
      foodQuery: 'Food Search', foodName: 'Food name', foodCategory: 'Food category', foodPlaceholder: 'e.g. apple', query: 'Search',
      tutorialSearch: 'Search tutorials', birdTitle: 'Parrot ID (species + behavior)', choosePhoto: 'Choose / take photo',
      chooseFile: 'Choose file', noFile: 'No file selected', recognize: 'Identify Behavior', recognizing: 'Identifying...',
      birdAlt: 'Parrot to identify', birdResult: 'Recognition Result', chooseBirdFirst: 'Please choose or take a parrot photo first',
      recognizeFail: 'Recognition failed', detectedParrot: 'Parrot detected', noParrot: 'No parrot detected',
      species: 'Species', behavior: 'Behavior', confidence: 'Confidence', behaviorUnavailable: 'Behavior recognition is unavailable or has no result',
      foodResult: 'Food Search Result', foodFamilyFruit: 'Rosaceae or common berry family', foodFamilyCommon: 'Common parrot supplement category',
      foodUnsafe: 'Not recommended as daily food', foodSafe: 'Safe in small amounts', foodAdvice: 'Try a small amount first; avoid salt, sugar, oil and seasoning.',
      submit: 'Submit', refresh: 'Refresh', searchRecord: 'Search medical records', newRecord: 'Write a new medical record',
      add: 'Add', modify: 'Edit', playRecording: 'Play recording',
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
      tutorials: ['Biblioteca', 'Guías de cuidado, corte, baño y limpieza'],
      food: ['Seguridad alimentaria', 'Comprueba si un alimento es apto'],
      'bird-id': ['Identificar ave', 'Sube o toma una foto para identificar especies'],
    },
    reportStats: ['Salud', 'Sueño', 'Llamadas', 'Comidas', 'Excrementos'],
    reportRecords: {
      photos: ['Fotos', '4 fotos emocionadas, 6 fotos durmiendo'],
      recordings: ['Grabaciones', '5 clips de imitación, 3 canciones'],
      risk: ['Riesgo de salud', 'Polvo alto por la tarde; ventila 20 minutos'],
    },
    tutorials: [
      ['Primeros 7 días en casa', 'Cuidado inicial', '8 min'],
      ['Alternativas seguras al corte de alas', 'Entrenamiento', '12 min'],
      ['Calor antes y después del baño medicinal', 'Baño', '6 min'],
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
      foodQuery: 'Consulta de comida', foodName: 'Alimento', foodCategory: 'Categoría', foodPlaceholder: 'p. ej. manzana', query: 'Buscar',
      tutorialSearch: 'Buscar tutoriales', birdTitle: 'Identificación de loro (especie + conducta)', choosePhoto: 'Elegir / tomar foto',
      chooseFile: 'Elegir archivo', noFile: 'Sin archivo', recognize: 'Identificar conducta', recognizing: 'Identificando...',
      birdAlt: 'Loro para identificar', birdResult: 'Resultado', chooseBirdFirst: 'Elige o toma una foto del loro primero',
      recognizeFail: 'Error de reconocimiento', detectedParrot: 'Loro detectado', noParrot: 'No se detectó loro',
      species: 'Especie', behavior: 'Conducta', confidence: 'Confianza', behaviorUnavailable: 'Reconocimiento de conducta no disponible o sin resultado',
      foodResult: 'Resultado de comida', foodFamilyFruit: 'Rosáceas o bayas comunes', foodFamilyCommon: 'Categoría común de suplemento para loros',
      foodUnsafe: 'No recomendado como alimento diario', foodSafe: 'Apto en pequeñas cantidades', foodAdvice: 'Prueba una cantidad pequeña; evita sal, azúcar, aceite y condimentos.',
      submit: 'Enviar', refresh: 'Actualizar', searchRecord: 'Buscar historiales', newRecord: 'Escribe un nuevo historial',
      add: 'Añadir', modify: 'Modificar', playRecording: 'Reproducir grabación',
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
      tutorials: ['チュートリアル', '初心者飼育、羽切り、薬浴、清掃ガイド'],
      food: ['食べ物安全', '食べ物がインコに適するか確認'],
      'bird-id': ['鳥識別', '写真をアップロードして種類を識別'],
    },
    reportStats: ['健康スコア', '睡眠時間', '鳴き声回数', '食事回数', '排泄回数'],
    reportRecords: {
      photos: ['写真記録', '興奮写真 4 枚、睡眠写真 6 枚'],
      recordings: ['録音', '物まね 5 本、歌練習 3 回'],
      risk: ['健康リスク通知', '午後の羽粉が高め、20 分換気を推奨'],
    },
    tutorials: [
      ['お迎え後7日間のケア', '初心者飼育', '8分'],
      ['安全な羽切り代替トレーニング', 'トレーニング', '12分'],
      ['薬浴前後の保温ポイント', '薬浴', '6分'],
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
      foodQuery: '食べ物検索', foodName: '食べ物名', foodCategory: 'カテゴリ', foodPlaceholder: '例：りんご', query: '検索',
      tutorialSearch: '教程キーワード検索', birdTitle: 'インコ識別（種類＋行動）', choosePhoto: '選択 / 撮影',
      chooseFile: 'ファイル選択', noFile: '未選択', recognize: '行動を識別', recognizing: '識別中…',
      birdAlt: '識別するインコ', birdResult: '識別結果', chooseBirdFirst: '先にインコの写真を選択または撮影してください',
      recognizeFail: '識別に失敗しました', detectedParrot: 'インコを検出', noParrot: 'インコ未検出',
      species: '種類', behavior: '行動', confidence: '信頼度', behaviorUnavailable: '行動識別は未有効、または結果がありません',
      foodResult: '食べ物検索結果', foodFamilyFruit: 'バラ科または一般的なベリー類', foodFamilyCommon: '一般的なインコ補助食カテゴリ',
      foodUnsafe: '日常食には非推奨', foodSafe: '少量なら可', foodAdvice: '初回は少量で試し、塩・砂糖・油・調味料を避けてください。',
      submit: '送信', refresh: '更新', searchRecord: 'カルテを検索', newRecord: '新しいカルテを記入',
      add: '追加', modify: '修正', playRecording: '録音を再生',
    },
  },
}

const activeView = computed(() => detailViews[activeRoute.value])
const reportCurveSet = computed(() => reportCurveSets[activeReportRange.value] || reportCurveSets.月报)
const text = computed(() => i18n[systemPrefs.value.language] || i18n.zh)
const ui = computed(() => uiCopy[systemPrefs.value.language] || uiCopy.zh)
const reportCurves = computed(() => reportCurveSet.value.curves.map((curve) => localizeCurve(curve)))
const languageClass = computed(() => `lang-${systemPrefs.value.language}`)
const themeClass = computed(() => (systemPrefs.value.theme === 'dark' ? 'night-theme' : 'day-theme'))
const settingsColorLabel = computed(() => (systemPrefs.value.theme === 'dark' ? text.value.white : text.value.black))
const localizedFoodCategories = computed(() => foodCategories.map((category) => foodCategoryLabel(category)))
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
const localizedReportStats = computed(() => reportStats.map((stat, index) => ({
  ...stat,
  label: ui.value.reportStats[index] || stat.label,
  trend: stat.trend === '稳定' ? labelText('stable') : stat.trend,
})))
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
const localizedTutorialCards = computed(() => tutorialCards.map((item, index) => {
  const copy = ui.value.tutorials?.[index]
  return copy ? { ...item, title: copy[0], tag: copy[1], minutes: copy[2] } : item
}))
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
const selectedAvatarParrot = computed(() => (
  localParrots.value.find((parrot) => parrot.id === account.value.avatarParrotId) || localParrots.value[0]
))
const profileFormAgeStage = computed(() => getAgeStage(profileForm.value.birthday))
const filteredTutorials = computed(() => {
  const keyword = tutorialKeyword.value.trim()
  if (!keyword) return localizedTutorialCards.value
  return localizedTutorialCards.value.filter((item) => `${item.title}${item.tag}`.includes(keyword))
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
  ...basePhotoRecords.value,
])
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
  return ui.value.labels?.[key] || key
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

function foodCategoryLabel(category) {
  const maps = {
    en: { 蔬菜: 'Vegetables', 水果: 'Fruit', 肉类: 'Meat', 昆虫: 'Insects', 谷物: 'Grains' },
    es: { 蔬菜: 'Verduras', 水果: 'Frutas', 肉类: 'Carne', 昆虫: 'Insectos', 谷物: 'Cereales' },
    ja: { 蔬菜: '野菜', 水果: '果物', 肉类: '肉類', 昆虫: '昆虫', 谷物: '穀物' },
  }
  return maps[systemPrefs.value.language]?.[category] || category
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
  return photo.id || `${photo.title}-${photo.time}`
}

function escapeSvgText(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function photoSource(photo) {
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
  try {
    const response = await fetch('/api/photos/batch-delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ids: keys }),
    })
    return response.ok
  } catch {
    return false
  }
}

function removeLocalPhotos(keys) {
  const deleteSet = new Set(keys)
  capturedPhotos.value = capturedPhotos.value.filter((photo) => !deleteSet.has(photoKey(photo)))
  basePhotoRecords.value = basePhotoRecords.value.filter((photo) => !deleteSet.has(photoKey(photo)))
  localStorage.setItem('parrotArchiveSnapshots', JSON.stringify(capturedPhotos.value))

  const profile = profiles.value.find((item) => item.id === selectedParrot.value.id)
  if (profile) profile.photos = `${archivePhotoRecords.value.length} 张`
}

async function deletePhotos() {
  const keys = [...selectedPhotoKeys.value]
  if (!keys.length) {
    openModal('risk', ui.value.noSelection, { value: ui.value.noSelection })
    return
  }

  await callBatchDeletePhotos(keys)
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
  const copies = {
    zh: ['可能的疾病 + 治疗建议', '可能为轻度呼吸道刺激或环境粉尘偏高', '先通风 20 分钟、观察鼻孔和呼吸频率；若持续张口呼吸或精神萎靡，请尽快联系异宠医院。'],
    en: ['Possible Illness + Care Advice', 'Possible mild respiratory irritation or high dust exposure', 'Ventilate for 20 minutes and watch nostrils and breathing rate. If open-mouth breathing or lethargy continues, contact an exotic-pet hospital.'],
    es: ['Posible enfermedad + consejo', 'Posible irritación respiratoria leve o polvo elevado', 'Ventila 20 minutos y observa fosas nasales y respiración. Si continúa respirando con el pico abierto o decaído, contacta un hospital exótico.'],
    ja: ['考えられる病気 + 対処法', '軽い呼吸器刺激、または粉じん高めの可能性', '20分換気し、鼻孔と呼吸回数を観察してください。開口呼吸や元気低下が続く場合はエキゾチック病院へ。'],
  }
  const copy = copies[systemPrefs.value.language] || copies.zh
  openModal('diagnosis', copy[0], {
    summary: copy[1],
    advice: copy[2],
  })
}

function refreshHospitals() {
  const currentIndex = hospitalPins.findIndex((item) => item.id === selectedHospital.value.id)
  selectedHospital.value = hospitalPins[(currentIndex + 1) % hospitalPins.length]
}

function queryFood() {
  const name = foodQuery.value.trim() || labelText('foodPlaceholder').replace(/^.*[:：]\s*/, '')
  openModal('food', labelText('foodResult'), {
    name,
    category: foodCategoryLabel(foodCategory.value),
    family: foodCategory.value === '水果' ? labelText('foodFamilyFruit') : labelText('foodFamilyCommon'),
    result: foodCategory.value === '肉类' ? labelText('foodUnsafe') : labelText('foodSafe'),
    advice: labelText('foodAdvice'),
  })
}

function openCurve(curve) {
  if (isReportGaugeCurve(curve)) {
    openMetricGauge(curveToMetric(curve))
    return
  }
  openModal('curve', curve.label, { ...curve, xAxis: localizedXAxis(reportCurveSet.value.xAxis) })
}

function openDustGauge(snapshot) {
  openMetricGauge({
    metric: snapshot.metric || 'dust',
    label: snapshot.label || labelText('dust'),
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
  if (text.includes('粉') || text.includes('尘') || text.includes('绮') || text.includes('μg') || text.includes('渭g') || text.includes('/m')) return 'dust'
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
    displayValue: `${latest}${curve.unit || 'μg/m³'}`,
    unit: curve.unit || 'μg/m³',
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
  window.addEventListener('growth-report-ready', handleGrowthReportReady)
  window.setTimeout(() => {
    if (notificationBadges.value.growth) showGrowthReportToast()
  }, 600)
})

onBeforeUnmount(() => {
  window.clearTimeout(reportToastTimer)
  window.clearTimeout(alarmToastTimer)
  window.removeEventListener('growth-report-ready', handleGrowthReportReady)
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

function linePointCoordinate(points, index, width = 260, height = 92) {
  return linePoints(points, width, height).split(' ')[index] || '0,0'
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
            <article v-for="stat in localizedReportStats" :key="stat.label" class="highlight-card">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ rangeText(activeReportRange) }}{{ ui.changeSuffix }}：{{ stat.trend }}</p>
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
              <svg class="mini-line-chart" viewBox="0 0 260 92" aria-label="历史趋势折线图">
                <polyline :points="linePoints(curve.points)" />
                <g
                  v-for="(point, index) in curve.points"
                  :key="`${curve.label}-${index}`"
                  class="chart-point"
                  :transform="`translate(${linePointCoordinate(curve.points, index)})`"
                >
                  <circle r="4" />
                  <text class="chart-point-tooltip" y="-12" text-anchor="middle">{{ point }}{{ curve.unit }}</text>
                </g>
              </svg>
            </button>
          </section>

          <section class="record-grid" aria-label="照片和录音记录">
            <button
              v-for="record in localizedReportRecords"
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
          <button v-for="module in localizedMedicalModules" :key="module.key" class="module-card action-card" type="button" @click="openThird(module.key)">
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
          <button v-for="module in localizedHandbookModules" :key="module.key" class="module-card action-card" type="button" @click="openThird(module.key)">
            <h2>{{ module.title }}</h2>
            <p>{{ module.note }}</p>
          </button>
        </section>

        <section v-else-if="thirdView === 'food'" class="third-page form-page">
          <article class="questionnaire-card">
            <h2>{{ labelText('foodQuery') }}</h2>
            <label><span>{{ labelText('foodName') }}</span><input v-model="foodQuery" :placeholder="labelText('foodPlaceholder')" /></label>
            <label><span>{{ labelText('foodCategory') }}</span><select v-model="foodCategory"><option v-for="category in foodCategories" :key="category" :value="category">{{ foodCategoryLabel(category) }}</option></select></label>
            <button type="button" @click="queryFood">{{ labelText('query') }}</button>
          </article>
        </section>

        <section v-else-if="thirdView === 'tutorials'" class="third-page records-page">
          <input v-model="tutorialKeyword" class="search-input" :placeholder="labelText('tutorialSearch')" />
          <article v-for="tutorial in filteredTutorials" :key="tutorial.title" class="memo-card">
            <strong>{{ tutorial.title }}</strong>
            <span>{{ tutorial.tag }} · {{ tutorial.minutes }}</span>
          </article>
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

    <!-- 非全屏时保持原挂载位置；监控全屏时将弹窗移入 MonitorCard 的全屏 DOM。 -->
    <Teleport :to="monitorFullscreen ? '#monitor-modal-host' : 'body'" :disabled="!monitorFullscreen">
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
            <label><span>鹦鹉名字</span><input v-model="profileForm.name" placeholder="例如：农药" /></label>
            <label><span>出生日期</span><input v-model="profileForm.birthday" placeholder="xxxx-xx-xx" /></label>
            <label><span>年龄标识</span><input :value="profileFormAgeStage" readonly /></label>
            <label><span>当前体重</span><input v-model="profileForm.weight" placeholder="例如：78g" /></label>
          </template>
          <template v-else-if="modal.type === 'setting-toggles'">
            <div class="setting-toggle-row">
              <span>{{ labelText('notifications') }}</span>
              <button type="button" :class="{ active: notificationEnabled }" @click="notificationEnabled = !notificationEnabled"></button>
            </div>
            <div class="setting-toggle-row">
              <span>{{ labelText('devicePermissions') }}</span>
              <button type="button" :class="{ active: permissionEnabled }" @click="permissionEnabled = !permissionEnabled"></button>
            </div>
          </template>
          <template v-else-if="modal.type === 'diagnosis'">
            <p><strong>{{ modal.item.summary }}</strong></p>
            <p>{{ modal.item.advice }}</p>
          </template>
          <template v-else-if="modal.type === 'food'">
            <p>{{ modal.item.name }} · {{ modal.item.category }} · {{ modal.item.family }}</p>
            <p>{{ modal.item.result }}{{ sentenceBreak() }}{{ modal.item.advice }}</p>
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
                <g
                  v-for="(point, index) in modal.item.points"
                  :key="`${modal.item.label}-detail-${index}`"
                  class="chart-point chart-point-large"
                  :transform="`translate(${linePointCoordinate(modal.item.points, index, 520, 220)})`"
                >
                  <circle r="6" />
                  <text class="chart-point-tooltip" y="-16" text-anchor="middle">{{ point }}{{ modal.item.unit }}</text>
                </g>
              </svg>
              <div class="chart-label-row">
                <span v-for="label in modal.item.xAxis" :key="label">{{ label }}</span>
              </div>
              <span class="axis-x">{{ activeReportRange === '日报' ? labelText('hourlyTrend') : `${rangeText(activeReportRange)} ${labelText('trend')}` }}</span>
            </div>
          </template>
          <template v-else-if="modal.type === 'photo-preview'">
            <figure class="photo-preview">
              <img :src="photoSource(modal.item)" :alt="modal.item.title" />
              <figcaption>{{ modal.item.title }} · {{ modal.item.time }}</figcaption>
            </figure>
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
          <button type="button" class="ghost-button" @click="closeModal">{{ text.cancel || '取消' }}</button>
          <button v-if="modal.type === 'archive-create'" type="button" class="save-button" @click="saveNewProfile">保存</button>
          <button v-else-if="modal.type === 'photo-preview'" type="button" class="save-button" @click="downloadPhoto(modal.item)">{{ ui.savePhoto }}</button>
          <button v-else type="button" class="save-button" @click="closeModal">{{ text.confirm }}</button>
        </footer>
      </section>
    </div>
    </Teleport>
  </main>
</template>
