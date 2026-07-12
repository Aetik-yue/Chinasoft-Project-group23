const zh = {
  common: {
    back: '返回', detailPage: '详情页', loading: '加载中…', saving: '保存中…', deleting: '删除中…',
    delete: '删除', confirmDelete: '确认删除', duration: '时长', scoreUnit: '分', sensorPending: '待接入',
  },
  titles: {
    reportPhotos: '成长照片', reportRecordings: '学舌录音', diagnosis: '智能问诊', hospitals: '附近医院',
    health: '健康分析', records: '病历', careProfile: '专属推荐', tutorials: '新手教程',
    tutorialDetail: '教程详情', birdId: '拍照识鹦鹉', archiveGallery: '宠物相册', archiveDetail: '档案详情',
  },
  report: {
    todayOverview: '今日概览', historyDateAria: '选择历史日期', viewReport: '查看报告', petSwitchAria: '报告鹦鹉切换',
    weekdays: ['日', '一', '二', '三', '四', '五', '六'], todayHealth: '📊 今日健康报告',
    healthComposite: '健康综合评分', healthExcellent: '棒极了！开心的鹦鹉在跳舞', healthGood: '状态不错，小太阳感到舒适',
    healthAttention: '环境不太完美，要多留意哦', healthDescription: '结合羽粉浓度、温湿度舒适区间以及宠物称重频率自动评估。',
    speciesEnvMatch: '🏡 专属环境适配', envComposite: '环境综合评分', envNoSensor: '未接入传感器数据',
    envExcellent: '优！环境配置非常理想', envGood: '良！环境基本适宜', envWarning: '警告！请及时调整环境',
    envDescription: '根据当前品种专属饲养方案对温湿度及粉尘浓度综合适配得出。', realtimeEnvAria: '实时环境监控',
    todayTrend: '📈 今日波动趋势', noTodayTrend: '暂无今日趋势数据', basedOnEnvWeight: '基于今日环境与体重稳定性',
    todayWeighed: '今日已称重', todayNotWeighed: '今日未称重', basedOnBehavior: '基于行为识别', weight: '体重',
    todayGrowthAria: '今日成长记录', photoMoments: '相册瞬间', photoMomentCount: '已捕捉 {n} 张日常瞬间',
    mimicAndCalls: '学舌与叫声', audioClipCount: '已录制 {n} 段音频片段', periodNoData: '该周期暂无数据',
    metricsAria: '报告关键指标', photoRecords: '照片记录', photoCount: '{n} 张照片', recordings: '录音', recordingCount: '{n} 段录音',
    voiceTitle: '🎙️ 叫声与学舌记录', voiceSummary: '已记录 {n} 条语音信息，支持在线回放。',
    voiceEmpty: '暂无语音记录。去“实时视频通话”对鹦鹉说话或录制吧！', play: '播放', pause: '暂停', deleteRecording: '删除此录音',
    selectDate: '选日期', selectWeek: '选周', selectMonth: '选月',
    dateHintDay: '选择要查看的日期：', dateHintWeek: '选择要查看的周（选该周任意一天）：', dateHintMonth: '选择要查看的月（选该月任意一天）：',
  },
  archive: {
    overviewTitle: '档案数据概览', profileCount: '已建档案', currentCare: '当前看护',
    overviewTip: '点击宠物卡片可快速修改资料、更换头像及删除档案。',
  },
  tutorial: {
    loading: '教程加载中…', notFound: '未找到教程内容', loadFailed: '教程加载失败',
    summaries: [
      '用循序渐进的 7 天计划帮助新到家的鹦鹉平稳适应。', '了解剪羽风险，并优先采用环境管理和召回训练。',
      '掌握药浴前后的保温、观察与恢复要点。', '按每日、每周、每月节奏完成笼舍清洁和消毒。',
      '用精神、排泄、羽毛和体重快速判断健康状态。', '在炎热与寒冷季节保持温度稳定并兼顾通风。',
      '识别绝不能喂的食物、安全替代品和误食处理方法。', '在换羽期补充营养、保湿并识别异常掉羽。',
      '从健康、环境、营养和行为四方面排查啄羽。', '搭配颗粒料、蔬果和少量种子，建立均衡食谱。',
      '选择合适笼舍、站杆和玩具，降低受伤风险。', '用正向强化训练上手、召回与回笼。',
      '准备安全外出笼，预防飞失、惊吓和温差风险。', '建立稳定光照与睡眠节奏，保证高质量休息。',
      '判断何时需要修剪指甲，并做好喙部日常护理。', '准备急救箱并掌握出血、中毒和中暑的应急原则。',
      '读懂开心、紧张、生气、生病和求偶信号。', '避开喂养、笼舍、剪羽和训练中的常见误区。',
      '正确管理饮水与补钙，避免常见营养错误。', '用全年护理日历安排每日、每周、每月任务。',
    ],
  },
  care: {
    dietLabels: { pellet: '颗粒料', veg: '蔬菜', fruit: '水果', seed: '种子坚果' },
    dustTolerance: { tolerant: '较耐受', moderate: '中等', sensitive: '敏感' },
    dustRange: '优 ≤{good} / 警 ≤{warn} {unit}',
  },
  birdId: {
    badge: '🤖 AI 实时扫描', scanner: '智能扫描舱', upload: '点击上传或拍照识别', formats: '支持 jpg、png 格式',
    reselect: '重新选择', analyzing: '⏳ 正在智能分析中...', recognize: '🔍 开启 AI 行为识别', guide: '💡 快速操作指南',
    step1: '拍照/上传', step1Text: '拍摄一张鹦鹉的清晰正面照，确保光线充足且无遮挡。',
    step2: '启动 AI 识别', step2Text: '点击“开启 AI 行为识别”，系统将利用千问视觉大模型分析品种和细微动作。',
    step3: '获取健康建议', step3Text: '识别完成后将自动弹出详细的品种百科和行为状态说明。',
    demoTitle: '✨ 示例照片快捷体验', demoSubtitle: '没有照片？点击下方示例即刻体验 AI 分析效果：',
    demos: [['小太阳 · 理羽', '放松舒适、整理羽毛'], ['玄凤 · 磨嘴啃咬', '探索环境、咬玩具'], ['和尚 · 蓬羽打盹', '休息睡眠、保持体温']],
  },
  ledger: {
    monthSpend: '本月支出', allRecordsHint: '当前宠物的全部记录', entryCount: '记账笔数', careRecordedHint: '每一笔照护都有记录',
    category: '分类', allCategories: '全部分类', addEntry: '记一笔', noMatchTitle: '没有找到匹配的消费记录', emptyTitle: '还没有消费记录',
    noMatchHint: '尝试更换关键词或分类再次搜索。', emptyHint: '记录宠物的第一笔照护支出吧。', firstEntry: '记下第一笔',
    createTitle: '记一笔支出', expenseDate: '支出日期', expenseCategory: '支出分类', expenseDescription: '支出说明', amount: '金额',
    descriptionExample: '例如：主粮补充装', saveRecord: '保存记录', irreversible: '删除后无法恢复',
    descriptionRequired: '请填写支出说明。', amountPositive: '金额必须大于 0。', profileRequired: '请先创建或选择宠物档案。',
    saved: '支出记录已保存', deleteTitle: '确认删除账本记录', unsyncedDelete: '该记录尚未同步到数据库，无法执行删除。',
    deleted: '账本记录已删除', unsyncedEdit: '该记录尚未同步到数据库，无法执行编辑。',
    categories: { 全部: '全部', 食物: '食物', 医疗: '医疗', 清洁: '清洁', 玩具: '玩具', 其他: '其他' },
    charts: { expense: '支出', categoryExpense: '分类支出', aria: '支出统计', trend: '支出趋势', last6Months: '最近 6 个月', currencyUnit: '单位：元', trendAria: '最近六个月支出柱状图', trendEmpty: '记录支出后显示趋势', categoryShare: '分类占比', destination: '支出去向', categoryCount: '共 5 类', categoryAria: '分类支出环形图', categoryEmpty: '暂无分类统计' },
  },
}

const en = {
  common: { back: 'Back', detailPage: 'details', loading: 'Loading…', saving: 'Saving…', deleting: 'Deleting…', delete: 'Delete', confirmDelete: 'Confirm delete', duration: 'Duration', scoreUnit: 'pts', sensorPending: 'Pending' },
  titles: { reportPhotos: 'Growth Photos', reportRecordings: 'Mimicry Recordings', diagnosis: 'Smart Triage', hospitals: 'Nearby Hospitals', health: 'Health Analysis', records: 'Medical Records', careProfile: 'Care Profile', tutorials: 'Beginner Tutorials', tutorialDetail: 'Tutorial Details', birdId: 'Parrot Photo ID', archiveGallery: 'Pet Album', archiveDetail: 'Profile Details' },
  report: {
    todayOverview: 'Today Overview', historyDateAria: 'Choose a history date', viewReport: 'View report', petSwitchAria: 'Switch report parrot', weekdays: ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'],
    todayHealth: '📊 Today’s Health Report', healthComposite: 'Overall Health Score', healthExcellent: 'Excellent! Your parrot is thriving', healthGood: 'Looking good and comfortable', healthAttention: 'The environment needs a little attention', healthDescription: 'Calculated from feather dust, temperature, humidity and weighing consistency.',
    speciesEnvMatch: '🏡 Species Environment Match', envComposite: 'Overall Environment Score', envNoSensor: 'No sensor data', envExcellent: 'Excellent environment', envGood: 'Good and generally suitable', envWarning: 'Warning — adjust the environment', envDescription: 'Calculated against the ideal temperature, humidity and dust range for this species.', realtimeEnvAria: 'Live environment monitoring', todayTrend: '📈 Today’s Trend', noTodayTrend: 'No trend data today',
    basedOnEnvWeight: 'Based on today’s environment and weight stability', todayWeighed: 'Weighed today', todayNotWeighed: 'Not weighed today', basedOnBehavior: 'Based on behavior recognition', weight: 'Weight', todayGrowthAria: 'Today’s growth records', photoMoments: 'Album Moments', photoMomentCount: '{n} daily moments captured', mimicAndCalls: 'Mimicry & Calls', audioClipCount: '{n} audio clips recorded', periodNoData: 'No data for this period', metricsAria: 'Report metrics', photoRecords: 'Photo Records', photoCount: '{n} photos', recordings: 'Recordings', recordingCount: '{n} recordings', voiceTitle: '🎙️ Calls & Mimicry', voiceSummary: '{n} voice records with online playback.', voiceEmpty: 'No voice records yet. Talk to or record your parrot in Live Video Call.', play: 'Play', pause: 'Pause', deleteRecording: 'Delete this recording', selectDate: 'Choose date', selectWeek: 'Choose week', selectMonth: 'Choose month', dateHintDay: 'Choose a date:', dateHintWeek: 'Choose a week (select any day in it):', dateHintMonth: 'Choose a month (select any day in it):',
  },
  archive: { overviewTitle: 'Profile Data Overview', profileCount: 'Profiles', currentCare: 'In Care', overviewTip: 'Click a pet card to edit details, change its avatar or delete the profile.' },
  tutorial: {
    loading: 'Loading tutorial…', notFound: 'Tutorial content not found', loadFailed: 'Failed to load tutorial',
    summaries: ['A calm seven-day plan for helping a newly arrived parrot settle in.','Understand trimming risks and prefer environment management and recall training.','Keep your parrot warm and monitored before and after a medicated bath.','Clean and disinfect the cage on daily, weekly and monthly rhythms.','Check energy, droppings, feathers and weight for early health signals.','Keep temperature stable while balancing ventilation in hot and cold seasons.','Know forbidden foods, safe alternatives and what to do after accidental ingestion.','Support nutrition and humidity during molting and spot abnormal feather loss.','Investigate plucking through health, environment, nutrition and behavior.','Build a balanced diet from pellets, vegetables, fruit and limited seeds.','Choose a safe cage, perches and toys to reduce injury risks.','Use positive reinforcement for step-up, recall and returning to the cage.','Prepare a safe travel cage and prevent escape, fright and temperature shock.','Set a consistent light and sleep routine for high-quality rest.','Know when nails need trimming and how to care for the beak.','Prepare a first-aid kit and learn the basics for bleeding, poisoning and heatstroke.','Read signs of happiness, fear, anger, illness and courtship.','Avoid common mistakes in feeding, housing, trimming and training.','Manage drinking water and calcium safely.','Use a year-round calendar for daily, weekly and monthly care.'],
  },
  care: { dietLabels: { pellet: 'Pellets', veg: 'Vegetables', fruit: 'Fruit', seed: 'Seeds & nuts' }, dustTolerance: { tolerant: 'Tolerant', moderate: 'Moderate', sensitive: 'Sensitive' }, dustRange: 'Good ≤{good} / Warn ≤{warn} {unit}' },
  birdId: { badge: '🤖 AI Realtime Scan', scanner: 'Smart Scanner', upload: 'Click to upload or take a photo', formats: 'Supports jpg and png', reselect: 'Choose again', analyzing: '⏳ Analyzing...', recognize: '🔍 Start AI Behavior ID', guide: '💡 Quick Guide', step1: 'Take/upload a photo', step1Text: 'Use a clear front-facing photo with good light and no obstruction.', step2: 'Start AI recognition', step2Text: 'The Qwen vision model analyzes species and subtle behavior.', step3: 'Get health advice', step3Text: 'Species facts and behavior guidance appear after recognition.', demoTitle: '✨ Try a Sample Photo', demoSubtitle: 'No photo? Choose a sample below:', demos: [['Sun conure · Preening','Relaxed and grooming feathers'],['Cockatiel · Chewing','Exploring and chewing a toy'],['Monk parakeet · Napping','Resting and keeping warm']] },
  ledger: { monthSpend: 'This Month', allRecordsHint: 'All records for the current pet', entryCount: 'Entries', careRecordedHint: 'Every care expense is recorded', category: 'Category', allCategories: 'All categories', addEntry: 'Add expense', noMatchTitle: 'No matching expenses', emptyTitle: 'No expenses yet', noMatchHint: 'Try another keyword or category.', emptyHint: 'Record your pet’s first care expense.', firstEntry: 'Add first expense', createTitle: 'Add an Expense', expenseDate: 'Expense date', expenseCategory: 'Category', expenseDescription: 'Description', amount: 'Amount', descriptionExample: 'e.g. pellet refill', saveRecord: 'Save record', irreversible: 'This cannot be undone', descriptionRequired: 'Enter an expense description.', amountPositive: 'Amount must be greater than 0.', profileRequired: 'Create or select a pet profile first.', saved: 'Expense saved', deleteTitle: 'Delete Ledger Entry', unsyncedDelete: 'This entry has not synced to the database and cannot be deleted.', deleted: 'Ledger entry deleted', unsyncedEdit: 'This entry has not synced to the database and cannot be edited.', categories: { 全部: 'All', 食物: 'Food', 医疗: 'Medical', 清洁: 'Cleaning', 玩具: 'Toys', 其他: 'Other' }, charts: { expense: 'Expense', categoryExpense: 'Category expense', aria: 'Expense statistics', trend: 'Expense Trend', last6Months: 'Last 6 months', currencyUnit: 'Unit: CNY', trendAria: 'Expense bar chart for the last six months', trendEmpty: 'Add expenses to show a trend', categoryShare: 'Category Share', destination: 'Where it went', categoryCount: '5 categories', categoryAria: 'Category expense doughnut chart', categoryEmpty: 'No category statistics' } },
}

const es = {
  common: { back: 'Volver', detailPage: 'detalles', loading: 'Cargando…', saving: 'Guardando…', deleting: 'Eliminando…', delete: 'Eliminar', confirmDelete: 'Confirmar eliminación', duration: 'Duración', scoreUnit: 'pts', sensorPending: 'Pendiente' },
  titles: { reportPhotos: 'Fotos de crecimiento', reportRecordings: 'Grabaciones', diagnosis: 'Consulta inteligente', hospitals: 'Hospitales cercanos', health: 'Análisis de salud', records: 'Historial médico', careProfile: 'Perfil de cuidado', tutorials: 'Tutoriales para principiantes', tutorialDetail: 'Detalle del tutorial', birdId: 'Identificar loro', archiveGallery: 'Álbum de mascota', archiveDetail: 'Detalle del perfil' },
  report: { todayOverview: 'Resumen de hoy', historyDateAria: 'Elegir fecha histórica', viewReport: 'Ver informe', petSwitchAria: 'Cambiar loro del informe', weekdays: ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'], todayHealth: '📊 Informe de salud de hoy', healthComposite: 'Puntuación general de salud', healthExcellent: '¡Excelente! Tu loro está muy bien', healthGood: 'Buen estado y cómodo', healthAttention: 'El entorno necesita atención', healthDescription: 'Calculado con polvo, temperatura, humedad y regularidad del pesaje.', speciesEnvMatch: '🏡 Adaptación del entorno', envComposite: 'Puntuación general del entorno', envNoSensor: 'Sin datos del sensor', envExcellent: 'Entorno excelente', envGood: 'Entorno adecuado', envWarning: 'Aviso: ajusta el entorno', envDescription: 'Calculado según los rangos ideales de la especie.', realtimeEnvAria: 'Monitoreo ambiental en vivo', todayTrend: '📈 Tendencia de hoy', noTodayTrend: 'Sin tendencia de hoy', basedOnEnvWeight: 'Basado en el entorno y la estabilidad del peso', todayWeighed: 'Pesado hoy', todayNotWeighed: 'Sin pesar hoy', basedOnBehavior: 'Basado en reconocimiento de conducta', weight: 'Peso', todayGrowthAria: 'Registros de crecimiento de hoy', photoMoments: 'Momentos del álbum', photoMomentCount: '{n} momentos capturados', mimicAndCalls: 'Imitación y llamadas', audioClipCount: '{n} clips grabados', periodNoData: 'Sin datos en este período', metricsAria: 'Métricas del informe', photoRecords: 'Fotos', photoCount: '{n} fotos', recordings: 'Grabaciones', recordingCount: '{n} grabaciones', voiceTitle: '🎙️ Llamadas e imitación', voiceSummary: '{n} grabaciones con reproducción en línea.', voiceEmpty: 'Aún no hay grabaciones. Habla o graba en Videollamada.', play: 'Reproducir', pause: 'Pausar', deleteRecording: 'Eliminar esta grabación', selectDate: 'Elegir fecha', selectWeek: 'Elegir semana', selectMonth: 'Elegir mes', dateHintDay: 'Elige una fecha:', dateHintWeek: 'Elige una semana (cualquier día):', dateHintMonth: 'Elige un mes (cualquier día):' },
  archive: { overviewTitle: 'Resumen de datos del perfil', profileCount: 'Perfiles', currentCare: 'En cuidado', overviewTip: 'Pulsa una mascota para editar, cambiar el avatar o eliminar el perfil.' },
  tutorial: { loading: 'Cargando tutorial…', notFound: 'Contenido no encontrado', loadFailed: 'Error al cargar el tutorial', summaries: ['Plan de siete días para que un loro recién llegado se adapte con calma.','Conoce los riesgos del corte y prioriza el manejo ambiental y el entrenamiento.','Mantén calor y observación antes y después del baño medicinal.','Limpia y desinfecta la jaula con rutinas diarias, semanales y mensuales.','Revisa energía, heces, plumas y peso para detectar problemas pronto.','Mantén temperatura estable y ventilación en verano e invierno.','Reconoce alimentos prohibidos, alternativas y primeros pasos tras una ingestión.','Apoya nutrición y humedad durante la muda y detecta pérdida anormal.','Investiga el picaje desde salud, entorno, nutrición y conducta.','Combina pellets, verduras, fruta y pocas semillas.','Elige jaula, perchas y juguetes seguros.','Usa refuerzo positivo para subir, volver y entrar en la jaula.','Prepara un transportín seguro y evita escapes, sustos y cambios térmicos.','Crea una rutina estable de luz y sueño.','Aprende cuándo cortar uñas y cómo cuidar el pico.','Prepara un botiquín y aprende lo básico para sangrado, intoxicación y calor.','Lee señales de alegría, miedo, enfado, enfermedad y cortejo.','Evita errores comunes de comida, jaula, corte y entrenamiento.','Gestiona agua y calcio con seguridad.','Organiza el cuidado diario, semanal y mensual durante todo el año.'] },
  care: { dietLabels: { pellet: 'Pellets', veg: 'Verduras', fruit: 'Fruta', seed: 'Semillas y nueces' }, dustTolerance: { tolerant: 'Tolerante', moderate: 'Moderada', sensitive: 'Sensible' }, dustRange: 'Bien ≤{good} / Aviso ≤{warn} {unit}' },
  birdId: { badge: '🤖 Escaneo IA en vivo', scanner: 'Escáner inteligente', upload: 'Pulsa para subir o tomar una foto', formats: 'Admite jpg y png', reselect: 'Elegir otra', analyzing: '⏳ Analizando...', recognize: '🔍 Iniciar identificación IA', guide: '💡 Guía rápida', step1: 'Foto/subida', step1Text: 'Usa una foto frontal clara, con buena luz y sin obstáculos.', step2: 'Iniciar IA', step2Text: 'El modelo visual Qwen analiza especie y conducta.', step3: 'Consejos de salud', step3Text: 'Tras identificar verás información y consejos de conducta.', demoTitle: '✨ Probar una foto', demoSubtitle: '¿Sin foto? Elige una muestra:', demos: [['Cotorra sol · Acicalado','Relajada y ordenando plumas'],['Ninfa · Mordisqueo','Explora y muerde un juguete'],['Cotorra monje · Siesta','Descansa y conserva calor']] },
  ledger: { monthSpend: 'Gasto del mes', allRecordsHint: 'Todos los registros de la mascota actual', entryCount: 'Movimientos', careRecordedHint: 'Cada gasto de cuidado queda registrado', category: 'Categoría', allCategories: 'Todas', addEntry: 'Añadir gasto', noMatchTitle: 'No hay gastos coincidentes', emptyTitle: 'Aún no hay gastos', noMatchHint: 'Prueba otra palabra o categoría.', emptyHint: 'Registra el primer gasto de tu mascota.', firstEntry: 'Añadir primero', createTitle: 'Añadir un gasto', expenseDate: 'Fecha', expenseCategory: 'Categoría', expenseDescription: 'Descripción', amount: 'Importe', descriptionExample: 'p. ej. recarga de alimento', saveRecord: 'Guardar', irreversible: 'No se puede deshacer', descriptionRequired: 'Escribe una descripción.', amountPositive: 'El importe debe ser mayor que 0.', profileRequired: 'Crea o selecciona un perfil primero.', saved: 'Gasto guardado', deleteTitle: 'Eliminar movimiento', unsyncedDelete: 'El registro no está sincronizado y no se puede eliminar.', deleted: 'Movimiento eliminado', unsyncedEdit: 'El registro no está sincronizado y no se puede editar.', categories: { 全部: 'Todo', 食物: 'Alimento', 医疗: 'Médico', 清洁: 'Limpieza', 玩具: 'Juguetes', 其他: 'Otro' }, charts: { expense: 'Gasto', categoryExpense: 'Gasto por categoría', aria: 'Estadísticas de gastos', trend: 'Tendencia', last6Months: 'Últimos 6 meses', currencyUnit: 'Unidad: CNY', trendAria: 'Gráfico de gastos de seis meses', trendEmpty: 'Añade gastos para ver la tendencia', categoryShare: 'Por categoría', destination: 'Destino del gasto', categoryCount: '5 categorías', categoryAria: 'Gráfico circular por categoría', categoryEmpty: 'Sin estadísticas' } },
}

const ja = {
  common: { back: '戻る', detailPage: '詳細ページ', loading: '読み込み中…', saving: '保存中…', deleting: '削除中…', delete: '削除', confirmDelete: '削除を確認', duration: '長さ', scoreUnit: '点', sensorPending: '未接続' },
  titles: { reportPhotos: '成長写真', reportRecordings: 'ものまね録音', diagnosis: 'スマート問診', hospitals: '近くの病院', health: '健康分析', records: 'カルテ', careProfile: '専用推奨', tutorials: '初心者チュートリアル', tutorialDetail: 'チュートリアル詳細', birdId: 'インコ写真識別', archiveGallery: 'ペットアルバム', archiveDetail: '記録詳細' },
  report: { todayOverview: '今日の概要', historyDateAria: '履歴日を選択', viewReport: 'レポートを見る', petSwitchAria: 'レポートのインコを切替', weekdays: ['日','月','火','水','木','金','土'], todayHealth: '📊 今日の健康レポート', healthComposite: '総合健康スコア', healthExcellent: '素晴らしい！元気に過ごしています', healthGood: '良い状態で快適です', healthAttention: '環境を少し見直しましょう', healthDescription: '羽粉、温湿度、体重測定の安定性から評価します。', speciesEnvMatch: '🏡 種類別環境適合', envComposite: '総合環境スコア', envNoSensor: 'センサーデータなし', envExcellent: '理想的な環境です', envGood: '概ね適した環境です', envWarning: '注意：環境を調整してください', envDescription: 'この種類の適温・適湿・粉塵範囲から算出します。', realtimeEnvAria: 'リアルタイム環境監視', todayTrend: '📈 今日の変動', noTodayTrend: '今日の傾向データなし', basedOnEnvWeight: '今日の環境と体重安定性に基づく', todayWeighed: '本日計量済み', todayNotWeighed: '本日未計量', basedOnBehavior: '行動識別に基づく', weight: '体重', todayGrowthAria: '今日の成長記録', photoMoments: 'アルバムの瞬間', photoMomentCount: '日常写真 {n} 枚', mimicAndCalls: 'ものまね・鳴き声', audioClipCount: '音声 {n} 本', periodNoData: 'この期間のデータなし', metricsAria: 'レポート指標', photoRecords: '写真記録', photoCount: '写真 {n} 枚', recordings: '録音', recordingCount: '録音 {n} 本', voiceTitle: '🎙️ 鳴き声・ものまね記録', voiceSummary: '音声記録 {n} 件、オンライン再生対応。', voiceEmpty: '音声記録はありません。ライブ通話で話しかけるか録音してください。', play: '再生', pause: '一時停止', deleteRecording: 'この録音を削除', selectDate: '日付を選択', selectWeek: '週を選択', selectMonth: '月を選択', dateHintDay: '表示する日付を選択：', dateHintWeek: '表示する週を選択（任意の日）：', dateHintMonth: '表示する月を選択（任意の日）：' },
  archive: { overviewTitle: '記録データ概要', profileCount: '登録数', currentCare: '現在の見守り', overviewTip: 'ペットカードを選ぶと編集、アバター変更、削除ができます。' },
  tutorial: { loading: 'チュートリアル読み込み中…', notFound: '内容が見つかりません', loadFailed: '読み込みに失敗しました', summaries: ['お迎え後7日間を落ち着いて過ごすための段階的プラン。','羽切りのリスクを知り、環境管理と呼び戻し訓練を優先します。','薬浴前後の保温と観察ポイント。','毎日・毎週・毎月の清掃と消毒リズム。','元気、排泄、羽、体重から健康を素早く確認。','暑さ・寒さの中で温度と換気を安定させます。','禁止食品、安全な代替、誤食時の初動を確認。','換羽期の栄養と湿度、異常な羽抜けの見分け方。','健康・環境・栄養・行動から抜羽を調べます。','ペレット、野菜、果物、少量の種で食事を整えます。','安全なケージ、止まり木、おもちゃの選び方。','正の強化でステップアップ、呼び戻し、帰巣を練習。','安全なキャリーで逃走、驚き、温度差を防ぎます。','安定した照明と睡眠リズムを作ります。','爪切りの目安とくちばしの日常ケア。','救急箱と出血、中毒、熱中症の初期対応。','喜び、恐れ、怒り、病気、求愛のサイン。','食事、ケージ、羽切り、訓練のよくある誤り。','飲水とカルシウムを安全に管理します。','年間カレンダーで毎日・毎週・毎月のケアを整理。'] },
  care: { dietLabels: { pellet: 'ペレット', veg: '野菜', fruit: '果物', seed: '種・ナッツ' }, dustTolerance: { tolerant: '比較的強い', moderate: '中程度', sensitive: '敏感' }, dustRange: '良好 ≤{good} / 警告 ≤{warn} {unit}' },
  birdId: { badge: '🤖 AIリアルタイムスキャン', scanner: 'スマートスキャナー', upload: 'クリックして写真を選択・撮影', formats: 'jpg、png 対応', reselect: '選び直す', analyzing: '⏳ AI分析中...', recognize: '🔍 AI行動識別を開始', guide: '💡 クイックガイド', step1: '撮影・アップロード', step1Text: '明るく遮る物のない正面写真を用意します。', step2: 'AI識別を開始', step2Text: 'Qwen視覚モデルが種類と細かな行動を分析します。', step3: '健康アドバイス', step3Text: '識別後に種類情報と行動説明を表示します。', demoTitle: '✨ サンプル写真で体験', demoSubtitle: '写真がない場合はサンプルを選択：', demos: [['コガネメキシコ · 羽づくろい','リラックスして羽を整える'],['オカメインコ · かじる','環境を探索しおもちゃをかじる'],['オキナインコ · うたた寝','休息して体温を保つ']] },
  ledger: { monthSpend: '今月の支出', allRecordsHint: '現在のペットの全記録', entryCount: '記録件数', careRecordedHint: 'すべてのケア支出を記録', category: '分類', allCategories: '全分類', addEntry: '支出を記録', noMatchTitle: '該当する支出なし', emptyTitle: '支出記録はありません', noMatchHint: 'キーワードまたは分類を変えてください。', emptyHint: '最初のケア支出を記録しましょう。', firstEntry: '最初の支出を記録', createTitle: '支出を記録', expenseDate: '支出日', expenseCategory: '分類', expenseDescription: '説明', amount: '金額', descriptionExample: '例：主食補充パック', saveRecord: '記録を保存', irreversible: '削除後は元に戻せません', descriptionRequired: '支出説明を入力してください。', amountPositive: '金額は0より大きくしてください。', profileRequired: '先にペット記録を作成または選択してください。', saved: '支出を保存しました', deleteTitle: '家計簿記録を削除', unsyncedDelete: '未同期のため削除できません。', deleted: '家計簿記録を削除しました', unsyncedEdit: '未同期のため編集できません。', categories: { 全部: 'すべて', 食物: '食べ物', 医疗: '医療', 清洁: '清掃', 玩具: 'おもちゃ', 其他: 'その他' }, charts: { expense: '支出', categoryExpense: '分類別支出', aria: '支出統計', trend: '支出傾向', last6Months: '過去6か月', currencyUnit: '単位：元', trendAria: '過去6か月の支出棒グラフ', trendEmpty: '支出を記録すると傾向を表示', categoryShare: '分類別割合', destination: '支出先', categoryCount: '全5分類', categoryAria: '分類別支出ドーナツグラフ', categoryEmpty: '分類統計なし' } },
}

const birdDemoResults = {
  zh: [
    { species: '绿颊锥尾鹦鹉（小太阳）', behavior: '理羽：当前感到安全、放松，正在梳理羽毛并涂抹尾脂腺油脂。' },
    { species: '玄凤鹦鹉（鸡尾鹦鹉）', behavior: '磨嘴/啃咬：正在探索环境和磨砺喙部，建议提供安全咬木玩具。' },
    { species: '和尚鹦鹉', behavior: '蓬羽打盹：处于放松休息状态，膨起身体羽毛以保持体温。' },
  ],
  en: [
    { species: 'Green-cheeked conure', behavior: 'Preening: relaxed and safe, grooming feathers and distributing preen oil.' },
    { species: 'Cockatiel', behavior: 'Chewing: exploring and maintaining the beak; provide a safe wooden toy.' },
    { species: 'Monk parakeet', behavior: 'Napping: resting with fluffed feathers to retain body heat.' },
  ],
  es: [
    { species: 'Cotorra de mejillas verdes', behavior: 'Acicalado: está relajada y segura, arreglando las plumas y distribuyendo aceite.' },
    { species: 'Ninfa', behavior: 'Mordisqueo: explora y cuida el pico; ofrece un juguete de madera seguro.' },
    { species: 'Cotorra monje', behavior: 'Siesta: descansa con las plumas infladas para conservar calor.' },
  ],
  ja: [
    { species: 'ホオミドリアカオウロコインコ', behavior: '羽づくろい：安心してリラックスし、羽を整えて尾脂を広げています。' },
    { species: 'オカメインコ', behavior: 'かじる：環境を探索し、くちばしを整えています。安全な木製玩具を用意してください。' },
    { species: 'オキナインコ', behavior: 'うたた寝：羽を膨らませ、体温を保ちながら休んでいます。' },
  ],
}

for (const [language, results] of Object.entries(birdDemoResults)) {
  ;({ zh, en, es, ja })[language].birdId.demoResults = results
}

const hospitalCopy = {
  zh: {
    mapAria: '附近医院地图',
    searchPlaceholder: '搜索医院名称/地址...',
    noResults: '未找到匹配的医院',
    addressUnavailable: '地址详见地图',
    phoneUnavailable: '暂无电话',
    phone: '联系电话：',
    websiteBooking: '访问官方网站 / 挂号预约',
  },
  en: {
    mapAria: 'Nearby hospitals map',
    searchPlaceholder: 'Search hospital name or address...',
    noResults: 'No matching hospitals found',
    addressUnavailable: 'See the map for the address',
    phoneUnavailable: 'No phone available',
    phone: 'Phone:',
    websiteBooking: 'Visit website / Book an appointment',
  },
  es: {
    mapAria: 'Mapa de hospitales cercanos',
    searchPlaceholder: 'Buscar hospital o dirección...',
    noResults: 'No se encontraron hospitales',
    addressUnavailable: 'Consulta la dirección en el mapa',
    phoneUnavailable: 'Teléfono no disponible',
    phone: 'Teléfono:',
    websiteBooking: 'Visitar sitio web / Pedir cita',
  },
  ja: {
    mapAria: '近くの病院マップ',
    searchPlaceholder: '病院名または住所を検索...',
    noResults: '一致する病院が見つかりません',
    addressUnavailable: '住所は地図でご確認ください',
    phoneUnavailable: '電話番号なし',
    phone: '電話番号：',
    websiteBooking: '公式サイトを見る / 予約する',
  },
}

for (const [language, copy] of Object.entries(hospitalCopy)) {
  ;({ zh, en, es, ja })[language].hospital = copy
}

const tutorialA11yCopy = {
  zh: '回到顶部',
  en: 'Back to top',
  es: 'Volver arriba',
  ja: 'ページ上部へ戻る',
}

for (const [language, backToTop] of Object.entries(tutorialA11yCopy)) {
  ;({ zh, en, es, ja })[language].tutorial.backToTop = backToTop
}

const reportActionCopy = {
  zh: {
    audioEmpty: '音频数据为空，无法播放',
    audioSourceUnavailable: '无可用的音频源',
    deleteRecordingConfirm: '确定要删除这段录音吗？',
    recordingDeleted: '录音已删除',
    deleteRecordingFailed: '删除失败：{message}',
  },
  en: {
    audioEmpty: 'This recording has no audio data and cannot be played.',
    audioSourceUnavailable: 'No playable audio source is available.',
    deleteRecordingConfirm: 'Delete this recording?',
    recordingDeleted: 'Recording deleted',
    deleteRecordingFailed: 'Delete failed: {message}',
  },
  es: {
    audioEmpty: 'La grabación no contiene audio y no se puede reproducir.',
    audioSourceUnavailable: 'No hay una fuente de audio disponible.',
    deleteRecordingConfirm: '¿Eliminar esta grabación?',
    recordingDeleted: 'Grabación eliminada',
    deleteRecordingFailed: 'Error al eliminar: {message}',
  },
  ja: {
    audioEmpty: '音声データがないため再生できません。',
    audioSourceUnavailable: '再生できる音声がありません。',
    deleteRecordingConfirm: 'この録音を削除しますか？',
    recordingDeleted: '録音を削除しました',
    deleteRecordingFailed: '削除に失敗しました：{message}',
  },
}

for (const [language, copy] of Object.entries(reportActionCopy)) {
  Object.assign(({ zh, en, es, ja })[language].report, copy)
}

const reportDetailCopy = {
  zh: {
    archiveRecordings: '成长音频', selectedReportPeriod: '所选报告周期', currentParrot: '当前鹦鹉',
    reportDeviceSummary: '统计设备 {device} 的环境、体重与行为识别数据', collecting: '统计中', completed: '已完成',
    reportConclusionAria: '报告综合结论', healthOverallScore: '健康综合评分', fullScore: '满分 100', periodConclusion: '本周期结论',
    overallGood: '整体状态良好', continueObserve: '建议持续观察', needsAttention: '需要及时关注', reportMetricsAria: '报告关键指标',
    latestRecord: '最近一次记录', countUnit: '次', continuousEvent: '连续行为事件', behaviorSummary: '行为识别摘要',
    behaviorSummaryText: '共识别 {records} 条记录，合并为 {events} 次连续行为，同类行为 30 秒内合并。',
    loadingTrend: '正在加载趋势数据…', noTrendBehavior: '该周期暂无环境趋势数据，行为统计仍可正常查看。',
    environmentAndHealth: '环境与健康', periodTrend: '周期趋势', clickChart: '点击图表可查看完整数据', growthArchive: '成长档案', periodRecords: '周期记录',
    conclusionNoData: '当前周期暂无足够数据，报告将在监测数据写入后自动更新。',
    conclusionGoodText: '本周期环境整体适宜，共记录 {events} 次连续行为，鹦鹉状态较稳定。',
    conclusionObserveText: '本周期整体状态一般，共记录 {events} 次连续行为，建议继续关注环境波动。',
    conclusionAttentionText: '本周期环境指标存在明显波动，共记录 {events} 次连续行为，建议及时检查笼舍环境。',
  },
  en: {
    archiveRecordings: 'Growth Audio', selectedReportPeriod: 'Selected report period', currentParrot: 'Current parrot',
    reportDeviceSummary: 'Environment, weight and behavior data for device {device}', collecting: 'Collecting', completed: 'Completed',
    reportConclusionAria: 'Report summary', healthOverallScore: 'Overall health score', fullScore: 'Out of 100', periodConclusion: 'Period summary',
    overallGood: 'Overall status is good', continueObserve: 'Keep monitoring', needsAttention: 'Needs prompt attention', reportMetricsAria: 'Key report metrics',
    latestRecord: 'Latest record', countUnit: 'times', continuousEvent: 'Consecutive behavior events', behaviorSummary: 'Behavior recognition summary',
    behaviorSummaryText: '{records} records were merged into {events} consecutive events; matching behaviors within 30 seconds count as one event.',
    loadingTrend: 'Loading trend data…', noTrendBehavior: 'No environmental trend data for this period; behavior statistics remain available.',
    environmentAndHealth: 'Environment & Health', periodTrend: 'Period Trends', clickChart: 'Click a chart to view all data', growthArchive: 'Growth Archive', periodRecords: 'Period Records',
    conclusionNoData: 'There is not enough data for this period. The report will update automatically as monitoring data arrives.',
    conclusionGoodText: 'The environment was generally suitable, with {events} consecutive behavior events and stable overall condition.',
    conclusionObserveText: 'Overall condition was fair, with {events} consecutive behavior events. Keep watching environmental changes.',
    conclusionAttentionText: 'Environmental readings fluctuated noticeably, with {events} consecutive behavior events. Check the enclosure promptly.',
  },
  es: {
    archiveRecordings: 'Audio de crecimiento', selectedReportPeriod: 'Período seleccionado', currentParrot: 'Loro actual',
    reportDeviceSummary: 'Datos de entorno, peso y conducta del dispositivo {device}', collecting: 'En curso', completed: 'Completado',
    reportConclusionAria: 'Resumen del informe', healthOverallScore: 'Puntuación general de salud', fullScore: 'De 100', periodConclusion: 'Conclusión del período',
    overallGood: 'Estado general bueno', continueObserve: 'Conviene seguir observando', needsAttention: 'Requiere atención', reportMetricsAria: 'Indicadores clave del informe',
    latestRecord: 'Último registro', countUnit: 'veces', continuousEvent: 'Eventos de conducta consecutivos', behaviorSummary: 'Resumen de conducta',
    behaviorSummaryText: 'Se combinaron {records} registros en {events} eventos consecutivos; las conductas iguales dentro de 30 segundos cuentan como un evento.',
    loadingTrend: 'Cargando tendencias…', noTrendBehavior: 'No hay tendencia ambiental para este período; las estadísticas de conducta siguen disponibles.',
    environmentAndHealth: 'Entorno y salud', periodTrend: 'Tendencia del período', clickChart: 'Pulsa un gráfico para ver todos los datos', growthArchive: 'Archivo de crecimiento', periodRecords: 'Registros del período',
    conclusionNoData: 'No hay datos suficientes para este período. El informe se actualizará cuando lleguen datos de monitoreo.',
    conclusionGoodText: 'El entorno fue adecuado y se registraron {events} eventos de conducta consecutivos; el estado general es estable.',
    conclusionObserveText: 'El estado general fue aceptable y se registraron {events} eventos consecutivos. Conviene observar los cambios ambientales.',
    conclusionAttentionText: 'Los indicadores ambientales variaron notablemente y se registraron {events} eventos consecutivos. Revisa pronto el recinto.',
  },
  ja: {
    archiveRecordings: '成長音声', selectedReportPeriod: '選択したレポート期間', currentParrot: '現在のインコ',
    reportDeviceSummary: 'デバイス {device} の環境・体重・行動認識データ', collecting: '集計中', completed: '完了',
    reportConclusionAria: 'レポート総合所見', healthOverallScore: '総合健康スコア', fullScore: '100点満点', periodConclusion: '期間所見',
    overallGood: '全体的に良好です', continueObserve: '継続して観察してください', needsAttention: '早めの確認が必要です', reportMetricsAria: 'レポート主要指標',
    latestRecord: '最新記録', countUnit: '回', continuousEvent: '連続行動イベント', behaviorSummary: '行動認識サマリー',
    behaviorSummaryText: '{records} 件の記録を {events} 回の連続行動に統合しました。同じ行動は30秒以内なら1回として集計します。',
    loadingTrend: '傾向データを読み込み中…', noTrendBehavior: 'この期間の環境傾向データはありませんが、行動統計は確認できます。',
    environmentAndHealth: '環境と健康', periodTrend: '期間傾向', clickChart: 'グラフを選ぶと全データを確認できます', growthArchive: '成長記録', periodRecords: '期間記録',
    conclusionNoData: 'この期間は十分なデータがありません。監視データが届くと自動更新されます。',
    conclusionGoodText: 'この期間の環境は概ね適切で、連続行動を {events} 回記録しました。全体的に安定しています。',
    conclusionObserveText: 'この期間の状態は概ね普通で、連続行動を {events} 回記録しました。環境変化を引き続き観察してください。',
    conclusionAttentionText: 'この期間は環境指標の変動が大きく、連続行動を {events} 回記録しました。早めに飼育環境を確認してください。',
  },
}

for (const [language, copy] of Object.entries(reportDetailCopy)) {
  const target = ({ zh, en, es, ja })[language]
  target.titles.archiveRecordings = copy.archiveRecordings
  Object.assign(target.report, copy)
}

export const INTERFACE_COPY = { zh, en, es, ja }

export function getInterfaceCopy(language) {
  return INTERFACE_COPY[language] || INTERFACE_COPY.zh
}
