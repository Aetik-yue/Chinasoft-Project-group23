<template>
  <div class="app-shell">
    <aside class="sidebar glass-panel">
      <div class="brand">
        <div class="brand-mark">烟</div>
        <div>
          <strong>智慧烟感预警系统</strong>
          <span>Smart Smoke Detection System</span>
        </div>
      </div>

      <nav class="menu">
        <button
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item"
          :class="{ active: activeMenu === item.key }"
          type="button"
          @click="setActiveView(item)"
        >
          <span>{{ item.icon }}</span> {{ item.label }}
        </button>
      </nav>

      <div class="side-status">
        <div><span class="dot"></span> 设备在线</div>
        <strong>{{ deviceOnlineCount }}/{{ deviceTotalCount }}</strong>
        <small id="sideSystemStatus">{{ systemStatus }}</small>
      </div>
    </aside>

    <main class="main-content">
      <header class="topbar glass-panel">
        <div class="page-title">
          <button class="icon-button" type="button" aria-label="菜单">☰</button>
          <div>
            <h1>智慧烟感预警系统</h1>
            <span>{{ activePageTitle }}</span>
          </div>
        </div>

        <div class="top-actions">
          <label class="theme-switcher">
            <span>切换风险状态</span>
            <select id="riskPreset" :value="themeType" @change="applyRiskPreset($event.target.value)">
              <option value="safe">正常 / 安全</option>
              <option value="low">低风险</option>
              <option value="medium">中风险</option>
              <option value="high">高风险 / 告警中</option>
            </select>
          </label>
          <span class="online-pill" id="onlineStatus">{{ onlineStatus }}</span>
          <time id="currentTime">{{ currentTime }}</time>
          <button class="admin-entry" type="button">管理员 ▾</button>
        </div>
      </header>

      <DeviceDisconnected
        v-if="shouldShowDisconnected"
        :message="connectionMessage"
        @reconnect="handleReconnectDevice"
      />

      <template v-else-if="activeView === 'dashboard'">
      <section class="stat-grid">
        <article class="stat-card glass-panel primary-card">
          <div class="card-head"><span>烟</span><b>当前烟雾浓度</b></div>
          <div class="big-number"><span id="currentSmoke">{{ smokeValue }}</span><em>ppm</em></div>
          <p>设备编号：<strong id="deviceId">{{ deviceId }}</strong></p>
          <p>更新时间：<strong id="updateTime">{{ updateTime }}</strong></p>
        </article>

        <article class="stat-card glass-panel">
          <div class="card-head"><span>险</span><b>风险等级</b></div>
          <div class="risk-text" id="riskLevel">{{ riskLevel }}</div>
          <p id="riskDescription">{{ riskDescription }}</p>
        </article>

        <article class="stat-card glass-panel">
          <div class="card-head"><span>警</span><b>报警状态</b></div>
          <div class="alarm-text" id="alarmStatus">{{ alarmStatus }}</div>
          <p id="alarmDescription">{{ alarmDescription }}</p>
        </article>

        <article class="stat-card glass-panel">
          <div class="card-head"><span>报</span><b>今日告警</b></div>
          <div class="big-number"><span id="todayAlarmCount">{{ todayAlarmCount }}</span><em>次</em></div>
          <p>已处理：<strong id="handledCount">{{ handledAlarmCount }}</strong> 次</p>
          <p>较昨日：<strong id="dayChange">{{ yesterdayDiff }}</strong></p>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="chart-card glass-panel">
          <div class="section-title">
            <h2>烟雾浓度历史趋势</h2>
            <div class="legend-note">默认展示最近 24 小时</div>
          </div>
          <div id="smokeChart" ref="smokeChartRef" class="chart"></div>
          <div class="range-tabs" id="rangeTabs">
            <button
              v-for="range in timeRanges"
              :key="range.value"
              type="button"
              :data-range="range.value"
              :class="{ active: selectedTimeRange === range.value }"
              @click="changeTimeRange(range.value)"
            >
              {{ range.label }}
            </button>
          </div>
        </article>

        <article class="control-card glass-panel">
          <div class="section-title">
            <h2>设备控制</h2>
          </div>

          <div class="device-list">
            <button
              class="device-toggle"
              :class="{ on: buzzerOn }"
              type="button"
              data-device="buzzer"
              @click="handleDeviceControl('buzzer')"
            >
              <span class="device-icon">声</span>
              <b>蜂鸣器</b>
              <em>{{ buzzerOn ? "开" : "关" }}</em>
            </button>
            <button
              class="device-toggle"
              :class="{ on: lightOn }"
              type="button"
              data-device="light"
              @click="handleDeviceControl('light')"
            >
              <span class="device-icon">灯</span>
              <b>报警灯</b>
              <em>{{ lightOn ? "开" : "关" }}</em>
            </button>
          </div>

          <div class="demo-actions">
            <button class="action danger" id="raiseSmokeBtn" type="button" @click="raiseSmoke">
              测试传感器开
            </button>
            <button class="action restore" id="restoreBtn" type="button" @click="restoreEnvironment">
              测试传感器关
            </button>
          </div>
        </article>
      </section>

      <section class="table-card glass-panel">
        <div class="section-title">
          <h2>历史报警记录</h2>
          <span>最近 5 条</span>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>报警时间</th>
                <th>设备编号</th>
                <th>报警类型</th>
                <th>烟雾浓度</th>
                <th>风险等级</th>
                <th>处理状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody id="alarmTableBody">
              <tr v-for="record in alarmLogs.slice(0, 5)" :key="`${record.alarmTime}-${record.smokeValue}`">
                <td>{{ record.alarmTime }}</td>
                <td>{{ record.deviceId }}</td>
                <td>{{ record.alarmType }}</td>
                <td>{{ record.smokeValue }} ppm</td>
                <td><span class="tag" :class="getRiskClass(record.riskLevel)">{{ record.riskLevel }}</span></td>
                <td><span class="status-tag">{{ record.status }}</span></td>
                <td><button class="detail-btn" type="button">查看详情</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      </template>

      <component v-else :is="currentView" />
    </main>
  </div>
</template>

<script setup>
import * as echarts from "echarts/dist/echarts.esm.min.js";
import DeviceDisconnected from "./components/DeviceDisconnected.vue";
import AlarmLogs from "./views/AlarmLogs.vue";
import DeviceManagement from "./views/DeviceManagement.vue";
import HistoryRecords from "./views/HistoryRecords.vue";
import LinkageControl from "./views/LinkageControl.vue";
import SystemSettings from "./views/SystemSettings.vue";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  DEVICE_ID,
  RISK_THRESHOLDS,
  formatClock,
  formatDateTime,
  getRiskKey,
  nextSmokeValue,
  presetSmokeValue,
  riskCopy
} from "./mockData.js";
import {
  connectDevice,
  controlDevice,
  getAlarmLogs,
  getDashboardCurrent,
  getDeviceConnectionStatus,
  getRuntimeLinkSnapshot,
  getSmokeHistory,
  getSmokeLatest,
  subscribeRuntimeLinkEvents
} from "./api/dashboard.js";

const smokeValue = ref(86);
const todayAlarmCount = ref(2);
const handledAlarmCount = ref(2);
const yesterdayDiff = ref("+1");
const middleRiskThreshold = ref(RISK_THRESHOLDS.lowMax);
const highRiskThreshold = ref(RISK_THRESHOLDS.mediumMax);
const deviceOnlineCount = ref(12);
const deviceTotalCount = ref(12);
const chartData = ref([]);
const alarmLogs = ref([]);
const deviceId = ref(DEVICE_ID);
const updateTime = ref(formatClock());
const currentTime = ref(formatDateTime());
const riskLevel = ref("正常");
const alarmStatus = ref("安全");
const themeType = ref("safe");
const systemStatus = ref("系统状态：运行正常");
const selectedTimeRange = ref("24h");
const buzzerOn = ref(false);
const lightOn = ref(false);
const fanOn = ref(false);
const isDeviceConnected = ref(true);
const deviceStatus = ref({
  connected: true,
  status: "connected",
  linkState: "online",
  displayMode: "dashboard"
});
const linkState = ref("online");
const displayMode = ref("dashboard");
const connectionMessage = ref("");
const activeMenu = ref("dashboard");
const activeView = ref("dashboard");

const mode = ref("safe");
const smokeChartRef = ref(null);
let smokeChart;
let clockTimer;
let smokeTimer;
let linkPollTimer;
let linkEventsCleanup;


const menuItems = [
  { key: "dashboard", view: "dashboard", icon: "⌂", label: "首页总览" },
  { key: "history", view: "history", icon: "□", label: "历史记录" },
  { key: "alarms", view: "alarms", icon: "◇", label: "告警日志" },
  { key: "devices", view: "devices", icon: "▣", label: "设备管理" },
  { key: "linkage", view: "linkage", icon: "↔", label: "联动控制" },
  { key: "settings", view: "settings", icon: "⚙", label: "系统设置" }
];

const viewComponents = {
  alarms: AlarmLogs,
  history: HistoryRecords,
  linkage: LinkageControl,
  devices: DeviceManagement,
  settings: SystemSettings
};
const timeRanges = [
  { value: "realtime", label: "实时" },
  { value: "6h", label: "6小时" },
  { value: "12h", label: "12小时" },
  { value: "24h", label: "24小时" },
  { value: "7d", label: "7天" }
];

const currentRiskCopy = computed(() => riskCopy[themeType.value] || riskCopy.safe);
const riskDescription = computed(() => currentRiskCopy.value.description);
const alarmDescription = computed(() => currentRiskCopy.value.alarmDescription);
const onlineStatus = computed(() => (themeType.value === "high" ? "告警联动中" : "系统运行正常"));
const shouldShowDisconnected = computed(() => isDisconnectedStatus(deviceStatus.value));
const currentView = computed(() => viewComponents[activeView.value] || AlarmLogs);
const activePageTitle = computed(() => menuItems.find((item) => item.key === activeMenu.value)?.label || "首页总览");

onMounted(async () => {
  await refreshDeviceConnectionStatus();
  setupLinkEvents();
  linkPollTimer = window.setInterval(refreshDeviceConnectionStatus, 3000);

  if (!shouldShowDisconnected.value) {
    await ensureDashboardReady();
  }

  updateClock();
  clockTimer = window.setInterval(updateClock, 1000);
  smokeTimer = window.setInterval(refreshSmokeValue, 3000);
  window.addEventListener("resize", resizeChart);
});

onBeforeUnmount(() => {
  window.clearInterval(clockTimer);
  window.clearInterval(smokeTimer);
  window.clearInterval(linkPollTimer);
  window.removeEventListener("resize", resizeChart);
  if (linkEventsCleanup) linkEventsCleanup();
  if (smokeChart) smokeChart.dispose();
});

watch(smokeValue, () => {
  updateRiskState();
  updateTime.value = formatClock();
});

watch(themeType, (value) => {
  document.body.className = `${value}-theme`;
});

watch(chartData, () => renderChart(), { deep: true });

watch(shouldShowDisconnected, async (disconnected) => {
  if (disconnected) return;
  await ensureDashboardReady();
});


const disconnectEventTypes = ["link_lost", "heartbeat_timeout", "link_fault"];

function isDisconnectedStatus(status = {}) {
  return (
    status.connected === false ||
    status.status === "disconnected" ||
    status.status === "error" ||
    status.linkState === "offline" ||
    status.linkState === "fault" ||
    status.displayMode === "unconnected_page"
  );
}

function applyDeviceConnectionStatus(status = {}) {
  const nextStatus = {
    ...deviceStatus.value,
    ...status
  };

  if (disconnectEventTypes.includes(nextStatus.eventType)) {
    nextStatus.connected = false;
    nextStatus.status = nextStatus.status || "disconnected";
    nextStatus.linkState = nextStatus.linkState || "offline";
    nextStatus.displayMode = "unconnected_page";
  }

  deviceStatus.value = nextStatus;
  isDeviceConnected.value = !isDisconnectedStatus(nextStatus);
  linkState.value = nextStatus.linkState || "online";
  displayMode.value = nextStatus.displayMode || "dashboard";
  connectionMessage.value = isDeviceConnected.value
    ? ""
    : nextStatus.message || "请检查设备连接状态。";
}

async function refreshDeviceConnectionStatus() {
  const status = await getDeviceConnectionStatus();
  applyDeviceConnectionStatus(status);

  if (isDisconnectedStatus(status)) return;
  if (status.displayMode === "dashboard" && status.connected !== false) return;

  const snapshot = await getRuntimeLinkSnapshot();
  applyDeviceConnectionStatus(snapshot);
}

function setupLinkEvents() {
  linkEventsCleanup = subscribeRuntimeLinkEvents((status) => {
    applyDeviceConnectionStatus(status);
  });
}

async function handleReconnectDevice() {
  const status = await connectDevice();
  applyDeviceConnectionStatus(status);
  await refreshDeviceConnectionStatus();
}

async function ensureDashboardReady() {
  await loadMockData();
  await nextTick();
  if (!smokeChart && smokeChartRef.value) initChart();
  renderChart();
  resizeChart();
}

async function setActiveView(item) {
  activeMenu.value = item.key;
  activeView.value = item.view;
  if (item.view === "dashboard" && !shouldShowDisconnected.value) {
    await nextTick();
    await ensureDashboardReady();
  }
}
async function loadMockData() {
  const current = await getDashboardCurrent();
  smokeValue.value = current.smokeValue;
  deviceId.value = current.deviceId;
  updateTime.value = formatClock(new Date(current.updateTime));
  deviceOnlineCount.value = current.deviceOnlineCount || 12;
  deviceTotalCount.value = current.deviceTotalCount || 12;
  todayAlarmCount.value = current.todayAlarmCount;
  handledAlarmCount.value = current.handledAlarmCount;
  yesterdayDiff.value = current.yesterdayDiff;
  buzzerOn.value = current.buzzerOn;
  lightOn.value = current.lightOn;
  fanOn.value = current.fanOn;
  chartData.value = await getSmokeHistory(selectedTimeRange.value);
  alarmLogs.value = await getAlarmLogs(5);
  updateRiskState(current);
}

function updateRiskState(current = {}) {
  const riskKey = current.themeType || getRiskKey(smokeValue.value);
  const copy = riskCopy[riskKey] || riskCopy.safe;

  themeType.value = riskKey;
  riskLevel.value = current.riskLevel || copy.level;
  alarmStatus.value = current.alarmStatus || copy.alarm;
  systemStatus.value = current.systemStatus || (riskKey === "high" ? "系统状态：告警中" : "系统状态：运行正常");
}

function updateClock() {
  currentTime.value = formatDateTime();
}

function applyRiskPreset(riskKey) {
  mode.value = riskKey === "high" ? "alarm" : riskKey;
  smokeValue.value = presetSmokeValue(riskKey);

  if (riskKey === "high") {
    buzzerOn.value = true;
    lightOn.value = true;
    fanOn.value = true;
    addAlarmRecord("设备联动告警");
  }

  syncLatestTrendPoint();
}

async function refreshSmokeValue() {
  if (shouldShowDisconnected.value) return;
  // 优先调真实后端 /api/smoke/latest，失败时降级本地随机波动
  let latest = null;
  try {
    latest = await getSmokeLatest();
  } catch (error) {
    console.warn("[refreshSmokeValue] 后端不可用，降级本地随机", error.message);
  }

  if (latest) {
    smokeValue.value = latest.smokeValue;
    if (latest.deviceId) deviceId.value = latest.deviceId;
    if (latest.updatedAt) updateTime.value = formatClock(new Date(latest.updatedAt));
    updateRiskState({
      themeType: latest.themeType,
      riskLevel: latest.riskLevel,
      alarmStatus: latest.alarmStatus
    });
    appendTrendPoint();
    return;
  }

  smokeValue.value = nextSmokeValue(mode.value, smokeValue.value);

  const riskKey = getRiskKey(smokeValue.value);
  if (riskKey === "medium" || riskKey === "high") {
    const last = alarmLogs.value[0];
    const lastMinute = last ? last.alarmTime.slice(0, 16) : "";
    const currentMinute = formatDateTime().slice(0, 16);
    if (lastMinute !== currentMinute) addAlarmRecord("烟雾浓度超标");
  }

  appendTrendPoint();
  renderTodayStats();
}

function raiseSmoke() {
  smokeValue.value = 452 + Math.round(Math.random() * 22);
  mode.value = "alarm";
  buzzerOn.value = true;
  lightOn.value = true;
  fanOn.value = true;
  addAlarmRecord("设备联动告警");
  syncLatestTrendPoint();
  renderTodayStats();
}

function restoreEnvironment() {
  smokeValue.value = 60 + Math.round(Math.random() * 16);
  mode.value = "safe";
  buzzerOn.value = false;
  lightOn.value = false;
  fanOn.value = false;
  syncLatestTrendPoint();
  renderTodayStats();
}

async function changeTimeRange(range) {
  selectedTimeRange.value = range;
  chartData.value = await getSmokeHistory(range);
  syncLatestTrendPoint();
}

async function handleDeviceControl(target) {
  const nextStatus = !getDeviceStatus(target);
  setDeviceStatus(target, nextStatus);

  const response = await controlDevice({
    deviceId: deviceId.value,
    target,
    action: nextStatus ? "on" : "off"
  });

  // Use the backend returned device status after the real API is connected.
  if (response.success && (response.status === "on" || response.status === "off")) {
    setDeviceStatus(target, response.status === "on");
  }
}

function getDeviceStatus(target) {
  if (target === "buzzer") return buzzerOn.value;
  if (target === "light") return lightOn.value;
  if (target === "fan") return fanOn.value;
  return false;
}

function setDeviceStatus(target, status) {
  if (target === "buzzer") buzzerOn.value = status;
  if (target === "light") lightOn.value = status;
  if (target === "fan") fanOn.value = status;
}

function addAlarmRecord(alarmType) {
  const riskKey = getRiskKey(smokeValue.value);
  const copy = riskCopy[riskKey];

  alarmLogs.value.unshift({
    alarmTime: formatDateTime(),
    deviceId: deviceId.value,
    alarmType,
    smokeValue: smokeValue.value,
    riskLevel: copy.level,
    status: "已处理"
  });
}

function renderTodayStats() {
  const today = formatDateTime().slice(0, 10);
  const todayAlarms = alarmLogs.value.filter((record) => {
    return record.alarmTime.startsWith(today) && ["中风险", "高风险"].includes(record.riskLevel);
  });

  todayAlarmCount.value = todayAlarms.length;
  handledAlarmCount.value = todayAlarms.filter((record) => record.status === "已处理").length;
  yesterdayDiff.value = todayAlarms.length >= 2 ? `+${todayAlarms.length - 1}` : "0";
}

function appendTrendPoint() {
  chartData.value = [
    ...chartData.value,
    {
      time: formatClock().slice(0, 5),
      value: smokeValue.value
    }
  ].slice(-getMaxPoints());
}

function syncLatestTrendPoint() {
  if (!chartData.value.length) return;

  chartData.value = chartData.value.map((item, index) => {
    if (index !== chartData.value.length - 1) return item;
    return {
      time: formatClock().slice(0, 5),
      value: smokeValue.value
    };
  });
}

function getMaxPoints() {
  const maxPoints = {
    realtime: 16,
    "6h": 13,
    "12h": 13,
    "24h": 25,
    "7d": 8
  };
  return maxPoints[selectedTimeRange.value] || 25;
}

function initChart() {
  if (!smokeChartRef.value) return;
  smokeChart = echarts.init(smokeChartRef.value);
}

function renderChart() {
  if (!smokeChart) return;

  smokeChart.setOption(
    {
      color: ["#65ff9a", "#ffe14f", "#ff5a57"],
      grid: { top: 54, right: 28, bottom: 42, left: 46 },
      tooltip: {
        trigger: "axis",
        backgroundColor: "rgba(6, 24, 31, 0.88)",
        borderColor: "rgba(255,255,255,0.18)",
        textStyle: { color: "#fff" }
      },
      legend: {
        top: 12,
        textStyle: { color: "rgba(255,255,255,0.86)" },
        data: ["烟雾浓度(ppm)", "中风险阈值(200ppm)", "高风险阈值(400ppm)"]
      },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: chartData.value.map((item) => item.time),
        axisLine: { lineStyle: { color: "rgba(255,255,255,0.32)" } },
        axisLabel: { color: "rgba(255,255,255,0.72)" },
        splitLine: { show: true, lineStyle: { color: "rgba(255,255,255,0.08)" } }
      },
      yAxis: {
        type: "value",
        min: 0,
        max: 520,
        name: "ppm",
        nameTextStyle: { color: "rgba(255,255,255,0.7)" },
        axisLabel: { color: "rgba(255,255,255,0.72)" },
        splitLine: { lineStyle: { color: "rgba(255,255,255,0.12)" } }
      },
      series: [
        {
          name: "烟雾浓度(ppm)",
          type: "line",
          smooth: true,
          symbolSize: 7,
          data: chartData.value.map((item) => item.value),
          areaStyle: { color: "rgba(101,255,154,0.16)" },
          lineStyle: { width: 3 },
          markLine: {
            silent: true,
            symbol: "none",
            label: { color: "rgba(255,255,255,0.76)" },
            data: [
              {
                yAxis: middleRiskThreshold.value,
                name: "中风险阈值",
                lineStyle: { color: "#ffe14f", type: "dashed", width: 1.5 }
              },
              {
                yAxis: highRiskThreshold.value,
                name: "高风险阈值",
                lineStyle: { color: "#ff5a57", type: "dashed", width: 1.5 }
              }
            ]
          }
        },
        {
          name: "中风险阈值(200ppm)",
          type: "line",
          showSymbol: false,
          data: chartData.value.map(() => middleRiskThreshold.value),
          lineStyle: { color: "#ffe14f", type: "dashed", width: 1 },
          tooltip: { show: false }
        },
        {
          name: "高风险阈值(400ppm)",
          type: "line",
          showSymbol: false,
          data: chartData.value.map(() => highRiskThreshold.value),
          lineStyle: { color: "#ff5a57", type: "dashed", width: 1 },
          tooltip: { show: false }
        }
      ]
    },
    true
  );
}

function resizeChart() {
  if (smokeChart) smokeChart.resize();
}

function getRiskClass(value) {
  if (value === "高风险") return "high";
  if (value === "中风险") return "medium";
  return "low";
}
</script>
