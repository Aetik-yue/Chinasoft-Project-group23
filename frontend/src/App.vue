<script setup>
import { computed, ref } from 'vue'
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
const account = ref({ ...userProfile })
const isSettingsEditing = ref(false)
const settingsDraft = ref({ ...userProfile })
const notificationEnabled = ref(true)
const permissionEnabled = ref(true)

const activeView = computed(() => detailViews[activeRoute.value])
const reportCurveSet = computed(() => reportCurveSets[activeReportRange.value] || reportCurveSets.月报)
const reportCurves = computed(() => reportCurveSet.value.curves)
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

function resetDetailState() {
  thirdView.value = ''
  petSwitchOpen.value = false
}

function handleOpen(entry) {
  lastOpenedRoute.value = entry.route
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
  openModal('curve', curve.label, { ...curve, xAxis: reportCurveSet.value.xAxis })
}

function openDustGauge(snapshot) {
  openModal('dust-gauge', '粉尘浓度仪表盘', snapshot)
}

function openDustDetail(snapshot) {
  openDustGauge(snapshot)
}

function openArchiveProfile(profile) {
  activeArchiveId.value = profile.id
  openThird(`archive:${profile.id}`)
}

function openWeightChart() {
  openModal('weight-chart', '体重记录曲线', selectedArchive.value)
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
    time: ledgerDraft.value.time || new Date().toISOString().slice(0, 10),
    createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    updatedAt: '',
    tag: ledgerDraft.value.tag || '其他',
    description: `${selectedParrot.value.shortName} · ${description}`,
    amount,
  })
  ledgerDraft.value = {
    time: new Date().toISOString().slice(0, 10),
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
    time: editingLedgerDraft.value.time || record.time,
    updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }),
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
    return
  }
  settingsDraft.value = { ...account.value }
  isSettingsEditing.value = true
}
</script>

<template>
  <main class="app-shell">
    <section v-if="!activeView" class="dashboard" aria-label="基于智慧烟感的宠物安全系统首页">
      <div class="column left-column">
        <EntryCard :card="entryCards.archive" size="archive" @open="handleOpen" />
        <EntryCard :card="entryCards.growth" size="growth" @open="handleOpen" />
      </div>

      <div class="column center-column">
        <div class="current-zone">
          <CurrentBirdCard :parrot="selectedParrot" @open="togglePetSwitch" />
          <section v-if="petSwitchOpen" class="pet-switch-panel" aria-label="宠物切换面板">
            <header>
              <h2>切换当前鹦鹉</h2>
              <button type="button" @click="openCreateProfile">新建档案</button>
            </header>
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
  :card="primaryCards.monitor"
  :device-id="selectedParrot.deviceId"
  @open="handleOpen"
  @dust-detail="openDustDetail"
/>
        <EntryCard :card="entryCards.ledger" size="ledger" @open="handleOpen" />
      </div>

      <div class="column right-column">
        <EntryCard :card="entryCards.settings" size="settings" @open="handleOpen" />
        <EntryCard :card="entryCards.medical" size="medical" @open="handleOpen" />
        <EntryCard :card="entryCards.handbook" size="handbook" @open="handleOpen" />
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
          <h1>{{ activeView.title }}</h1>
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
                v-for="range in ['日报', '周报', '月报']"
                :key="range"
                :class="{ active: activeReportRange === range }"
                type="button"
                @click="activeReportRange = range"
              >
                {{ range }}
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
              type="button"
              @click="openCurve(curve)"
            >
              <header>
                <h2>{{ curve.label }}</h2>
                <strong>{{ curve.value }}</strong>
              </header>
              <svg class="mini-line-chart" viewBox="0 0 260 92" aria-hidden="true">
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
          <article v-for="photo in photoRecords" :key="photo.title" class="photo-record-card">
            <span aria-hidden="true"></span>
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
          <article v-for="photo in photoRecords" :key="`archive-${photo.title}`" class="archive-photo-tile">
            <span aria-hidden="true"></span>
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
              <i v-for="point in [28, 35, 39, 48, 52, 57, 64]" :key="point" :style="{ height: `${point}%` }"></i>
            </div>
          </button>
          <button class="module-card archive-action-module" type="button" @click="openThird('archive-gallery')">
            <h2>成长相册</h2>
            <p>{{ selectedArchive.photos }}，截图和睡眠照片会自动归档。</p>
            <div class="photo-strip" aria-hidden="true"><span></span><span></span><span></span></div>
          </button>
          <article class="module-card weight-input-card">
            <h2>录入体重</h2>
            <label><span>今日体重</span><input value="78g" /></label>
            <button type="button" @click="openModal('archive', '体重已保存', selectedArchive)">保存</button>
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
            <input v-model="ledgerDraft.time" placeholder="时间：2026-07-04" />
            <input v-model="ledgerDraft.tag" placeholder="标签：主粮/医疗/用品" />
            <input v-model="ledgerDraft.description" placeholder="描述：玩具铃铛" />
            <input v-model="ledgerDraft.amount" placeholder="金额：29" />
            <button type="button" @click="addLedgerRecord">新增</button>
          </div>
          <article v-for="record in filteredLedgerRecords" :key="record.id" class="ledger-record-card">
            <template v-if="editingLedgerId === record.id && editingLedgerDraft">
              <input v-model="editingLedgerDraft.time" />
              <input v-model="editingLedgerDraft.tag" />
              <input v-model="editingLedgerDraft.description" />
              <input v-model="editingLedgerDraft.amount" />
              <button type="button" @click="saveLedgerRecord(record)">保存</button>
            </template>
            <template v-else>
              <span>{{ record.time }}</span>
              <small>创建 {{ record.createdAt }}</small>
              <strong>{{ record.tag }}</strong>
              <p>{{ record.description }}</p>
              <em>¥{{ record.amount }}</em>
              <i v-if="record.updatedAt">更新 {{ record.updatedAt }}</i>
              <button type="button" @click="startEditLedger(record)">编辑</button>
            </template>
          </article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'settings'">
        <section class="settings-page">
          <button class="settings-edit-button" type="button" @click="toggleSettingsEdit">
            {{ isSettingsEditing ? '保存' : '编辑' }}
          </button>
          <article class="settings-profile-card">
            <div class="settings-avatar-wrap">
              <span class="settings-avatar">
                <ParrotVisual :type="selectedAvatarParrot.avatarType" />
              </span>
              <select v-if="isSettingsEditing" v-model="settingsDraft.avatarParrotId" aria-label="选择头像鹦鹉">
                <option v-for="parrot in localParrots" :key="parrot.id" :value="parrot.id">{{ parrot.name }}</option>
              </select>
            </div>
            <label class="settings-name-row">
              <span>用户名</span>
              <input v-if="isSettingsEditing" v-model="settingsDraft.username" />
              <strong v-else>{{ account.username }}</strong>
            </label>
            <p class="settings-user-id">用户 ID：{{ account.userId }}</p>
            <p class="settings-location">位置信息：{{ account.location }}</p>
            <div class="settings-phone-row">
              <span>手机绑定</span>
              <button
                type="button"
                :class="{ active: settingsDraft.phoneBound }"
                :disabled="!isSettingsEditing"
                @click="settingsDraft.phoneBound = !settingsDraft.phoneBound"
              >
                {{ settingsDraft.phoneBound ? '已绑定' : '未绑定' }}
              </button>
            </div>
            <button class="settings-open-button" type="button" @click="openModal('setting-toggles', '设置')">设置</button>
          </article>
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
          <template v-else-if="modal.type === 'dust-gauge'">
            <div class="dust-gauge-panel">
              <div class="dust-gauge" :style="{ '--needle-angle': dustNeedleRotation(modal.item.dustValue) }">
                <div class="gauge-scale" aria-hidden="true">
                  <span class="tick tick-low">低</span>
                  <span class="tick tick-mid">中</span>
                  <span class="tick tick-high">高</span>
                </div>
                <span class="gauge-needle"></span>
                <span class="gauge-hub"></span>
              </div>
              <div class="dust-gauge-readout">
                <strong>{{ modal.item.dustValue }}{{ modal.item.dustUnit }}</strong>
                <span>当前程度：{{ dustGaugeLevel(modal.item.dustValue, modal.item.dustLevel) }}</span>
                <em>{{ modal.item.connected ? '已连接后端实时数据' : '后端未连接，当前为保底模拟值' }}</em>
              </div>
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
