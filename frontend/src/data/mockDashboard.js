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
    deviceId: 'device-001',
    photos: '0 张',
    lastWeight: '暂无体重记录',
    weightHistory: [],
  },
]

// 成长报告指标卡：不再使用 mock 数据，改由前端根据真实环境历史 / 体重记录计算。
export const reportStats = [];

// 成长报告曲线：不再使用 mock 数据，改由前端根据后端预聚合的小时报表实时生成。
// 保留空结构以兼容旧引用；无真实数据时曲线区域为空。
export const reportCurveSets = {};

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
    minutes: '10 分钟',
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
    minutes: '8 分钟',
    summary: '药浴能辅助治疗羽虱、皮屑等问题，但鹦鹉体温高、羽毛保温性强，湿身后一旦着凉很容易生病。浴前浴后的保温是关键。',
    article: '/tutorials/medicated-bath.md',
  },
  {
    id: 'daily-cleaning',
    title: '笼舍日常清洁与消毒',
    tag: '清洁教程',
    minutes: '9 分钟',
    summary: '干净的笼舍是预防呼吸道和消化道疾病的基础。按每日、每周、每月三个层次做好清洁节奏，能大幅减少细菌和羽粉堆积。',
    article: '/tutorials/daily-cleaning.md',
  },
  {
    id: 'health-check',
    title: '判断鹦鹉是否健康',
    tag: '健康观察',
    minutes: '8 分钟',
    summary: '鹦鹉善于隐藏不适，等到明显萎靡时往往病情已经加重。每天花 2 分钟观察精神、排泄、羽毛和体重，能帮你早发现、早处理。',
    article: '/tutorials/health-check.md',
  },
  {
    id: 'temperature-care',
    title: '夏季防暑与冬季保暖',
    tag: '环境管理',
    minutes: '9 分钟',
    summary: '鹦鹉对温度变化比较敏感，最适宜的环境温度在 20-26℃ 之间。夏季防暑和冬季保暖都要避免极端手段，重点是稳定和通风。',
    article: '/tutorials/temperature-care.md',
  },
  {
    id: 'toxic-foods',
    title: '鹦鹉常见有毒食物清单',
    tag: '食物安全',
    minutes: '7 分钟',
    summary: '鹦鹉的代谢和人类不同，很多人能吃的东西对它们是毒药。整理一份绝对不能喂的清单，并给出安全替代品和误食处理方法。',
    article: '/tutorials/toxic-foods.md',
  },
  {
    id: 'molting-care',
    title: '换羽期护理要点',
    tag: '换羽护理',
    minutes: '8 分钟',
    summary: '鹦鹉每年换羽 1-2 次，期间体能消耗大。本篇讲换羽期怎么补充营养、做好保湿、减少应激，以及如何识别异常掉羽。',
    article: '/tutorials/molting-care.md',
  },
  {
    id: 'feather-plucking',
    title: '啄羽问题排查',
    tag: '行为问题',
    minutes: '10 分钟',
    summary: '鹦鹉拔自己羽毛是常见又棘手的问题，背后原因往往不止一个。本篇提供一个从健康、环境、营养到行为的逐步排查思路。',
    article: '/tutorials/feather-plucking.md',
  },
  {
    id: 'daily-diet',
    title: '日常喂养与食谱搭配',
    tag: '喂养指南',
    minutes: '9 分钟',
    summary: '鹦鹉不能只吃种子。本篇讲怎么搭配一份相对均衡的日常食谱，包括滋养丸、蔬果、种子的比例，以及不同阶段的需求。',
    article: '/tutorials/daily-diet.md',
  },
  {
    id: 'cage-setup',
    title: '笼舍与玩具布置',
    tag: '环境布置',
    minutes: '8 分钟',
    summary: '笼子是鹦鹉的家，布置得当能减少应激、预防肥胖、避免受伤。本篇讲笼子尺寸、站杆选择、玩具配置和安全检查清单。',
    article: '/tutorials/cage-setup.md',
  },
  {
    id: 'training-basics',
    title: '训练入门：上手与回笼',
    tag: '训练教程',
    minutes: '10 分钟',
    summary: '训练不是为了让鹦鹉表演，而是建立信任、方便日常护理。本篇讲最基础的上手和回笼指令训练，以及常见问题和禁忌。',
    article: '/tutorials/training-basics.md',
  },
  {
    id: 'travel-cage',
    title: '外出与外出笼使用',
    tag: '外出安全',
    minutes: '8 分钟',
    summary: '带鹦鹉出门风险不小：飞丢、受惊、温差、接触病菌。本篇讲怎么选外出笼、出门前准备，以及应对惊吓和户外风险。',
    article: '/tutorials/travel-cage.md',
  },
  {
    id: 'sleep-light',
    title: '夜间光照与睡眠管理',
    tag: '作息管理',
    minutes: '7 分钟',
    summary: '鹦鹉需要充足且高质量的睡眠。睡不好会导致激素紊乱、暴躁、免疫力下降。本篇讲光照规律、笼衣使用和小夜灯的选择。',
    article: '/tutorials/sleep-light.md',
  },
  {
    id: 'nail-beak-care',
    title: '修剪指甲与喙部护理',
    tag: '日常护理',
    minutes: '7 分钟',
    summary: '指甲过长会影响站立、勾住织物导致骨折；喙部异常则影响进食。本篇讲怎么判断该剪、怎么安全修剪，以及喙部日常护理。',
    article: '/tutorials/nail-beak-care.md',
  },
  {
    id: 'first-aid',
    title: '急救箱与常见意外处理',
    tag: '急救常识',
    minutes: '10 分钟',
    summary: '意外发生时，正确的前期处理能为就医争取时间。本篇讲怎么准备鹦鹉急救箱，以及出血、撞伤、缠绕、中毒、中暑等处理原则。',
    article: '/tutorials/first-aid.md',
  },
  {
    id: 'body-language',
    title: '鹦鹉情绪与肢体语言',
    tag: '行为理解',
    minutes: '8 分钟',
    summary: '鹦鹉不会说话，但会用身体表达情绪。本篇帮你读懂开心、紧张、生气、生病和求偶期的信号，避免被咬了才知道它不爽。',
    article: '/tutorials/body-language.md',
  },
  {
    id: 'common-mistakes',
    title: '新手常见误区',
    tag: '避坑指南',
    minutes: '9 分钟',
    summary: '养鹦鹉新手容易踩的坑很多，本篇整理 15 个高频误区，从喂养、笼舍、剪羽到训练，帮你少走弯路、少花钱、少让鸟受罪。',
    article: '/tutorials/common-mistakes.md',
  },
  {
    id: 'water-calcium',
    title: '饮水与补钙常识',
    tag: '营养健康',
    minutes: '7 分钟',
    summary: '水和钙是鹦鹉最容易被忽视的两样东西。本篇讲怎么喂水、水盒怎么选，以及为什么补钙重要、怎么安全补钙和常见误区。',
    article: '/tutorials/water-calcium.md',
  },
  {
    id: 'seasonal-calendar',
    title: '全年护理日历',
    tag: '季节管理',
    minutes: '8 分钟',
    summary: '鹦鹉的护理需求随季节变化。本篇给你一份 12 个月的养护清单，以及每月、每周、每天和年度的固定任务，按节奏来更轻松。',
    article: '/tutorials/seasonal-calendar.md',
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
