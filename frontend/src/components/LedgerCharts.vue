<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  records: {
    type: Array,
    default: () => [],
  },
  dark: {
    type: Boolean,
    default: false,
  },
  copy: {
    type: Object,
    default: () => ({}),
  },
  locale: {
    type: String,
    default: 'zh-CN',
  },
  categories: {
    type: Object,
    default: () => ({}),
  },
})

const DEFAULT_COPY = {
  expense: '支出', categoryExpense: '分类支出', aria: '支出统计', trend: '支出趋势',
  last6Months: '最近 6 个月', currencyUnit: '单位：元', trendAria: '最近六个月支出柱状图',
  trendEmpty: '记录支出后显示趋势', categoryShare: '分类占比', destination: '支出去向',
  categoryCount: '共 5 类', categoryAria: '分类支出环形图', categoryEmpty: '暂无分类统计',
}
const copyText = computed(() => ({ ...DEFAULT_COPY, ...props.copy }))

const trendRef = ref(null)
const categoryRef = ref(null)
let trendChart = null
let categoryChart = null
let resizeObserver = null

const CATEGORY_COLORS = {
  食物: '#d68c3d',
  医疗: '#d66767',
  清洁: '#4c9a91',
  玩具: '#6f8fca',
  其他: '#9a7ab5',
}

function normalizeCategory(category) {
  const value = String(category || '').trim()
  if (Object.hasOwn(CATEGORY_COLORS, value)) return value
  if (/主粮|零食|饲料|食物|食品/.test(value)) return '食物'
  if (/医疗|体检|药|就诊/.test(value)) return '医疗'
  if (/清洁|卫生|消毒|用品/.test(value)) return '清洁'
  if (/玩具|娱乐/.test(value)) return '玩具'
  return '其他'
}

function recentMonths(count = 6) {
  const now = new Date()
  return Array.from({ length: count }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (count - 1 - index), 1)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    return { key, label: new Intl.DateTimeFormat(props.locale, { month: 'short' }).format(date) }
  })
}

const monthlyData = computed(() => {
  const months = recentMonths()
  const totals = Object.fromEntries(months.map((item) => [item.key, 0]))
  props.records.forEach((record) => {
    const month = String(record.time || record.expenseDate || '').slice(0, 7)
    if (Object.hasOwn(totals, month)) totals[month] += Number(record.amount || 0)
  })
  return months.map((item) => ({ ...item, value: Number(totals[item.key].toFixed(2)) }))
})

const categoryData = computed(() => {
  const totals = Object.fromEntries(Object.keys(CATEGORY_COLORS).map((category) => [category, 0]))
  props.records.forEach((record) => {
    totals[normalizeCategory(record.tag || record.category)] += Number(record.amount || 0)
  })
  return Object.entries(totals)
    .map(([name, value]) => ({ name: props.categories[name] || name, value: Number(value.toFixed(2)) }))
    .filter((item) => item.value > 0)
})

const hasRecords = computed(() => props.records.some((record) => Number(record.amount || 0) > 0))

function chartTextColor() {
  return props.dark ? '#e8e1f3' : '#5f5147'
}

function mutedTextColor() {
  return props.dark ? '#aeb8ca' : '#8b7b70'
}

function renderTrendChart() {
  if (!trendRef.value) return
  trendChart ||= echarts.init(trendRef.value)
  const values = monthlyData.value.map((item) => item.value)
  trendChart.setOption({
    animationDuration: 500,
    grid: { left: 18, right: 18, top: 26, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      formatter: (items) => `${items[0].axisValue}<br/>${copyText.value.expense}：¥${Number(items[0].value).toFixed(2)}`,
    },
    xAxis: {
      type: 'category',
      data: monthlyData.value.map((item) => item.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: props.dark ? '#42506a' : '#cfdae1' } },
      axisLabel: { color: mutedTextColor(), fontWeight: 700 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: mutedTextColor(), formatter: (value) => `¥${value}` },
      splitLine: { lineStyle: { color: props.dark ? 'rgba(174,184,202,.13)' : 'rgba(84,139,177,.12)' } },
    },
    series: [{
      name: copyText.value.expense,
      type: 'bar',
      data: values,
      barMaxWidth: 34,
      itemStyle: {
        color: '#d68c3d',
        borderRadius: [7, 7, 2, 2],
      },
      emphasis: { itemStyle: { color: '#bf742d' } },
    }],
  }, true)
}

function renderCategoryChart() {
  if (!categoryRef.value) return
  categoryChart ||= echarts.init(categoryRef.value)
  categoryChart.setOption({
    animationDuration: 500,
    color: Object.values(CATEGORY_COLORS),
    tooltip: {
      trigger: 'item',
      formatter: ({ name, value, percent }) => `${name}<br/>¥${Number(value).toFixed(2)} · ${percent}%`,
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 6,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: chartTextColor(), fontWeight: 700 },
    },
    series: [{
      name: copyText.value.categoryExpense,
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderColor: props.dark ? '#10243f' : '#f7fbfe',
        borderWidth: 3,
        borderRadius: 5,
      },
      label: { show: false },
      emphasis: {
        scaleSize: 7,
        label: { show: true, color: chartTextColor(), fontWeight: 900, formatter: '{b}\n{d}%' },
      },
      data: categoryData.value,
    }],
  }, true)
}

async function renderCharts() {
  await nextTick()
  renderTrendChart()
  renderCategoryChart()
}

onMounted(() => {
  renderCharts()
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize()
    categoryChart?.resize()
  })
  if (trendRef.value) resizeObserver.observe(trendRef.value)
  if (categoryRef.value) resizeObserver.observe(categoryRef.value)
})

watch(() => [props.records, props.dark, props.copy, props.locale, props.categories], renderCharts, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  trendChart?.dispose()
  categoryChart?.dispose()
})
</script>

<template>
  <section class="ledger-chart-section" :aria-label="copyText.aria">
    <article class="ledger-chart-panel">
      <header>
        <div>
          <span>{{ copyText.trend }}</span>
          <strong>{{ copyText.last6Months }}</strong>
        </div>
        <small>{{ copyText.currencyUnit }}</small>
      </header>
      <div v-show="hasRecords" ref="trendRef" class="ledger-chart-canvas" role="img" :aria-label="copyText.trendAria"></div>
      <div v-if="!hasRecords" class="ledger-chart-empty">{{ copyText.trendEmpty }}</div>
    </article>

    <article class="ledger-chart-panel">
      <header>
        <div>
          <span>{{ copyText.categoryShare }}</span>
          <strong>{{ copyText.destination }}</strong>
        </div>
        <small>{{ copyText.categoryCount }}</small>
      </header>
      <div v-show="hasRecords" ref="categoryRef" class="ledger-chart-canvas" role="img" :aria-label="copyText.categoryAria"></div>
      <div v-if="!hasRecords" class="ledger-chart-empty">{{ copyText.categoryEmpty }}</div>
    </article>
  </section>
</template>

<style scoped>
.ledger-chart-section {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, .75fr);
  gap: 16px;
}

.ledger-chart-panel {
  min-width: 0;
  min-height: 310px;
  padding: 20px;
  border: 1px solid rgba(84, 139, 177, .16);
  border-radius: 24px;
  background:
    radial-gradient(circle at 88% 12%, rgba(255, 255, 255, .8), transparent 34%),
    linear-gradient(145deg, #f7fbfe, #eaf4fa);
  box-shadow: 0 10px 24px rgba(63, 102, 128, .08);
}

.ledger-chart-panel header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.ledger-chart-panel header div {
  display: grid;
  gap: 4px;
}

.ledger-chart-panel header span {
  color: #4b718b;
  font-size: 14px;
  font-weight: 900;
}

.ledger-chart-panel header strong {
  color: #2d3f4b;
  font-size: 20px;
}

.ledger-chart-panel header small {
  color: #8b7b70;
  font-size: 12px;
  font-weight: 700;
}

.ledger-chart-canvas,
.ledger-chart-empty {
  width: 100%;
  height: 235px;
}

.ledger-chart-empty {
  display: grid;
  place-items: center;
  color: #8b7b70;
  font-weight: 800;
}

:global(.night-theme) .ledger-chart-panel {
  border-color: rgba(189, 174, 255, .2);
  background: #10243f;
  box-shadow: none;
}

:global(.night-theme) .ledger-chart-panel header span,
:global(.night-theme) .ledger-chart-panel header strong,
:global(.night-theme) .ledger-chart-panel header small,
:global(.night-theme) .ledger-chart-empty {
  color: #e8e1f3;
}

@media (max-width: 900px) {
  .ledger-chart-section {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 620px) {
  .ledger-chart-panel {
    min-height: 280px;
    padding: 16px;
  }

  .ledger-chart-canvas,
  .ledger-chart-empty {
    height: 210px;
  }
}
</style>
