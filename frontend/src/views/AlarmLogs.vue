<template>
  <section class="mock-view alarm-logs-view">
    <div class="mock-toolbar">
      <input v-model="keyword" class="mock-input wide" type="search" placeholder="搜索告警时间、设备编号、类型" />
      <button class="mock-button" type="button" @click="resetFilters">重置搜索</button>
    </div>

    <div class="mock-filter-row">
      <label v-for="group in filterGroups" :key="group.key" class="mock-filter">
        <span>{{ group.label }}</span>
        <select v-model="filters[group.key]">
          <option v-for="option in group.options" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>
    </div>

    <div class="mock-list alarm-log-list">
      <article v-for="log in filteredLogs.slice(0, 5)" :key="log.id" class="mock-list-row">
        <span>{{ log.time }}</span>
        <span>{{ log.deviceId }}</span>
        <span>{{ log.type }}</span>
        <span>{{ log.level }}</span>
        <span>{{ log.status }}</span>
        <button class="mock-link-button" type="button" @click="selectedLog = log">查看详情</button>
      </article>
    </div>

    <div v-if="selectedLog" class="mock-modal-mask" @click.self="selectedLog = null">
      <div class="mock-modal">
        <h3>告警详情</h3>
        <p>浓度峰值：{{ selectedLog.peak }} ppm</p>
        <p>触发设备：{{ selectedLog.deviceName }}</p>
        <p>处理人：{{ selectedLog.handler }}</p>
        <p>处理备注：{{ selectedLog.remark }}</p>
        <button class="mock-button" type="button" @click="selectedLog = null">关闭</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";

const keyword = ref("");
const selectedLog = ref(null);
const defaultFilters = { type: "全部", level: "全部", status: "全部", time: "全部" };
const filters = reactive({ ...defaultFilters });

const filterGroups = [
  { key: "type", label: "告警类型", options: ["全部", "烟雾超限", "设备离线", "联动失败", "手动控制"] },
  { key: "level", label: "告警等级", options: ["全部", "低", "中", "高"] },
  { key: "status", label: "处理状态", options: ["全部", "未处理", "处理中", "已处理"] },
  { key: "time", label: "时间范围", options: ["全部", "近一小时", "近六小时", "近一天", "近七天"] }
];

const logs = [
  { id: 1, time: "2026-07-02 09:36", deviceId: "SMK-023", deviceName: "三号楼烟感", type: "烟雾超限", level: "高", status: "未处理", peak: 462, handler: "待分配", remark: "高风险自动联动" },
  { id: 2, time: "2026-07-02 08:58", deviceId: "SMK-011", deviceName: "实验室烟感", type: "联动失败", level: "中", status: "处理中", peak: 231, handler: "王工", remark: "报警灯响应延迟" },
  { id: 3, time: "2026-07-02 08:12", deviceId: "SMK-008", deviceName: "库房烟感", type: "设备离线", level: "中", status: "未处理", peak: 0, handler: "待分配", remark: "心跳超时" },
  { id: 4, time: "2026-07-01 21:20", deviceId: "SMK-017", deviceName: "食堂烟感", type: "手动控制", level: "低", status: "已处理", peak: 118, handler: "李工", remark: "人工测试复位" },
  { id: 5, time: "2026-07-01 18:47", deviceId: "SMK-030", deviceName: "地下车库烟感", type: "烟雾超限", level: "高", status: "已处理", peak: 418, handler: "赵工", remark: "已通风排查" }
];

const latestLogTime = computed(() => getLatestDate(logs.map((log) => log.time)));
const filteredLogs = computed(() => {
  const query = keyword.value.trim();
  return logs.filter((log) => {
    const text = `${log.time}${log.deviceId}${log.deviceName}${log.type}${log.status}${log.level}`;
    const matchKeyword = !query || text.includes(query);
    const matchType = filters.type === "全部" || log.type === filters.type;
    const matchLevel = filters.level === "全部" || log.level === filters.level;
    const matchStatus = filters.status === "全部" || log.status === filters.status;
    const matchTime = filters.time === "全部" || matchRecentTime(log.time, filters.time, latestLogTime.value);
    return matchKeyword && matchType && matchLevel && matchStatus && matchTime;
  });
});

function resetFilters() {
  keyword.value = "";
  Object.assign(filters, defaultFilters);
  selectedLog.value = null;
}

function matchRecentTime(timeText, range, baseDate) {
  const date = parseDate(timeText);
  if (!date || !baseDate) return true;
  const hours = {
    近一小时: 1,
    近六小时: 6,
    近一天: 24,
    近七天: 24 * 7
  }[range];
  if (!hours) return true;
  const diff = baseDate.getTime() - date.getTime();
  return diff >= 0 && diff <= hours * 60 * 60 * 1000;
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
</script>