export const parrotSpeciesOptions = [
  '虎皮',
  '牡丹',
  '玄凤',
  '小太阳',
  '和尚',
  '吸蜜',
  '凯克',
  '黑顶',
  '折衷',
  '裸胸',
  '金太阳',
]

// 模拟鹦鹉仅作未登录/后端不可用时的降级展示；统一使用有真实数据的 device-001。
// 登录后若后端有宠物档案，会被 loadCareBootstrap 替换为真实数据。
export const parrots = [
  {
    id: 'demo-001',
    petId: 'PET-DEMO-001',
    deviceId: 'device-001',
    name: '演示鹦鹉',
    shortName: '演示鹦鹉',
    avatarType: 'avatar-orange',
    species: '小太阳',
    birthday: '2024-05-18',
    weight: '78g',
    sex: '公',
    status: '站立',
    ageStage: '青少年',
    route: '/archive',
    apiRaw: { currentStatus: 'standing', sex: 'male' },
  },
]

export const currentParrot = parrots[0]

export const entryCards = {
  archive: {
    key: 'archive',
    title: '宠物档案',
    subtitle: '头像模型、资料、体重与相册',
    theme: 'purple',
    visual: 'archive',
    route: '/archive',
  },
  growth: {
    key: 'growth',
    title: '成长报告',
    subtitle: '日报周报月报与健康曲线',
    theme: 'purple',
    visual: 'growth',
    route: '/growth-report',
    badge: 2,
  },
  settings: {
    key: 'settings',
    title: '用户设置',
    subtitle: '头像、账号、位置与权限',
    theme: 'green',
    visual: 'settings',
    route: '/settings',
  },
  medical: {
    key: 'medical',
    title: '医疗助手',
    subtitle: '智能问诊、附近医院与病历',
    theme: 'orange',
    visual: 'medical',
    route: '/medical-assistant',
    badge: 1,
  },
  ledger: {
    key: 'ledger',
    title: '记账本',
    subtitle: '按时间记录饲养花费',
    theme: 'blue',
    visual: 'ledger',
    route: '/community-ledger',
  },
  handbook: {
    key: 'handbook',
    title: '饲养手册',
    subtitle: '教程库、食物安全、拍照识鸟',
    theme: 'lavender',
    visual: 'handbook',
    route: '/care-handbook',
  },
}

export const primaryCards = {
  monitor: {
    key: 'monitor',
    title: '实时视频通话',
    route: '/monitor',
    statusLabel: '当前状态：站立',
    online: true,
  },
}

// 档案卡片降级数据：仅当后端无鹦鹉档案时展示这 1 条。
export const archiveProfiles = [
  {
    id: 'demo-001',
    avatarType: 'avatar-orange',
    name: '演示鹦鹉',
    species: '小太阳',
    birthday: '2024-05-18',
    weight: '78g',
    sex: '公',
    status: '当前状态站立',
    ageStage: '青少年',
    device: 'device-001',
    photos: '0 张',
    lastWeight: '暂无体重记录',
    weightHistory: [],
  },
]

export const reportStats = [
  { label: '健康评分', value: '92', trend: '+4' },
  { label: '睡眠时长', value: '10.4h', trend: '+0.8h' },
  { label: '鸣叫次数', value: '136', trend: '+18' },
  { label: '进食次数', value: '7', trend: '稳定' },
  { label: '排泄次数', value: '14', trend: '-2' },
]

export const reportCurveSets = {
  日报: {
    xAxis: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
    curves: [
      { label: '温度曲线', value: '26.3°C', unit: '°C', axis: '环境温度', points: [24.1, 24.3, 25.2, 26.0, 26.3, 25.9, 25.6] },
      { label: '湿度曲线', value: '58%', unit: '%', axis: '环境湿度', points: [55, 57, 56, 58, 60, 59, 58] },
      { label: '粉尘曲线', value: '低', unit: 'μg/m³', axis: '羽粉浓度', points: [16, 18, 22, 19, 20, 17, 15] },
      { label: '体重变化曲线', value: '78g', unit: 'g', axis: '体重', points: [78, 78, 78, 78.1, 78, 78, 78] },
    ],
  },
  周报: {
    xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    curves: [
      { label: '温度曲线', value: '26.3°C', unit: '°C', axis: '环境温度', points: [24.1, 24.8, 25.2, 26.0, 26.3, 25.9, 25.6] },
      { label: '湿度曲线', value: '58%', unit: '%', axis: '环境湿度', points: [52, 55, 57, 58, 56, 59, 58] },
      { label: '粉尘曲线', value: '低', unit: 'μg/m³', axis: '羽粉浓度', points: [18, 21, 19, 16, 20, 17, 15] },
      { label: '体重变化曲线', value: '78g', unit: 'g', axis: '体重', points: [76.8, 77.1, 77.4, 77.5, 77.8, 78.0, 78.0] },
    ],
  },
  月报: {
    xAxis: ['第1周', '第2周', '第3周', '第4周'],
    curves: [
      { label: '温度曲线', value: '26.1°C', unit: '°C', axis: '环境温度', points: [25.2, 25.8, 26.1, 26.3] },
      { label: '湿度曲线', value: '57%', unit: '%', axis: '环境湿度', points: [54, 56, 57, 58] },
      { label: '粉尘曲线', value: '低', unit: 'μg/m³', axis: '羽粉浓度', points: [20, 18, 16, 15] },
      { label: '体重变化曲线', value: '78g', unit: 'g', axis: '体重', points: [76.5, 77.2, 77.8, 78.0] },
    ],
  },
}

export const reportRecords = [
  { type: '照片记录', value: '最兴奋照片 4 张，睡觉照片 6 张', action: 'photos' },
  { type: '录音', value: '学舌 5 段，歌曲练习 3 次', action: 'recordings' },
  { type: '健康风险提醒', value: '下午羽粉偏高，建议通风 20 分钟', action: 'risk' },
]

export const photoRecords = [
  { id: 'photo-excited', title: '最兴奋照片', time: '07-03 15:30' },
  { id: 'photo-sleep', title: '睡觉照片', time: '07-03 13:12' },
  { id: 'photo-meal', title: '吃饭照片', time: '07-02 18:40' },
  { id: 'photo-stand', title: '站立照片', time: '07-02 09:21' },
  { id: 'photo-wing', title: '扇翅膀照片', time: '07-01 17:08' },
  { id: 'photo-call', title: '大叫照片', time: '07-01 10:25' },
]

export const recordingRecords = [
  { title: '学舌练习：你好啾啾', time: '07-03 16:02', length: '00:18' },
  { title: '歌曲练习：小星星', time: '07-03 12:20', length: '00:32' },
  { title: '鸣叫情绪：兴奋', time: '07-02 18:11', length: '00:12' },
  { title: '学舌练习：早上好', time: '07-02 08:05', length: '00:21' },
]

export const medicalModules = [
  { key: 'diagnosis', title: '智能问诊', note: '填写外在表现问卷，获得初步风险判断' },
  { key: 'hospitals', title: '附近医院', note: '查看可治疗异宠的医院和联系方式' },
  { key: 'records', title: '病历', note: '按时间记录就诊、用药和复查事项' },
]

export const hospitalPins = [
  { id: 'h1', name: '晨羽异宠医院', address: '浦东新区栖霞路 88 号', phone: '021-6628-1101', x: 62, y: 36 },
  { id: 'h2', name: '绿洲宠物诊疗中心', address: '张江路 218 号', phone: '021-7712-0933', x: 41, y: 58 },
  { id: 'h3', name: '南风鸟类门诊', address: '花木路 16 号', phone: '021-6800-5720', x: 74, y: 67 },
]

export const handbookModules = [
  { key: 'tutorials', title: '教程库', note: '新手喂养、剪羽、药浴、清洁教程' },
  { key: 'food', title: '食物安全', note: '输入食物名称查询是否适合鹦鹉' },
  { key: 'bird-id', title: '拍照识鹦鹉', note: '上传或拍照识别种类与行为' },
]

export const foodCategories = ['蔬菜', '水果', '肉类', '昆虫', '谷物']

export const tutorials = [
  {
    id: 'newbie-7days',
    title: '新手到家 7 天照护',
    tag: '新手喂养',
    minutes: '8 分钟',
    summary: '刚接回家的鹦鹉需要静养、观察和逐步建立信任。这份 7 天计划帮你度过最关键适应期，避免因为过度互动或乱喂食导致应激。',
    article: '/tutorials/newbie-7days.md',
  },
  {
    id: 'wing-trim',
    title: '安全剪羽与替代训练',
    tag: '剪羽教程',
    minutes: '12 分钟',
    summary: '剪羽不是必选项。如果担心飞丢，可以先通过环境管理和召回训练来降低风险；确实需要剪羽时，也要掌握正确方法，避免剪到血羽。',
    article: '/tutorials/wing-trim.md',
  },
  {
    id: 'medicated-bath',
    title: '药浴前后的保温要点',
    tag: '药浴教程',
    minutes: '6 分钟',
    summary: '药浴能辅助治疗羽虱、皮屑等问题，但鹦鹉体温高、羽毛保温性强，湿身后一旦着凉很容易生病。浴前浴后的保温是关键。',
    article: '/tutorials/medicated-bath.md',
  },
  {
    id: 'daily-cleaning',
    title: '笼舍日常清洁与消毒',
    tag: '清洁教程',
    minutes: '7 分钟',
    summary: '干净的笼舍是预防呼吸道和消化道疾病的基础。每天做简单清理、每周做深度清洁，再配合正确的消毒剂，能大幅减少细菌和羽粉堆积。',
    article: '/tutorials/daily-cleaning.md',
  },
  {
    id: 'health-check',
    title: '判断鹦鹉是否健康',
    tag: '健康观察',
    minutes: '5 分钟',
    summary: '鹦鹉善于隐藏不适，等到明显萎靡时往往病情已经加重。每天花 2 分钟观察精神状态、排泄和羽毛，能帮你早发现、早处理。',
    article: '/tutorials/health-check.md',
  },
  {
    id: 'temperature-care',
    title: '夏季防暑与冬季保暖',
    tag: '环境管理',
    minutes: '6 分钟',
    summary: '鹦鹉对温度变化比较敏感，最适宜的环境温度在 20-26℃ 之间。夏季防暑和冬季保暖都要避免极端手段，重点是“稳定”和“通风”。',
    article: '/tutorials/temperature-care.md',
  },
]

// 未登录时的用户资料占位，登录后会被 GET /auth/me 返回的真实资料覆盖。
export const userProfile = {
  avatarParrotId: 'demo-001',
  username: '',
  phone: '',
  email: '',
  phoneBound: false,
  emailBound: false,
  userId: '',
  location: '',
}

export const detailViews = {
  '/archive': {
    kind: 'archive',
    title: '宠物档案',
    theme: 'purple',
    visual: 'archive',
  },
  '/growth-report': {
    kind: 'report',
    title: '成长报告',
    theme: 'purple',
    visual: 'growth',
  },
  '/settings': {
    kind: 'settings',
    title: '用户设置',
    theme: 'green',
    visual: 'settings',
  },
  '/medical-assistant': {
    kind: 'medical',
    title: '医疗助手',
    theme: 'orange',
    visual: 'medical',
  },
  '/community-ledger': {
    kind: 'ledger',
    title: '记账本',
    theme: 'blue',
    visual: 'ledger',
  },
  '/care-handbook': {
    kind: 'handbook',
    title: '饲养手册',
    theme: 'lavender',
    visual: 'handbook',
  },
}
