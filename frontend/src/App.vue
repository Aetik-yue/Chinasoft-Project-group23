<script setup>
import { computed, ref } from 'vue'
import CurrentBirdCard from './components/CurrentBirdCard.vue'
import EntryCard from './components/EntryCard.vue'
import MonitorCard from './components/MonitorCard.vue'
import ParrotVisual from './components/ParrotVisual.vue'
import {
  archiveCards,
  currentParrot,
  detailViews,
  entryCards,
  parrots,
  primaryCards,
  reportCurves,
  reportRecords,
  reportStats,
  userSettingCards,
} from './data/mockDashboard'

const activeRoute = ref('')
const lastOpenedRoute = ref('')
const selectedFilter = ref('')
const petSwitchOpen = ref(false)
const selectedParrot = ref(currentParrot)
const activeReportRange = ref('周报')
const modal = ref(null)

const activeView = computed(() => detailViews[activeRoute.value])

function handleOpen(entry) {
  lastOpenedRoute.value = entry.route
  petSwitchOpen.value = false

  if (entry.route === '/monitor') return
  if (entry.route?.startsWith('/monitor/records')) {
    activeRoute.value = '/growth-report'
    selectedFilter.value = '时间'
    activeReportRange.value = '周报'
    return
  }
  if (detailViews[entry.route]) {
    activeRoute.value = entry.route
    selectedFilter.value = detailViews[entry.route].filters[0]
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
  selectedFilter.value = ''
}

function openModal(type, title, item = null) {
  petSwitchOpen.value = false
  modal.value = { type, title, item }
}

function closeModal() {
  modal.value = null
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
      class="detail-shell"
      :class="[`detail-${activeView.theme}`, `detail-kind-${activeView.kind}`]"
      :aria-label="`${activeView.title}二级界面`"
    >
      <header class="detail-header">
        <button class="back-button" type="button" aria-label="返回首页" @click="goHome">
          <span aria-hidden="true"></span>
        </button>
        <div class="detail-title-block">
          <p>鹦鹉智能看护</p>
          <h1>{{ activeView.title }}</h1>
        </div>
        <div class="detail-avatar">
          <ParrotVisual :type="selectedParrot.avatarType" />
        </div>
      </header>

      <div class="detail-layout">
        <section class="detail-hero">
          <div class="detail-copy">
            <p>{{ selectedParrot.name }} · {{ selectedParrot.species }} · {{ selectedParrot.weight }}</p>
            <h2>{{ activeView.intro }}</h2>
          </div>
          <div class="detail-visual" aria-hidden="true">
            <ParrotVisual :type="activeView.visual" />
          </div>
        </section>

        <aside class="detail-side">
          <div class="filter-box">
            <h2>筛选</h2>
            <div class="filter-list">
              <button
                v-for="filter in activeView.filters"
                :key="filter"
                class="filter-pill"
                :class="{ active: selectedFilter === filter }"
                type="button"
                @click="selectedFilter = filter"
              >
                {{ filter }}
              </button>
            </div>
          </div>

          <div class="action-box">
            <h2>快捷操作</h2>
            <template v-if="activeView.kind === 'archive'">
              <button type="button" aria-label="新建鹦鹉档案" @click="openModal('archive', '新建鹦鹉档案')">新建档案</button>
              <button type="button" aria-label="编辑当前鹦鹉档案" @click="openModal('archive', '编辑当前档案', selectedParrot)">编辑档案</button>
              <button type="button" aria-label="删除当前鹦鹉档案" @click="openModal('delete', '删除档案', selectedParrot)">删除档案</button>
            </template>
            <template v-else-if="activeView.kind === 'settings'">
              <button type="button" aria-label="编辑通知设置" @click="openModal('settings', '编辑通知设置', userSettingCards[6])">通知设置</button>
              <button type="button" aria-label="编辑设备权限" @click="openModal('settings', '编辑设备权限', userSettingCards[7])">设备权限</button>
            </template>
            <template v-else>
              <button v-for="action in activeView.actions" :key="action" type="button">
                {{ action }}
              </button>
            </template>
          </div>
        </aside>

        <template v-if="activeView.kind === 'archive'">
          <section class="archive-grid" aria-label="宠物档案卡片">
            <article v-for="card in archiveCards" :key="card.key" class="edit-info-card">
              <span>{{ card.title }}</span>
              <strong>{{ card.value }}</strong>
              <p>{{ card.note }}</p>
              <button type="button" :aria-label="`编辑${card.title}`" @click="openModal('archive', `编辑${card.title}`, card)">编辑</button>
            </article>
          </section>
        </template>

        <template v-else-if="activeView.kind === 'report'">
          <section class="report-toolbar" aria-label="报告切换和筛选">
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
            <div class="report-search">
              <span>时间</span>
              <span>鹦鹉</span>
              <span>类型</span>
              <span>关键词</span>
            </div>
          </section>

          <section class="report-stat-grid" aria-label="报告关键指标">
            <article v-for="stat in reportStats" :key="stat.label" class="highlight-card">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ activeReportRange }}变化：{{ stat.trend }}</p>
            </article>
          </section>

          <section class="curve-grid" aria-label="曲线区域">
            <article v-for="curve in reportCurves" :key="curve.label" class="curve-card">
              <header>
                <h2>{{ curve.label }}</h2>
                <strong>{{ curve.value }}</strong>
              </header>
              <div class="mini-chart" aria-hidden="true">
                <i
                  v-for="(point, index) in curve.points"
                  :key="`${curve.label}-${index}`"
                  :style="{ height: `${point}%` }"
                ></i>
              </div>
            </article>
          </section>

          <section class="record-grid" aria-label="照片和录音记录">
            <article v-for="record in reportRecords" :key="record.type" class="module-card compact">
              <h2>{{ record.type }}</h2>
              <p>{{ record.value }}</p>
            </article>
          </section>
        </template>

        <template v-else-if="activeView.kind === 'settings'">
          <section class="settings-grid" aria-label="用户设置卡片">
            <article v-for="card in userSettingCards" :key="card.key" class="edit-info-card">
              <span>{{ card.title }}</span>
              <strong>{{ card.value }}</strong>
              <p>{{ card.note }}</p>
              <button type="button" :aria-label="`编辑${card.title}`" @click="openModal('settings', `编辑${card.title}`, card)">编辑</button>
            </article>
          </section>
        </template>

        <template v-else>
          <section class="highlight-row" aria-label="关键指标">
            <article v-for="item in activeView.highlights" :key="item.label" class="highlight-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.note }}</p>
            </article>
          </section>

          <section class="module-grid" aria-label="功能模块">
            <article v-for="panel in activeView.panels" :key="panel.title" class="module-card">
              <h2>{{ panel.title }}</h2>
              <ul>
                <li v-for="item in panel.items" :key="item">{{ item }}</li>
              </ul>
            </article>
          </section>
        </template>
      </div>
    </section>

    <div v-if="modal" class="modal-backdrop" role="presentation" @click.self="closeModal">
      <section class="edit-modal" role="dialog" aria-modal="true" :aria-label="modal.title">
        <header>
          <h2>{{ modal.title }}</h2>
          <button type="button" aria-label="关闭弹窗" @click="closeModal">×</button>
        </header>
        <div class="modal-body">
          <template v-if="modal.type === 'delete'">
            <p>确认删除 {{ modal.item?.name }} 的档案？删除后相关体重记录、相册和报告会被归档。</p>
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
          <button type="button" class="save-button" @click="closeModal">保存</button>
        </footer>
      </section>
    </div>
  </main>
</template>
