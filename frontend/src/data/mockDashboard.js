export const parrots = [
  {
    id: 'sun-001',
    name: '小太阳 · 啾啾',
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
    name: '虎皮 · 豆豆',
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
    name: '玄凤 · 奶油',
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
    subtitle: '头像模型、资料、体重与设备',
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
    subtitle: '问诊、医院、保险与风险提醒',
    theme: 'orange',
    visual: 'medical',
    route: '/medical-assistant',
    badge: 1,
  },
  ledger: {
    key: 'ledger',
    title: '社区与记账',
    subtitle: '真实用品评价、附近鸟友和花费',
    theme: 'blue',
    visual: 'ledger',
    route: '/community-ledger',
  },
  handbook: {
    key: 'handbook',
    title: '饲养百科',
    subtitle: '教程、食物查询、识图搜索',
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

export const archiveCards = [
  {
    key: 'model',
    title: '鹦鹉档案头像与模型',
    value: '橙色小太阳',
    note: '支持 DIY 羽色、拍照生成头像、动作模型：睡觉/扇翅膀/吃东西/站立/大叫',
  },
  {
    key: 'profile',
    title: '基本资料',
    value: '啾啾 · 小太阳鹦鹉',
    note: '出生 2024-05-18 · 78g · 公 · 当前状态站立',
  },
  {
    key: 'weight',
    title: '体重记录',
    value: '78g',
    note: '近 7 天 +1.2g，曲线稳定',
  },
  {
    key: 'device',
    title: '设备绑定',
    value: '笼舍 A-01',
    note: '摄像头、温湿度、亮度、羽粉传感器在线',
  },
  {
    key: 'gallery',
    title: '成长相册',
    value: '128 张',
    note: '截图会自动保存到这里，支持按日期和类型查看',
  },
  {
    key: 'list',
    title: '已建档鹦鹉列表',
    value: '3 只',
    note: '啾啾、豆豆、奶油；支持多鹦鹉切换和删除档案',
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
  { label: '温度曲线', value: '26.3℃', points: [28, 34, 31, 45, 42, 49, 44] },
  { label: '湿度曲线', value: '58%', points: [55, 48, 53, 50, 57, 52, 60] },
  { label: '粉尘曲线', value: '低', points: [38, 31, 35, 28, 33, 26, 30] },
  { label: '体重变化曲线', value: '78g', points: [40, 42, 41, 45, 46, 49, 52] },
]

export const reportRecords = [
  { type: '照片记录', value: '最兴奋照片 4 张，睡觉照片 6 张' },
  { type: '学舌录音', value: '5 段，2 段疑似成功模仿' },
  { type: '歌曲录音', value: '小星星练习 3 次' },
  { type: '健康风险提醒', value: '下午羽粉偏高，建议通风 20 分钟' },
]

export const userSettingCards = [
  { key: 'avatar', title: '用户头像', value: '默认蛋头像', note: '建立宠物档案后可选择鹦鹉模型头像' },
  { key: 'name', title: '用户名', value: 'Wenderella', note: '用于社区帖子和报告接收人' },
  { key: 'phone', title: '手机绑定', value: '138****6628', note: '用于提醒、找回账号和紧急通知' },
  { key: 'id', title: '用户 ID', value: 'U-230701-042', note: '只读，用于客服核对' },
  { key: 'location', title: '位置信息', value: '上海市 · 浦东新区', note: '用于推荐附近鸟友和异宠医院' },
  { key: 'defaultParrot', title: '默认鹦鹉', value: '啾啾', note: '打开应用默认展示的档案' },
  { key: 'notice', title: '通知设置', value: '健康、喂食、清洁已开启', note: '可细分日报、周报、异常提醒' },
  { key: 'permission', title: '设备权限', value: '摄像头/麦克风/位置', note: '用于视频通话、学舌录音和附近服务' },
]

export const detailViews = {
  '/archive': {
    kind: 'archive',
    title: '宠物档案',
    theme: 'purple',
    visual: 'archive',
    intro: '以卡片管理每只鹦鹉，不用整页长表单；新增、编辑、删除都通过弹窗完成。',
    filters: ['全部鹦鹉', '小太阳', '虎皮', '玄凤', '需录入体重'],
  },
  '/growth-report': {
    kind: 'report',
    title: '成长报告',
    theme: 'purple',
    visual: 'growth',
    intro: '支持日报、周报、月报切换，集中查看健康评分、行为次数、环境曲线、照片和录音。',
    filters: ['时间', '鹦鹉', '类型', '关键词'],
  },
  '/settings': {
    kind: 'settings',
    title: '用户设置',
    theme: 'green',
    visual: 'settings',
    intro: '每个设置卡片只展示必要信息和一个编辑按钮，修改内容统一在弹窗里完成。',
    filters: ['账号', '通知', '权限', '位置'],
  },
  '/medical-assistant': {
    kind: 'generic',
    title: '医疗助手',
    theme: 'orange',
    visual: 'medical',
    intro: '围绕异宠就医、基础问诊、保险和环境风险提醒，给主人更明确的下一步建议。',
    filters: ['线上问诊', '附近医院', '保险', '紧急风险'],
    actions: ['填写问卷', '查附近医院', '查看保险'],
    highlights: [
      { label: '环境建议', value: '开空调', note: '温度高于舒适上限' },
      { label: '问诊结果', value: '轻度呼吸风险', note: '建议通风并观察鼻孔' },
      { label: '附近医院', value: '3家', note: '筛选可治疗异宠' },
    ],
    panels: [
      {
        title: '基础健康',
        items: ['检测亮度、温度、湿度、羽粉浓度', '提示开空调、通风、放遮光帘', '建立健康风险提醒'],
      },
      {
        title: '智慧就医',
        items: ['问卷判断大致疾病类型和应对方法', '展示周围可治疗异宠的宠物医院', '支持线上问诊入口'],
      },
      {
        title: '保险与边界',
        items: ['展示支持鹦鹉的宠物保险', '重点面向家养小型鹦鹉', '大型需证鹦鹉仅在识鸟功能中展示知识'],
      },
    ],
  },
  '/community-ledger': {
    kind: 'generic',
    title: '社区与记账',
    theme: 'blue',
    visual: 'ledger',
    intro: '把真实养鸟经验、用品评价和日常花费放在一起，方便主人做决定和复盘成本。',
    filters: ['附近帖子', '用品评价', '本月花费', '按鹦鹉筛选'],
    actions: ['发布问题', '新增消费', '跳转购买'],
    highlights: [
      { label: '总花费', value: '￥2,486', note: '啾啾档案下累计' },
      { label: '本月消费', value: '￥318', note: '主要为粮食和玩具' },
      { label: '附近鸟友', value: '12位', note: '同城小太阳用户较多' },
    ],
    panels: [
      {
        title: '社区功能',
        items: ['发布问题帖子并获得回复', '按地域推荐附近用户帖子', '用户之间可以交好友'],
      },
      {
        title: '用品筛选',
        items: ['筛选靠谱鹦鹉用品', '跳转购买链接', '评论真实好用程度'],
      },
      {
        title: '记账本',
        items: ['记录每一条消费', '按宠物档案区分', '用户界面显示总花费和明细'],
      },
    ],
  },
  '/care-handbook': {
    kind: 'generic',
    title: '饲养百科',
    theme: 'lavender',
    visual: 'handbook',
    intro: '面向新手和准备养鹦鹉的人，提供教程、食物安全查询和拍照识鸟。',
    filters: ['新手', '剪羽', '药浴', '食物安全', '识图搜索'],
    actions: ['搜索教程', '上传图片', '查询食物'],
    highlights: [
      { label: '食物查询', value: '苹果可少量', note: '去籽，避免过量糖分' },
      { label: '热门教程', value: '剪羽护理', note: '含风险提示和替代方案' },
      { label: '识鸟结果', value: '虎皮鹦鹉', note: '支持野外拍照识别大型鹦鹉' },
    ],
    panels: [
      {
        title: '教程库',
        items: ['新手喂养、剪羽、药浴、清洁教程', '搜索和筛选功能', '支持识图搜索相关教程'],
      },
      {
        title: '食物安全',
        items: ['输入食物名称查询是否有害', '按水果、蔬菜、谷物等类别筛选', '给出喂食频率和注意事项'],
      },
      {
        title: '拍照识鸟',
        items: ['野外遇到鹦鹉可拍照识别', '展示种类介绍', '此功能允许识别大型鹦鹉'],
      },
    ],
  },
}
