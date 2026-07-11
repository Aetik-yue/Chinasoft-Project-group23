// 鹦鹉品种专属饲养配置
// 数据抽取自 知识库/parrot-knowledge-base.md，供「饲养手册 > 专属推荐」页使用。
// 覆盖 parrotSpeciesOptions 的全部 11 个品种；未录入品种走 GENERAL 兜底配置。
// 后续要补新品种，往 SPECIES_CARE_PROFILES 里加一条同名条目即可。

const TOXIC_FOODS = [
  '牛油果', '巧克力', '咖啡因（咖啡/茶）', '酒精', '洋葱', '大蒜', '蘑菇',
  '果核果籽', '苹果籽', '高盐食物', '高脂肪/油炸食品', '木糖醇', '生豆类', '乳制品',
]

// 禁忌食物的「为什么不能吃」说明，抽取自 知识库/parrot-knowledge-base.md。
// 模板里给毒物标签加 data-reason 悬浮提示用。
const TOXIC_FOOD_REASONS = {
  '牛油果': '含 Persin（果素），可致呼吸困难和心力衰竭，数小时内猝死，无特效解毒。果肉、果皮、果核、叶全株有毒。',
  '巧克力': '含可可碱，鹦鹉无法代谢，可致心律不齐、抽搐、死亡。可可含量越高毒性越强。',
  '咖啡因（咖啡/茶）': '咖啡因可致心律不齐、过度兴奋、死亡。咖啡、茶、可乐等任何含咖啡因饮品均禁止。',
  '酒精': '乙醇致中枢神经抑制、死亡，极少量即可致命。任何含酒精食物饮品均禁止。',
  '洋葱': '含硫代硫酸盐，可致溶血性贫血（红细胞破坏）。生熟均毒，少量长期累积也危险。',
  '大蒜': '含硫代硫酸盐，可致溶血性贫血。生熟均毒，禁止作为调味或食物。',
  '蘑菇': '某些蘑菇含多种毒素，可致肝肾损伤。为安全起见避免所有蘑菇。',
  '果核果籽': '核籽常含氰苷，在体内释放氰化物致中毒。喂水果务必去净核与籽。',
  '苹果籽': '籽含氰苷，释放氰化物致中毒。果肉安全但必须去籽。',
  '高盐食物': '高钠致钠中毒、肾损伤。人类零食（薯片、咸味坚果）禁止，鹦鹉盐需求极低。',
  '高脂肪/油炸食品': '高脂致脂肪肝和肥胖。油炸食物、肥肉禁止，种子为主食也属高脂风险。',
  '木糖醇': '对鹦鹉可致低血糖和肝衰竭。任何无糖加工食品禁止。',
  '生豆类': '含植物凝集素，可致消化系统损伤。必须充分煮熟后方可少量喂食。',
  '乳制品': '鹦鹉缺乏乳糖酶，无法消化乳糖，可致腹泻。避免牛奶、奶酪等乳制品。',
}

// 通用兜底配置：未录入专属方案的品种先用这套「中小型鹦鹉通用建议」，
// 并由 getSpeciesCareProfile 打上 fallback: true 标记，UI 顶部会提示。
const GENERAL_CARE_PROFILE = {
  name: '通用中小型鹦鹉',
  latin: '',
  origin: '—',
  bodyLength: '—',
  weight: '—',
  lifespan: '因品种而异',
  temperament: '参考下方关联教程与品种百科。',
  talkingAbility: '因品种而异',
  // 适宜环境区间（用于环境适配度评分对比）
  tempRange: [18, 28],      // ℃
  humidityRange: [40, 65],  // %
  dustLevel: '中等',
  dustTolerance: 'moderate', // tolerant | moderate | sensitive
  // 粉尘浓度阈值（ppm，与烟雾传感器单位一致）：低于 good 满分，good~warn 之间线性衰减，超过 warn 低分
  // 敏感品种（呼吸道敏感）按知识库建议下调到 25/60，其余用系统默认 35/80。
  dustThreshold: { good: 35, warn: 80 },
  risks: ['温度骤变应激', '空气污浊（粉尘/油烟/香薰）对鹦鹉可致命'],
  // 饮食配比（百分比，仅作展示用）
  dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
  recommendedFoods: ['颗粒饲料', '深绿叶菜（西兰花、少量菠菜）', '胡萝卜', '苹果（去籽去核）', '熟玉米', '蓝莓'],
  toxicFoods: TOXIC_FOODS,
  // 专属护理建议：针对该品种独特需求（替代对大教程库的直接关联）
  careAdvice: [
    { title: '易肥胖', text: '以颗粒饲料为主食，种子仅作训练零食，避免脂肪肝。' },
    { title: '社会性需求', text: '群居性极强，建议成对或成群饲养，避免孤独抑郁。' },
    { title: '环境刺激', text: '提供洗澡水盆与啃咬玩具，减少啄羽与无聊行为。' },
    { title: '饮食要点', text: '主食颗粒料，搭配深绿叶菜与少量水果，温度骤变时注意保温。' },
  ],
}

// 已录入的专属品种配置。key 与 parrotSpeciesOptions 里的中文品种名对齐。
const SPECIES_CARE_PROFILES = {
  虎皮: {
    name: '虎皮鹦鹉',
    latin: 'Melopsittacus undulatus',
    origin: '澳大利亚内陆',
    bodyLength: '约 18 cm',
    weight: '30–40 g',
    lifespan: '5–10 年（良好饲养可达 15 年）',
    temperament: '活泼好动、好奇心强、群居性极强，适合初次养鸟者；建议成对饲养。',
    talkingAbility: '★★★★☆（雄性可学会大量词汇）',
    tempRange: [18, 26],
    humidityRange: [40, 60],
    dustLevel: '少',
    dustTolerance: 'tolerant',
    dustThreshold: { good: 35, warn: 80 },
    risks: ['温度骤变应激', '种子为主食易肥胖', '体型小代谢快，拒食 24 小时即危险'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['小米', '稗子', '颗粒饲料', '绿叶菜', '苹果（去籽）', '胡萝卜'],
    careAdvice: [
      { title: '社会性需求', text: '群居性极强，建议成对或成群饲养，单独饲养需提供大量互动避免孤独。' },
      { title: '饮食要点', text: '主食颗粒料，控制种子比例防腐败肥胖，搭配绿叶菜与水果。' },
      { title: '温度敏感', text: '体型小代谢快，环境温度保持 18–26℃，避免骤变应激。' },
      { title: '丰容建议', text: '提供洗澡水盆与啃咬玩具，减少啄羽等无聊行为。' },
    ],
  },
  牡丹: {
    name: '牡丹鹦鹉',
    latin: 'Agapornis spp.',
    origin: '非洲和马达加斯加',
    bodyLength: '约 13–17 cm',
    weight: '40–60 g',
    lifespan: '10–20 年',
    temperament: '活泼好动、深情（成对饲养伴侣关系亲密）；单独饲养需更多互动，可能对其他鸟有攻击性。',
    talkingAbility: '★☆☆☆☆（极少学话，可学吹口哨）',
    tempRange: [20, 28],
    humidityRange: [40, 60],
    dustLevel: '少',
    dustTolerance: 'tolerant',
    dustThreshold: { good: 35, warn: 80 },
    risks: ['攻击性', '温差应激', '伴侣间易打斗'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['颗粒饲料', '小米', '苹果（去籽）', '西兰花', '胡萝卜', '蓝莓'],

    careAdvice: [
      { title: '攻击性管理', text: '配对关系亲密但可能对其他鸟有攻击性，合笼需逐步引导观察。' },
      { title: '温差防护', text: '对温差敏感，环境温度保持 20–28℃，避免冷风直吹。' },
      { title: '伴侣互动', text: '单独饲养需主人大量陪伴与互动，否则易抑郁啄羽。' },
      { title: '饮食要点', text: '主食颗粒料+新鲜蔬果，少量种子，成对活动促进食欲。' },
    ],
  },
  玄凤: {
    name: '玄凤鹦鹉',
    latin: 'Nymphicus hollandicus',
    origin: '澳大利亚',
    bodyLength: '约 30–33 cm',
    weight: '80–120 g',
    lifespan: '15–25 年',
    temperament: '温和亲人、喜欢被抚摸，冠羽表达情绪（竖起=警觉/兴奋，平放=放松）；适合家庭饲养。',
    talkingAbility: '★★★☆☆（雄性擅长吹口哨，学话不如虎皮）',
    tempRange: [18, 26],
    humidityRange: [40, 60],
    dustLevel: '多',
    dustTolerance: 'moderate',
    dustThreshold: { good: 35, warn: 80 },
    risks: ['呼吸道疾病', '羽粉过敏', '脂肪瘤', '雌性易蛋难产', '对空气质量敏感'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['颗粒饲料', '羽衣甘蓝', '胡萝卜', '西兰花', '苹果（去籽）', '熟红薯'],

    careAdvice: [
      { title: '羽粉管理', text: '羽粉量多，粉尘耐受中等，需定期通风保持空气质量。' },
      { title: '读情绪', text: '看冠羽位置判断情绪——竖起警觉、平放放松、后倾恐惧。' },
      { title: '雌性呵护', text: '雌性易蛋难产，注意补钙与控制产蛋频率。' },
      { title: '互动方式', text: '喜欢被挠头颈部，多口哨互动，可站肩上陪伴。' },
    ],
  },
  小太阳: {
    name: '绿颊锥尾鹦鹉',
    latin: 'Pyrrhura molinae',
    origin: '南美洲',
    bodyLength: '约 25 cm',
    weight: '60–80 g',
    lifespan: '15–25 年',
    temperament: '活泼亲人、爱玩、好奇，适合家庭饲养；叫声较大需考虑邻居。',
    talkingAbility: '★★☆☆☆（可学少量词汇）',
    tempRange: [20, 28],
    humidityRange: [50, 65],
    dustLevel: '中等',
    // 知识库明确：该品种呼吸道敏感，建议粉尘告警阈值下调到 25/60。
    dustTolerance: 'moderate',
    dustThreshold: { good: 25, warn: 60 },
    risks: ['呼吸道敏感', '叫声尖锐', '换羽期易烦躁'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['颗粒饲料', '西兰花', '胡萝卜', '苹果（去籽）', '熟玉米', '蓝莓'],

    careAdvice: [
      { title: '呼吸道敏感', text: '粉尘耐受敏感，环境粉尘需控制在 25ppm 以下，注意通风。' },
      { title: '叫声注意', text: '叫声尖锐较大，适合家庭但需考虑邻居，可通过训练降低音量。' },
      { title: '换羽期护理', text: '换羽期易烦躁，增加环境湿度与玩具丰容。' },
      { title: '饮食要点', text: '主食颗粒料 60%，搭配蔬菜 25%、水果 10%、坚果 5%，控制种子防肥胖。' },
    ],
  },
  和尚: {
    name: '和尚鹦鹉',
    latin: 'Myiopsitta monachus',
    origin: '南美洲',
    bodyLength: '约 29 cm',
    weight: '100–130 g',
    lifespan: '15–20 年',
    temperament: '聪明、社交性强，会建造复杂群巢；学话能力较强，适应力强。',
    talkingAbility: '★★★★☆（学话能力较强）',
    tempRange: [18, 28],
    humidityRange: [40, 60],
    dustLevel: '少',
    dustTolerance: 'tolerant',
    dustThreshold: { good: 35, warn: 80 },
    risks: ['肥胖（种子为主食）', '繁殖期攻击性', '过度鸣叫'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['颗粒饲料', '燕麦', '苹果（去籽）', '西兰花', '胡萝卜', '玉米'],

    careAdvice: [
      { title: '聪明爱玩', text: '聪明、社交性强、会筑巢，需大量精神刺激与互动游戏。' },
      { title: '学话训练', text: '学话能力较强，雄性尤佳，多重复训练可学不少词汇。' },
      { title: '饮食要点', text: '主食颗粒料+新鲜蔬果，蛋白质需求略高，补足维生素。' },
      { title: '繁殖注意', text: '繁殖期攻击性增强，注意观察伴侣关系与营养补充。' },
    ],
  },
  吸蜜: {
    name: '虹彩吸蜜鹦鹉',
    latin: 'Trichoglossus moluccanus',
    origin: '澳大利亚、新几内亚、印尼东部',
    bodyLength: '约 25–30 cm',
    weight: '100–180 g',
    lifespan: '15–25 年',
    temperament: '活泼好奇、爱玩；饮食特殊（花蜜、花粉、软果为主），粪便稀湿故清洁频率需更高。',
    talkingAbility: '★★★☆☆',
    tempRange: [22, 28],
    humidityRange: [50, 70],
    dustLevel: '极少',
    dustTolerance: 'sensitive',
    dustThreshold: { good: 25, warn: 60 },
    risks: ['喷射式稀便致羽毛污秽', '呼吸道敏感', '营养失衡', '对卫生要求高'],
    // 吸蜜类主食为花蜜，配比与常规鹦鹉不同。
    dietRate: { pellet: 30, veg: 20, fruit: 40, seed: 10 },
    recommendedFoods: ['花蜜/吸蜜粉（主食）', '香蕉', '木瓜', '芒果', '苹果（去籽）', '熟玉米'],
    extraToxicFoods: ['普通颗粒粮（吸蜜需专用花蜜配方，不可喂普通种子粮）'],
    careAdvice: [
      { title: '特殊饮食', text: '主食为花蜜/吸蜜粉+软果，粪便稀湿需每日清洁栖笼与食盆。' },
      { title: '卫生要求', text: '喷射式稀便易污染环境，不适合对卫生要求苛刻的家庭。' },
      { title: '饮食要点', text: '配比：花蜜主食 30%、软果 40%、蔬菜 20%、种子 10%。' },
      { title: '敏感呵护', text: '呼吸道敏感，避免油烟/香薰，保持通风。' },
    ],
  },
  凯克: {
    name: '凯克鹦鹉',
    latin: 'Pionites spp.',
    origin: '南美洲亚马逊流域',
    bodyLength: '约 23 cm',
    weight: '140–170 g',
    lifespan: '25–40 年',
    temperament: '极其活泼好动，以"跳跃"行为闻名；学话极少但擅长杂耍，精力旺盛。',
    talkingAbility: '★☆☆☆☆（学话极少，擅长杂耍）',
    tempRange: [22, 28],
    humidityRange: [50, 65],
    dustLevel: '少',
    // 知识库：粉尘耐受较敏感，建议粉尘告警阈值下调。
    dustTolerance: 'sensitive',
    dustThreshold: { good: 25, warn: 60 },
    risks: ['呼吸道敏感', '跳跃行为致撞伤', '粉尘敏感（建议下调告警阈值）'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['颗粒饲料', '葡萄', '苹果（去籽）', '胡萝卜', '西兰花', '香蕉'],

    careAdvice: [
      { title: '防撞伤', text: '以"跳跃"行为闻名，移除笼内尖锐栖杠，避免撞伤。' },
      { title: '精力释放', text: '极其活泼好动，需大量出笼活动时间与觅食玩具。' },
      { title: '粉尘敏感', text: '呼吸道敏感，建议粉尘阈值 25ppm，保持通风。' },
      { title: '饮食要点', text: '主食颗粒料+新鲜蔬果，少量坚果作训练奖励即可。' },
    ],
  },
  黑顶: {
    name: '黑顶吸蜜鹦鹉',
    latin: 'Lorius lory',
    origin: '新几内亚',
    bodyLength: '约 28 cm',
    weight: '150–200 g',
    lifespan: '15–25 年',
    temperament: '活泼、羽色鲜艳；饮食与虹彩吸蜜相同（花蜜为主），粪便稀湿。',
    talkingAbility: '★★★☆☆',
    tempRange: [22, 28],
    humidityRange: [50, 70],
    dustLevel: '极少',
    dustTolerance: 'sensitive',
    dustThreshold: { good: 25, warn: 60 },
    risks: ['湿便致羽毛污秽', '营养失衡', '急救时不可用普通颗粒粮（需花蜜配方）'],
    // 同虹彩吸蜜，主食为花蜜。
    dietRate: { pellet: 30, veg: 20, fruit: 40, seed: 10 },
    recommendedFoods: ['花蜜/吸蜜粉（主食）', '木瓜', '芒果', '香蕉', '软质水果', '熟玉米'],
    extraToxicFoods: ['普通颗粒粮（需专用花蜜配方）'],
    careAdvice: [
      { title: '特殊饮食', text: '主食花蜜/吸蜜粉+软果，喂食需用花蜜配方，不可喂普通颗粒粮。' },
      { title: '稀便护理', text: '粪便稀湿易污染羽毛，需更高频率清洁，注意卫生。' },
      { title: '急救注意', text: '急救灌食必须用花蜜配方，错误饮食会致命。' },
      { title: '饮食要点', text: '配比：花蜜 30%、软果 40%、蔬菜 20%、种子 10%，多补充花蜜水分。' },
    ],
  },
  折衷: {
    name: '折衷鹦鹉',
    latin: 'Eclectus roratus',
    origin: '新几内亚、澳洲北部、印尼',
    bodyLength: '约 35–42 cm',
    weight: '350–550 g',
    lifespan: '30–50 年',
    temperament: '相对独立、温和；雌雄羽色差异极大（雄绿雌红蓝），对湿度需求极高。',
    talkingAbility: '★★★★☆',
    tempRange: [24, 30],
    humidityRange: [60, 80],
    dustLevel: '少',
    dustTolerance: 'sensitive',
    dustThreshold: { good: 25, warn: 60 },
    risks: ['应激啄羽', '低湿致羽毛脆裂、皮肤脱屑', '急救时需维持 60% 以上湿度'],
    // 折衷需更高蔬果纤维。
    dietRate: { pellet: 50, veg: 30, fruit: 15, seed: 5 },
    recommendedFoods: ['颗粒饲料', '羽衣甘蓝', '胡萝卜', '芒果', '木瓜', '甜椒'],

    careAdvice: [
      { title: '高湿刚需', text: '对湿度需求极高（60–80%），干燥环境致羽毛脆裂、皮肤脱屑。' },
      { title: '雌雄异色', text: '雌雄羽色差异极大——雄绿雌红蓝，是鹦鹉中典型的雌雄异色。' },
      { title: '应激啄羽', text: '易应激啄羽，需稳定环境与充足互动，避免频繁更换笼舍。' },
      { title: '饮食要点', text: '配比：颗粒料 50%、蔬果 45%、种子 5%，需更多纤维预防肥胖。' },
    ],
  },
  裸胸: {
    name: '戈芬氏凤头鹦鹉（裸胸凤头）',
    latin: 'Cacatua goffiniana',
    origin: '印尼塔劳群岛',
    bodyLength: '约 32 cm',
    weight: '约 300 g',
    lifespan: '30–40 年',
    temperament: '好奇爱玩、聪明（研究显示具工具使用能力）；体型较小的凤头，但需求不减。',
    talkingAbility: '★★☆☆☆',
    tempRange: [22, 28],
    humidityRange: [50, 65],
    dustLevel: '多',
    dustTolerance: 'sensitive',
    dustThreshold: { good: 25, warn: 60 },
    risks: ['啄羽', '粉尘多', '需丰富精神刺激与啃咬玩具'],
    dietRate: { pellet: 55, veg: 25, fruit: 10, seed: 10 },
    recommendedFoods: ['颗粒饲料', '杏仁/核桃（少量）', '西兰花', '胡萝卜', '苹果（去籽）', '葡萄'],

    careAdvice: [
      { title: '凤头羽粉', text: '羽粉量多且敏感，需每日通风+空气净化器，勤打扫笼舍。' },
      { title: '聪明互动', text: '好奇心强、聪明（具工具使用能力），需提供解谜/觅食玩具。' },
      { title: '啄羽预防', text: '易啄羽/无聊，保证每日出笼活动与啃咬玩具。' },
      { title: '饮食要点', text: '主食颗粒料搭配新鲜蔬果，补充钙质与维生素 A。' },
    ],
  },
  金太阳: {
    name: '太阳锥尾鹦鹉（金太阳）',
    latin: 'Aratinga solstitialis',
    origin: '南美洲',
    bodyLength: '约 30 cm',
    weight: '120–140 g',
    lifespan: '15–25 年',
    temperament: '活泼、羽色鲜艳（橙黄绿色）；叫声极大（可达 100 分贝），需大量社交互动，不建议公寓饲养。',
    talkingAbility: '★★☆☆☆',
    tempRange: [20, 28],
    humidityRange: [50, 65],
    dustLevel: '中等',
    dustTolerance: 'tolerant',
    dustThreshold: { good: 35, warn: 80 },
    risks: ['叫声极大（可达 100 分贝以上）', '情绪化', '需大量社交否则易抑郁'],
    dietRate: { pellet: 60, veg: 25, fruit: 10, seed: 5 },
    recommendedFoods: ['颗粒饲料', '芒果', '木瓜', '胡萝卜', '西兰花', '蓝莓'],
    careAdvice: [
      { title: '噪音管理', text: '叫声极大可达 100 分贝以上，不建议公寓饲养，可通过训练与环境丰容降低音量。' },
      { title: '社交刚需', text: '需大量社交互动，孤独易抑郁啄羽，建议主人每日长时间陪伴。' },
      { title: '饮食要点', text: '主食颗粒料+芒果木瓜等软果，控制种子防肥胖。' },
      { title: '情绪呵护', text: '情绪化明显，避免突然环境变化引发应激啄羽。' },
    ],
  },
}

/**
 * 按品种名取专属饲养配置。
 * 未录入的品种（含「未录入」/空值）返回 GENERAL_CARE_PROFILE 并打 fallback:true。
 */
export function getSpeciesCareProfile(species) {
  const key = species && String(species).trim()
  if (key && SPECIES_CARE_PROFILES[key]) {
    const profile = SPECIES_CARE_PROFILES[key]
    // 禁忌食品 = 通用毒物 + 品种专属额外毒物（吸蜜类需专用配方，普粮反而成毒）
    const toxicFoods = profile.extraToxicFoods
      ? [...TOXIC_FOODS, ...profile.extraToxicFoods]
      : [...TOXIC_FOODS]
    return { ...profile, toxicFoods, fallback: false }
  }
  return { ...GENERAL_CARE_PROFILE, toxicFoods: [...TOXIC_FOODS], fallback: true }
}

export const SPECIES_CARE_AVAILABLE = Object.keys(SPECIES_CARE_PROFILES)

/**
 * 取某个禁忌食物的「为什么不能吃」说明。
 * 1. 优先查 TOXIC_FOOD_REASONS（知识库收录的通用毒物）；
 * 2. 品种专属额外禁忌（如吸蜜的「普通颗粒粮（需专用花蜜配方）」），
 *    名字里已用全角括号写明原因，提取括号内文字作为说明；
 * 3. 兜底返回通用提示。
 */
export function getToxicReason(food) {
  if (!food) return '对当前品种不适宜，请避免喂食。'
  if (TOXIC_FOOD_REASONS[food]) return TOXIC_FOOD_REASONS[food]
  const match = String(food).match(/（(.+?)）/);
  return match ? match[1] : '对当前品种不适宜，请避免喂食。'
}
