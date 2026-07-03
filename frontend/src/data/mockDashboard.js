export const parrots = [
  {
    id: 'sun-001',
    name: '啾啾 · 小太阳鹦鹉',
    shortName: '啾啾',
    avatarType: 'avatar-orange',
    species: '小太阳鹦鹉',
    birthday: '2024-05-18',
    weight: '78g',
    sex: '公',
    status: '站立',
    route: '/archive',
  },
  {
    id: 'budgie-002',
    name: '豆豆 · 虎皮鹦鹉',
    shortName: '豆豆',
    avatarType: 'avatar-orange',
    species: '虎皮鹦鹉',
    birthday: '2025-01-09',
    weight: '42g',
    sex: '母',
    status: '吃东西',
    route: '/archive',
  },
  {
    id: 'cockatiel-003',
    name: '奶油 · 玄凤鹦鹉',
    shortName: '奶油',
    avatarType: 'avatar-orange',
    species: '玄凤鹦鹉',
    birthday: '2023-11-22',
    weight: '92g',
    sex: '未知',
    status: '睡觉',
    route: '/archive',
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
    subtitle: '账号、通知、位置与设备权限',
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
    title: '社区与记账',
    subtitle: '帖子、用品评价和饲养花费',
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

export const archiveProfiles = [
  {
    id: 'sun-001',
    name: '啾啾',
    species: '小太阳鹦鹉',
    birthday: '2024-05-18',
    weight: '78g',
    sex: '公',
    status: '当前状态站立',
    device: '笼舍 A-01',
    photos: '128 张',
    lastWeight: '2026-07-03 录入 78g',
  },
  {
    id: 'budgie-002',
    name: '豆豆',
    species: '虎皮鹦鹉',
    birthday: '2025-01-09',
    weight: '42g',
    sex: '母',
    status: '当前状态吃东西',
    device: '笼舍 B-02',
    photos: '76 张',
    lastWeight: '2026-07-01 录入 42g',
  },
  {
    id: 'cockatiel-003',
    name: '奶油',
    species: '玄凤鹦鹉',
    birthday: '2023-11-22',
    weight: '92g',
    sex: '未知',
    status: '当前状态睡觉',
    device: '笼舍 C-01',
    photos: '204 张',
    lastWeight: '2026-06-29 录入 92g',
  },
]

export const reportStats = [
  { label: '健康评分', value: '92', trend: '+4' },
  { label: '睡眠时长', value: '10.4h', trend: '+0.8h' },
  { label: '鸣叫次数', value: '136', trend: '+18' },
  { label: '进食次数', value: '7', trend: '稳定' },
  { label: '排泄次数', value: '14', trend: '-2' },
]

export const reportCurves = [
  { label: '温度曲线', value: '26.3°C', unit: '°C', axis: '环境温度', points: [24.1, 24.8, 25.2, 26.0, 26.3, 25.9, 25.6] },
  { label: '湿度曲线', value: '58%', unit: '%', axis: '环境湿度', points: [52, 55, 57, 58, 56, 59, 58] },
  { label: '粉尘曲线', value: '低', unit: 'μg/m³', axis: '羽粉浓度', points: [18, 21, 19, 16, 20, 17, 15] },
  { label: '体重变化曲线', value: '78g', unit: 'g', axis: '体重', points: [76.8, 77.1, 77.4, 77.5, 77.8, 78.0, 78.0] },
]

export const reportRecords = [
  { type: '照片记录', value: '最兴奋照片 4 张，睡觉照片 6 张' },
  { type: '学舌录音', value: '5 段，2 段疑似成功模仿' },
  { type: '歌曲录音', value: '小星星练习 3 次' },
  { type: '健康风险提醒', value: '下午羽粉偏高，建议通风 20 分钟' },
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
  { key: 'bird-id', title: '拍照识鸟', note: '上传或拍照识别鹦鹉种类' },
]

export const foodCategories = ['蔬菜', '水果', '肉类', '昆虫', '谷物']

export const tutorialCards = [
  { title: '新手到家 7 天照护', tag: '新手喂养', minutes: '8 分钟' },
  { title: '安全剪羽与替代训练', tag: '剪羽教程', minutes: '12 分钟' },
  { title: '药浴前后的保温要点', tag: '药浴教程', minutes: '6 分钟' },
]

export const communityModules = [
  { key: 'posts', title: '社区问答', note: '发布问题，查看附近鸟友回复' },
  { key: 'products', title: '用品评价', note: '筛选靠谱用品和真实使用反馈' },
  { key: 'ledger', title: '记账本', note: '按鹦鹉档案记录饲养花费' },
]

export const userSettingCards = [
  { key: 'avatar', title: '用户头像', value: '默认蛋头像', note: '建立宠物档案后可选择鹦鹉模型头像' },
  { key: 'name', title: '用户名', value: 'Wenderella', note: '用于社区帖子和报告接收人' },
  { key: 'phone', title: '手机绑定', value: '138****6628', note: '用于提醒、找回账号和紧急通知' },
  { key: 'id', title: '用户 ID', value: 'U-230701-042', note: '只读，用于客服核对' },
  { key: 'location', title: '位置信息', value: '上海市 · 浦东新区', note: '用于推荐附近鸟友和异宠医院' },
  { key: 'defaultParrot', title: '默认鹦鹉', value: '啾啾', note: '打开应用默认展示的档案' },
  { key: 'notice', title: '通知设置', value: '健康、喂食、清洁已开启', note: '可细分日报、周报、异常提醒' },
  { key: 'permission', title: '设备权限', value: '摄像头、麦克风、位置', note: '用于视频通话、学舌录音和附近服务' },
]

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
    kind: 'community',
    title: '社区与记账',
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
