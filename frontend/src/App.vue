<script setup>
import { computed, ref } from 'vue'
import CurrentBirdCard from './components/CurrentBirdCard.vue'
import EntryCard from './components/EntryCard.vue'
import MonitorCard from './components/MonitorCard.vue'
import ParrotVisual from './components/ParrotVisual.vue'
import {
  archiveProfiles,
  communityModules,
  currentParrot,
  detailViews,
  entryCards,
  foodCategories,
  handbookModules,
  hospitalPins,
  medicalModules,
  parrots,
  primaryCards,
  reportCurves,
  reportRecords,
  reportStats,
  tutorialCards,
  userSettingCards,
} from './data/mockDashboard'

const activeRoute = ref('')
const thirdView = ref('')
const lastOpenedRoute = ref('')
const petSwitchOpen = ref(false)
const selectedParrot = ref(currentParrot)
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
const newLedgerRecord = ref('')
const medicalRecords = ref([
  '2026-07-01 羽粉偏高，通风后恢复',
  '2026-06-20 体重 77.5g，精神正常',
  '2026-06-02 药浴后保温 2 小时',
])
const ledgerRecords = ref([
  '啾啾 · 主粮补充装 · ¥88',
  '豆豆 · 磨爪站杆 · ¥36',
  '奶油 · 体检挂号 · ¥120',
])

const activeView = computed(() => detailViews[activeRoute.value])
const selectedArchive = computed(() => {
  const id = thirdView.value.replace('archive:', '')
  return archiveProfiles.find((profile) => profile.id === id) || archiveProfiles[0]
})
const filteredTutorials = computed(() => {
  const keyword = tutorialKeyword.value.trim()
  if (!keyword) return tutorialCards
  return tutorialCards.filter((item) => `${item.title}${item.tag}`.includes(keyword))
})
const filteredMedicalRecords = computed(() => {
  const keyword = medicalRecordSearch.value.trim()
  if (!keyword) return medicalRecords.value
  return medicalRecords.value.filter((item) => item.includes(keyword))
})
const filteredLedgerRecords = computed(() => {
  const keyword = ledgerKeyword.value.trim()
  if (!keyword) return ledgerRecords.value
  return ledgerRecords.value.filter((item) => item.includes(keyword))
})

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
  openModal('curve', curve.label, curve)
}

function addMedicalRecord() {
  const content = newMedicalRecord.value.trim()
  if (!content) return
  medicalRecords.value.unshift(`2026-07-03 ${content}`)
  newMedicalRecord.value = ''
}

function addLedgerRecord() {
  const content = newLedgerRecord.value.trim()
  if (!content) return
  ledgerRecords.value.unshift(`${selectedParrot.value.shortName} · ${content}`)
  newLedgerRecord.value = ''
}
</script>

<template>
  <main class="app-shell">
    <section v-if="!activeView" class="dashboard" aria-label="鹦鹉智能看护系统首页">
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
              <button type="button" @click="openModal('archive', '新建鹦鹉档案')">新建档案</button>
            </header>
            <button
              v-for="parrot in parrots"
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
        <MonitorCard :card="primaryCards.monitor" @open="handleOpen" />
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
          <p>{{ thirdView ? '三级界面' : '二级界面' }}</p>
          <h1>{{ activeView.title }}</h1>
        </div>
        <div class="detail-avatar">
          <ParrotVisual :type="selectedParrot.avatarType" />
        </div>
      </header>

      <template v-if="activeView.kind === 'report'">
        <section class="report-page">
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
              <button type="button" @click="togglePetSwitch">{{ selectedParrot.shortName }}</button>
              <section v-if="petSwitchOpen" class="report-pet-panel" aria-label="报告鹦鹉切换">
                <button v-for="parrot in parrots" :key="parrot.id" type="button" @click="selectParrot(parrot)">
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
              <div class="mini-chart" aria-hidden="true">
                <i
                  v-for="(point, index) in curve.points"
                  :key="`${curve.label}-${index}`"
                  :style="{ height: `${Math.max(18, Number(point))}%` }"
                ></i>
              </div>
            </button>
          </section>

          <section class="record-grid" aria-label="照片和录音记录">
            <article v-for="record in reportRecords" :key="record.type" class="module-card compact">
              <h2>{{ record.type }}</h2>
              <p>{{ record.value }}</p>
            </article>
          </section>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'archive'">
        <section v-if="!thirdView" class="archive-page">
          <div class="archive-actions">
            <button type="button" @click="openModal('archive', '新建鹦鹉档案')">增加档案</button>
          </div>
          <button
            v-for="profile in archiveProfiles"
            :key="profile.id"
            class="profile-card"
            type="button"
            @click="openThird(`archive:${profile.id}`)"
          >
            <span>基本资料</span>
            <strong>{{ profile.name }} · {{ profile.species }}</strong>
            <em>出生 {{ profile.birthday }} · {{ profile.weight }} · {{ profile.sex }} · {{ profile.status }}</em>
          </button>
        </section>

        <section v-else class="third-page archive-third">
          <article class="profile-card profile-card-large">
            <span>基本资料</span>
            <strong>{{ selectedArchive.name }} · {{ selectedArchive.species }}</strong>
            <em>出生 {{ selectedArchive.birthday }} · {{ selectedArchive.weight }} · {{ selectedArchive.sex }} · {{ selectedArchive.status }}</em>
            <button type="button" @click="openModal('archive', '编辑基本资料', selectedArchive)">编辑</button>
          </article>
          <article class="module-card">
            <h2>体重记录</h2>
            <p>{{ selectedArchive.lastWeight }}</p>
            <div class="large-line-chart" aria-hidden="true">
              <i v-for="point in [28, 35, 39, 48, 52, 57, 64]" :key="point" :style="{ height: `${point}%` }"></i>
            </div>
          </article>
          <article class="module-card">
            <h2>成长相册</h2>
            <p>{{ selectedArchive.photos }}，截图和睡眠照片会自动归档。</p>
            <div class="photo-strip" aria-hidden="true"><span></span><span></span><span></span></div>
          </article>
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
            <input v-model="newMedicalRecord" placeholder="填写一条病历或备忘录" />
            <button type="button" @click="addMedicalRecord">新增</button>
          </div>
          <article v-for="record in filteredMedicalRecords" :key="record" class="memo-card">{{ record }}</article>
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

      <template v-else-if="activeView.kind === 'community'">
        <section v-if="!thirdView" class="module-only-grid">
          <button v-for="module in communityModules" :key="module.key" class="module-card action-card" type="button" @click="openThird(module.key)">
            <h2>{{ module.title }}</h2>
            <p>{{ module.note }}</p>
          </button>
        </section>

        <section v-else-if="thirdView === 'ledger'" class="third-page records-page">
          <input v-model="ledgerKeyword" class="search-input" placeholder="搜索消费记录" />
          <div class="record-editor">
            <input v-model="newLedgerRecord" placeholder="例如：玩具铃铛 · ¥29" />
            <button type="button" @click="addLedgerRecord">新增</button>
          </div>
          <article v-for="record in filteredLedgerRecords" :key="record" class="memo-card">{{ record }}</article>
        </section>

        <section v-else class="third-page records-page">
          <input v-model="ledgerKeyword" class="search-input" :placeholder="thirdView === 'posts' ? '搜索附近帖子' : '搜索用品评价'" />
          <article class="memo-card">{{ thirdView === 'posts' ? '附近鸟友：小太阳最近换羽正常吗？' : '真实评价：低尘纸砂，适合小型鹦鹉笼底' }}</article>
          <article class="memo-card">{{ thirdView === 'posts' ? '上海鸟友线下交流：周末清洁笼具心得' : '用品清单：不锈钢食盆，易清洗，边缘圆润' }}</article>
        </section>
      </template>

      <template v-else-if="activeView.kind === 'settings'">
        <section class="settings-grid clean-settings" aria-label="用户设置卡片">
          <article v-for="card in userSettingCards" :key="card.key" class="edit-info-card">
            <span>{{ card.title }}</span>
            <strong>{{ card.value }}</strong>
            <p>{{ card.note }}</p>
            <button type="button" :aria-label="`编辑${card.title}`" @click="openModal('settings', `编辑${card.title}`, card)">编辑</button>
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
          <template v-if="modal.type === 'delete'">
            <p>确认删除 {{ modal.item?.name }} 的档案？删除后相关体重记录、相册和报告会被归档。</p>
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
          <template v-else-if="modal.type === 'curve'">
            <div class="detail-line-chart">
              <span class="axis-y">{{ modal.item.axis }} / {{ modal.item.unit }}</span>
              <div class="chart-area">
                <i
                  v-for="(point, index) in modal.item.points"
                  :key="`${modal.item.label}-detail-${index}`"
                  :style="{ height: `${Math.max(18, Number(point))}%` }"
                >
                  <b>{{ point }}</b>
                </i>
              </div>
              <span class="axis-x">7 天趋势</span>
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
          <button type="button" class="save-button" @click="closeModal">确定</button>
        </footer>
      </section>
    </div>
  </main>
</template>
