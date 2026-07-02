<template>
  <section class="mock-view history-records-view">
    <div class="mock-toolbar">
      <input v-model="keyword" class="mock-input wide" type="search" placeholder="搜索时间、设备编号、位置" />
      <button class="mock-button" type="button" @click="resetFilters">清空</button>
    </div>

    <div class="mock-filter-row">
      <label v-for="group in filterGroups" :key="group.key" class="mock-filter">
        <span>{{ group.label }}</span>
        <select v-model="filters[group.key]">
          <option v-for="option in group.options" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>
    </div>

    <div class="mock-table-list history-list">
      <article v-for="(record, index) in filteredRecords" :key="record.id" class="history-row" :class="getHistoryRowClass(record, index)">
        <span>{{ record.time }}</span>
        <span>{{ record.deviceId }}</span>
        <span>{{ record.location }}</span>
        <span>{{ record.smoke }} ppm</span>
        <span>{{ record.risk }}</span>
        <span>{{ record.alarm }}</span>
        <span>{{ record.status }}</span>
        <button class="mock-link-button" type="button">查看</button>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";

const keyword = ref("");
const defaultFilters = { time: "全部", risk: "全部", status: "全部", smoke: "全部" };
const filters = reactive({ ...defaultFilters });
const filterGroups = [
  { key: "time", label: "时间范围", options: ["全部", "今天", "近 7 天", "近 30 天"] },
  { key: "risk", label: "风险等级", options: ["全部", "正常", "低风险", "中风险", "高风险"] },
  { key: "status", label: "处理状态", options: ["全部", "未处理", "处理中", "已处理"] },
  { key: "smoke", label: "浓度范围", options: ["全部", "0-100", "100-200", "200-400", "400以上"] }
];

const records = [
  { id: 1, time: "2026-07-02 09:30", deviceId: "SMK-023", location: "三号楼 2F", smoke: 86, risk: "正常", alarm: "安全", status: "未处理" },
  { id: 2, time: "2026-07-02 09:15", deviceId: "SMK-011", location: "实验楼 1F", smoke: 156, risk: "低风险", alarm: "关注", status: "处理中" },
  { id: 3, time: "2026-07-02 08:42", deviceId: "SMK-030", location: "地下车库", smoke: 365, risk: "中风险", alarm: "预警", status: "已处理" },
  { id: 4, time: "2026-07-01 22:08", deviceId: "SMK-017", location: "食堂后厨", smoke: 421, risk: "高风险", alarm: "告警", status: "已处理" },
  { id: 5, time: "2026-07-01 19:33", deviceId: "SMK-008", location: "库房 A 区", smoke: 71, risk: "正常", alarm: "安全", status: "未处理" },
  { id: 6, time: "2026-07-01 15:20", deviceId: "SMK-019", location: "办公楼 3F", smoke: 184, risk: "低风险", alarm: "关注", status: "处理中" }
];

const latestRecordTime = computed(() => getLatestDate(records.map((record) => record.time)));
const filteredRecords = computed(() => {
  const query = keyword.value.trim();
  return records.filter((record) => {
    const text = `${record.time}${record.deviceId}${record.location}${record.risk}${record.status}${record.alarm}`;
    const matchKeyword = !query || text.includes(query);
    const matchTime = filters.time === "全部" || matchTimeRange(record.time, filters.time, latestRecordTime.value);
    const matchRisk = filters.risk === "全部" || record.risk === filters.risk;
    const matchStatus = filters.status === "全部" || record.status === filters.status;
    const matchSmoke = filters.smoke === "全部" || matchSmokeRange(record.smoke, filters.smoke);
    return matchKeyword && matchTime && matchRisk && matchStatus && matchSmoke;
  });
});


function getHistoryRowClass(record, index) {
  return {
    "highlight-row": record.deviceId === "SMK-030",
    "fade-level-1": index === 3,
    "fade-level-2": index === 4,
    "fade-level-3": index >= 5
  };
}
function resetFilters() {
  keyword.value = "";
  Object.assign(filters, defaultFilters);
}

function matchSmokeRange(value, range) {
  if (range === "0-100") return value >= 0 && value <= 100;
  if (range === "100-200") return value > 100 && value <= 200;
  if (range === "200-400") return value > 200 && value <= 400;
  if (range === "400以上") return value > 400;
  return true;
}

function matchTimeRange(timeText, range, baseDate) {
  const date = parseDate(timeText);
  if (!date || !baseDate) return true;
  if (range === "今天") return toDateKey(date) === toDateKey(baseDate);
  if (range === "近 7 天") return withinDays(date, baseDate, 7);
  if (range === "近 30 天") return withinDays(date, baseDate, 30);
  return true;
}

function withinDays(date, baseDate, days) {
  const diff = baseDate.getTime() - date.getTime();
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
}

function getLatestDate(values) {
  const dates = values.map(parseDate).filter(Boolean);
  if (!dates.length) return null;
  return new Date(Math.max(...dates.map((date) => date.getTime())));
}

function parseDate(value) {
  const date = new Date(value.replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function toDateKey(date) {
  return date.toISOString().slice(0, 10);
}
</script>