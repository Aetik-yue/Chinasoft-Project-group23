<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  records: { type: Array, default: () => [] },
  dark: { type: Boolean, default: false },
  types: { type: Array, default: () => [] },
  labels: { type: Object, default: () => ({}) },
})

const typeRef = ref(null)
const trendRef = ref(null)
let typeChart = null
let trendChart = null
let resizeObserver = null

// 健康评分规则：近 90 天内每类记录的权重 + 单类上限（防一类压垮总分）。
// symptom/diagnosis 扣分，medication 轻扣，recheck 加分（积极随访），other 微扣。
const SCORE_RULES = [
  { value: 'symptom', weight: -8, cap: -40 },
  { value: 'diagnosis', weight: -6, cap: -30 },
  { value: 'medication', weight: -3, cap: -15 },
  { value: 'recheck', weight: 3, cap: 12 },
  { value: 'other', weight: -1, cap: -5 },
]

function daysSince(dateStr) {
  if (!dateStr) return null
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return null
  return Math.max(0, Math.floor((Date.now() - d.getTime()) / 86400000))
}

const hasRecords = computed(() => props.records.length > 0)

const recent90 = computed(() => {
  const cutoff = Date.now() - 90 * 86400000
  return props.records.filter((r) => {
    const d = new Date(r.recordDate || '')
    return !Number.isNaN(d.getTime()) && d.getTime() >= cutoff
  })
})

const healthScore = computed(() => {
  const counts = { symptom: 0, diagnosis: 0, medication: 0, recheck: 0, other: 0 }
  recent90.value.forEach((r) => {
    const t = r.recordType || 'other'
    if (counts[t] != null) counts[t] += 1
  })
  let total = 100
  const breakdown = SCORE_RULES.map((rule) => {
    const cnt = counts[rule.value] || 0
    const contribution = rule.weight < 0
      ? Math.max(rule.weight * cnt, rule.cap)
      : Math.min(rule.weight * cnt, rule.cap)
    total += contribution
    return { value: rule.value, count: cnt, contribution }
  })
  total = Math.max(0, Math.min(100, Math.round(total)))
  const level = total >= 85 ? 0 : total >= 70 ? 1 : total >= 50 ? 2 : 3
  return { total, level, breakdown }
})

const maxBreakdownCount = computed(() => Math.max(1, ...healthScore.value.breakdown.map((b) => b.count)))

const typeDist = computed(() => {
  const counts = Object.fromEntries(props.types.map((t) => [t.value, 0]))
  props.records.forEach((r) => {
    const t = r.recordType || 'other'
    if (counts[t] != null) counts[t] += 1
  })
  return props.types
    .map((t) => ({ name: t.label, value: counts[t.value] || 0, color: t.color }))
    .filter((item) => item.value > 0)
})

function recentMonths(count = 6) {
  const now = new Date()
  return Array.from({ length: count }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (count - 1 - index), 1)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    return { key, label: `${String(date.getMonth() + 1).padStart(2, '0')}` }
  })
}

const monthlyTrend = computed(() => {
  const months = recentMonths()
  const counts = Object.fromEntries(months.map((m) => [m.key, 0]))
  props.records.forEach((r) => {
    const m = String(r.recordDate || '').slice(0, 7)
    if (counts[m] != null) counts[m] += 1
  })
  return months.map((m) => ({ ...m, value: counts[m.key] }))
})

const recentFive = computed(() => (
  [...props.records]
    .sort((a, b) => (b.recordDate || '').localeCompare(a.recordDate || ''))
    .slice(0, 5)
))

const analysis = computed(() => {
  const sorted = [...props.records].sort((a, b) => (b.recordDate || '').localeCompare(a.recordDate || ''))
  const last = sorted[0]
  const lastDays = last ? daysSince(last.recordDate) : null
  const counts = {}
  props.records.forEach((r) => {
    const t = r.recordType || 'other'
    counts[t] = (counts[t] || 0) + 1
  })
  let mostFrequent = null
  let max = 0
  Object.entries(counts).forEach(([t, c]) => {
    if (c > max) { max = c; mostFrequent = t }
  })
  const lastSymptom = sorted.find((r) => (r.recordType || '') === 'symptom')
  return {
    hasLast: !!last,
    lastDays,
    mostFrequentLabel: mostFrequent ? (props.types.find((t) => t.value === mostFrequent)?.label || '') : '',
    mostFrequentCount: max,
    lastSymptomText: lastSymptom ? (lastSymptom.content || '').slice(0, 40) : '',
  }
})

function typeLabel(value) {
  return props.types.find((t) => t.value === (value || 'other'))?.label || value || ''
}
function typeColor(value) {
  return props.types.find((t) => t.value === (value || 'other'))?.color || '#6f8a93'
}
function tagStyle(value) {
  const c = typeColor(value)
  return {
    color: c,
    borderColor: c,
    background: props.dark
      ? `color-mix(in srgb, ${c} 26%, transparent)`
      : `color-mix(in srgb, ${c} 14%, #ffffff)`,
  }
}
const LEVEL_KEYS = ['levelGood', 'levelFair', 'levelCaution', 'levelWarning']
const ADVICE_KEYS = ['advice0', 'advice1', 'advice2', 'advice3']
function levelLabel(level) { return props.labels[LEVEL_KEYS[level]] || '' }
function adviceText(level) { return props.labels[ADVICE_KEYS[level]] || '' }

function chartTextColor() { return props.dark ? '#d4e6ec' : '#1f3a44' }
function mutedTextColor() { return props.dark ? '#8fb0b8' : '#5a7a86' }

function renderTypeChart() {
  if (!typeRef.value) return
  typeChart ||= echarts.init(typeRef.value)
  if (!typeDist.value.length) { typeChart.clear(); return }
  typeChart.setOption({
    animationDuration: 500,
    color: typeDist.value.map((d) => d.color),
    tooltip: { trigger: 'item', formatter: ({ name, value, percent }) => `${name}<br/>${value} · ${percent}%` },
    legend: {
      type: 'scroll', orient: 'vertical', right: 6, top: 'center',
      itemWidth: 10, itemHeight: 10, textStyle: { color: chartTextColor(), fontWeight: 700 },
    },
    series: [{
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderColor: props.dark ? '#142228' : '#ffffff',
        borderWidth: 3,
        borderRadius: 5,
      },
      label: { show: false },
      emphasis: {
        scaleSize: 7,
        label: { show: true, color: chartTextColor(), fontWeight: 900, formatter: '{b}\n{d}%' },
      },
      data: typeDist.value,
    }],
  }, true)
}

function renderTrendChart() {
  if (!trendRef.value) return
  trendChart ||= echarts.init(trendRef.value)
  trendChart.setOption({
    animationDuration: 500,
    grid: { left: 18, right: 18, top: 26, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      formatter: (items) => `${items[0].axisValue}<br/>${props.labels.recordCount || ''}：${items[0].value}`,
    },
    xAxis: {
      type: 'category',
      data: monthlyTrend.value.map((m) => m.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: props.dark ? '#42506a' : '#cfdae1' } },
      axisLabel: { color: mutedTextColor(), fontWeight: 700 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: mutedTextColor() },
      splitLine: { lineStyle: { color: props.dark ? 'rgba(174,184,202,.13)' : 'rgba(84,139,177,.12)' } },
    },
    series: [{
      type: 'bar',
      data: monthlyTrend.value.map((m) => m.value),
      barMaxWidth: 34,
      itemStyle: { color: '#4a90d9', borderRadius: [7, 7, 2, 2] },
      emphasis: { itemStyle: { color: '#3a7fc4' } },
    }],
  }, true)
}

async function renderCharts() {
  await nextTick()
  renderTypeChart()
  renderTrendChart()
}

onMounted(() => {
  renderCharts()
  resizeObserver = new ResizeObserver(() => {
    typeChart?.resize()
    trendChart?.resize()
  })
  if (typeRef.value) resizeObserver.observe(typeRef.value)
  if (trendRef.value) resizeObserver.observe(trendRef.value)
})

watch(() => [props.records, props.dark, props.types, props.labels], renderCharts, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  typeChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <section class="med-charts">
    <!-- 健康评分卡（仿 env-score-card：大总分 + 等级 + 分项条） -->
    <article class="med-chart-panel med-score-card" :data-level="healthScore.level">
      <header>
        <div>
          <span>{{ labels.title }}</span>
          <strong>{{ labels.subtitle }}</strong>
        </div>
        <small>{{ labels.score }}</small>
      </header>
      <div v-if="hasRecords" class="med-score-total">
        <strong :data-level="healthScore.level">{{ healthScore.total }}</strong>
        <span>/100</span>
        <em class="med-score-level" :data-level="healthScore.level">{{ levelLabel(healthScore.level) }}</em>
      </div>
      <div v-else class="med-score-empty">{{ labels.noRecordsHint }}</div>
      <ul v-if="hasRecords" class="med-breakdown">
        <li v-for="b in healthScore.breakdown" :key="b.value">
          <div class="med-breakdown-head">
            <span>{{ typeLabel(b.value) }}</span>
            <strong>{{ b.count }}</strong>
          </div>
          <div class="med-breakdown-bar">
            <i :style="{ width: (b.count / maxBreakdownCount * 100) + '%', background: typeColor(b.value) }"></i>
          </div>
          <em class="med-breakdown-contri" :class="{ 'is-positive': b.contribution > 0 }">{{ b.contribution > 0 ? '+' : '' }}{{ b.contribution }}</em>
        </li>
      </ul>
    </article>

    <!-- 病历类型分布 donut -->
    <article class="med-chart-panel">
      <header>
        <div>
          <span>{{ labels.typeDist }}</span>
          <strong>{{ labels.typeDistSub }}</strong>
        </div>
      </header>
      <div v-show="typeDist.length" ref="typeRef" class="med-chart-canvas" role="img" :aria-label="labels.typeDist"></div>
      <div v-if="!typeDist.length" class="med-chart-empty">{{ labels.noTypeHint }}</div>
    </article>

    <!-- 病历时间趋势 bar -->
    <article class="med-chart-panel">
      <header>
        <div>
          <span>{{ labels.trend }}</span>
          <strong>{{ labels.trendSub }}</strong>
        </div>
      </header>
      <div v-show="hasRecords" ref="trendRef" class="med-chart-canvas" role="img" :aria-label="labels.trend"></div>
      <div v-if="!hasRecords" class="med-chart-empty">{{ labels.noTrendHint }}</div>
    </article>

    <!-- 健康分析卡 -->
    <article class="med-chart-panel med-analysis-card">
      <header>
        <div><span>{{ labels.analysis }}</span></div>
      </header>
      <ul class="med-analysis">
        <li>
          <span>{{ labels.lastRecord }}</span>
          <strong v-if="analysis.hasLast">{{ analysis.lastDays == null ? '--' : analysis.lastDays + labels.daysAgo }}</strong>
          <strong v-else>{{ labels.noRecordsHint }}</strong>
        </li>
        <li v-if="analysis.mostFrequentLabel">
          <span>{{ labels.mostFrequent }}</span>
          <strong>{{ analysis.mostFrequentLabel }}（{{ analysis.mostFrequentCount }}）</strong>
        </li>
        <li v-if="analysis.lastSymptomText">
          <span>{{ labels.lastSymptom }}</span>
          <strong>{{ analysis.lastSymptomText }}</strong>
        </li>
      </ul>
      <p class="med-advice" :data-level="healthScore.level">{{ adviceText(healthScore.level) }}</p>
    </article>

    <!-- 近期病历时间线 -->
    <article class="med-chart-panel">
      <header>
        <div><span>{{ labels.recent }}</span></div>
      </header>
      <ul class="med-timeline">
        <li v-for="r in recentFive" :key="r.id">
          <span class="med-timeline-date" :style="{ color: typeColor(r.recordType) }">{{ (r.recordDate || '').slice(5) }}</span>
          <span class="med-tl-tag" :style="tagStyle(r.recordType)">{{ typeLabel(r.recordType) }}</span>
          <span class="med-timeline-text">{{ (r.content || r.text || '').slice(0, 50) }}</span>
        </li>
        <li v-if="!recentFive.length" class="med-chart-empty med-timeline-empty">{{ labels.noRecordsHint }}</li>
      </ul>
    </article>
  </section>
</template>

<style scoped>
.med-charts {
  display: grid;
  gap: 16px;
  width: min(820px, 100%);
}

.med-chart-panel {
  min-width: 0;
  padding: 20px;
  border: 1px solid rgba(74, 144, 217, .22);
  border-radius: 24px;
  background: linear-gradient(160deg, #ffffff, #e9f2f0);
  box-shadow: 0 12px 26px rgba(20, 60, 70, .12), inset 0 1px 0 rgba(255, 255, 255, .9);
}

.med-chart-panel header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.med-chart-panel header div {
  display: grid;
  gap: 4px;
}

.med-chart-panel header span {
  color: #5a7a86;
  font-size: 14px;
  font-weight: 900;
}

.med-chart-panel header strong {
  color: #1f4a5a;
  font-size: 20px;
}

.med-chart-panel header small {
  color: #8fb0b8;
  font-size: 12px;
  font-weight: 700;
}

.med-chart-canvas,
.med-chart-empty {
  width: 100%;
  height: 235px;
}

.med-chart-empty {
  display: grid;
  place-items: center;
  color: #8fb0b8;
  font-weight: 800;
}
.med-timeline-empty { list-style: none; }

/* 健康评分卡 */
.med-score-total {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 18px;
}
.med-score-total strong {
  font-size: 56px;
  font-weight: 900;
  line-height: 1;
  color: #2f9a87;
}
.med-score-total strong[data-level="0"] { color: #2f9a87; }
.med-score-total strong[data-level="1"] { color: #b87e16; }
.med-score-total strong[data-level="2"] { color: #bf561d; }
.med-score-total strong[data-level="3"] { color: #bf3330; }
.med-score-total span {
  color: #8fb0b8;
  font-size: 18px;
  font-weight: 700;
}
.med-score-level {
  margin-left: auto;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  border: 1.5px solid currentColor;
}
.med-score-level[data-level="0"] { color: #2f9a87; background: color-mix(in srgb, #2f9a87 14%, #ffffff); }
.med-score-level[data-level="1"] { color: #b87e16; background: color-mix(in srgb, #b87e16 14%, #ffffff); }
.med-score-level[data-level="2"] { color: #bf561d; background: color-mix(in srgb, #bf561d 14%, #ffffff); }
.med-score-level[data-level="3"] { color: #bf3330; background: color-mix(in srgb, #bf3330 16%, #ffffff); }
.med-score-empty {
  padding: 18px;
  border-radius: 14px;
  background: rgba(74, 144, 217, .06);
  color: #5a7a86;
  font-weight: 700;
  text-align: center;
}

.med-breakdown {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}
.med-breakdown li {
  display: grid;
  grid-template-columns: 1fr auto 56px;
  align-items: center;
  gap: 12px;
}
.med-breakdown-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.med-breakdown-head span {
  color: #1f4a5a;
  font-size: 14px;
  font-weight: 700;
}
.med-breakdown-head strong {
  color: #1f4a5a;
  font-size: 16px;
  font-weight: 900;
}
.med-breakdown-bar {
  height: 8px;
  border-radius: 999px;
  background: rgba(74, 144, 217, .1);
  overflow: hidden;
}
.med-breakdown-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width .3s ease;
}
.med-breakdown-contri {
  text-align: right;
  color: #5a7a86;
  font-size: 13px;
  font-weight: 800;
  font-style: normal;
}
.med-breakdown-contri.is-positive { color: #2f7d5a; }

/* 健康分析卡 */
.med-analysis {
  list-style: none;
  margin: 0 0 14px;
  padding: 0;
  display: grid;
  gap: 10px;
}
.med-analysis li {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: baseline;
}
.med-analysis li span {
  color: #5a7a86;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}
.med-analysis li strong {
  color: #1f4a5a;
  font-size: 14px;
  font-weight: 700;
}
.med-advice {
  margin: 0;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(74, 144, 217, .08);
  border: 1px solid rgba(74, 144, 217, .18);
  color: #1f4a5a;
  font-size: 14px;
  line-height: 1.6;
}
.med-advice[data-level="3"] {
  background: rgba(191, 51, 48, .08);
  border-color: rgba(191, 51, 48, .3);
  color: #8a2a24;
  font-weight: 700;
}

/* 近期病历时间线 */
.med-timeline {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}
.med-timeline li {
  display: grid;
  grid-template-columns: 44px auto 1fr;
  align-items: center;
  gap: 10px;
}
.med-timeline-date {
  font-size: 13px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}
.med-tl-tag {
  padding: 2px 10px;
  border: 1px solid;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}
.med-timeline-text {
  color: #1f4a5a;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 暗色 */
:global(.night-theme) .med-chart-panel {
  background: linear-gradient(160deg, #1c313a, #142228);
  border-color: rgba(127, 208, 200, .3);
  box-shadow: 0 12px 26px rgba(0, 0, 0, .4), inset 0 1px 0 rgba(255, 255, 255, .06);
}
:global(.night-theme) .med-chart-panel header span { color: #8fb0b8; }
:global(.night-theme) .med-chart-panel header strong { color: #d4e6ec; }
:global(.night-theme) .med-chart-panel header small { color: #6f8a93; }
:global(.night-theme) .med-chart-empty { color: #6f8a93; }
:global(.night-theme) .med-score-total span { color: #6f8a93; }
:global(.night-theme) .med-score-level[data-level="0"] { background: color-mix(in srgb, #2f9a87 26%, transparent); }
:global(.night-theme) .med-score-level[data-level="1"] { background: color-mix(in srgb, #b87e16 26%, transparent); }
:global(.night-theme) .med-score-level[data-level="2"] { background: color-mix(in srgb, #bf561d 26%, transparent); }
:global(.night-theme) .med-score-level[data-level="3"] { background: color-mix(in srgb, #bf3330 28%, transparent); }
:global(.night-theme) .med-score-empty { background: rgba(127, 208, 200, .08); color: #8fb0b8; }
:global(.night-theme) .med-breakdown-head span,
:global(.night-theme) .med-breakdown-head strong { color: #d4e6ec; }
:global(.night-theme) .med-breakdown-bar { background: rgba(127, 208, 200, .12); }
:global(.night-theme) .med-breakdown-contri { color: #8fb0b8; }
:global(.night-theme) .med-analysis li span { color: #8fb0b8; }
:global(.night-theme) .med-analysis li strong { color: #e4f0f4; }
:global(.night-theme) .med-advice {
  background: rgba(74, 144, 217, .14);
  border-color: rgba(127, 208, 200, .2);
  color: #d4e6ec;
}
:global(.night-theme) .med-advice[data-level="3"] {
  background: rgba(191, 51, 48, .18);
  border-color: rgba(191, 51, 48, .4);
  color: #f0a8a4;
}
:global(.night-theme) .med-timeline-date { color: #9fd6c8; }
:global(.night-theme) .med-timeline-text { color: #c4d6dc; }

@media (max-width: 620px) {
  .med-chart-panel { padding: 16px; }
  .med-chart-canvas, .med-chart-empty { height: 210px; }
  .med-score-total strong { font-size: 46px; }
  .med-timeline li { grid-template-columns: 40px auto 1fr; }
}

@media (prefers-reduced-motion: reduce) {
  .med-breakdown-bar i { transition: none; }
}
</style>
