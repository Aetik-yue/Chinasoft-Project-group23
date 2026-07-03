import {
  DEVICE_ID,
  RISK_THRESHOLDS,
  createTrendData,
  formatClock,
  formatDateTime,
  getRiskKey,
  initialAlarmRecords,
  nextSmokeValue,
  presetSmokeValue,
  riskCopy
} from "./mockData.js";

const state = {
  currentSmoke: 86,
  mode: "safe",
  currentRange: "24h",
  trendData: createTrendData("24h"),
  devices: {
    buzzer: false,
    alarmLight: false,
    fan: false
  },
  alarmRecords: [...initialAlarmRecords]
};

const el = {
  body: document.body,
  currentTime: document.querySelector("#currentTime"),
  currentSmoke: document.querySelector("#currentSmoke"),
  updateTime: document.querySelector("#updateTime"),
  deviceId: document.querySelector("#deviceId"),
  riskLevel: document.querySelector("#riskLevel"),
  riskDescription: document.querySelector("#riskDescription"),
  alarmStatus: document.querySelector("#alarmStatus"),
  alarmDescription: document.querySelector("#alarmDescription"),
  todayAlarmCount: document.querySelector("#todayAlarmCount"),
  handledCount: document.querySelector("#handledCount"),
  dayChange: document.querySelector("#dayChange"),
  sideSystemStatus: document.querySelector("#sideSystemStatus"),
  onlineStatus: document.querySelector("#onlineStatus"),
  alarmTableBody: document.querySelector("#alarmTableBody"),
  riskPreset: document.querySelector("#riskPreset"),
  rangeTabs: document.querySelector("#rangeTabs"),
  raiseSmokeBtn: document.querySelector("#raiseSmokeBtn"),
  restoreBtn: document.querySelector("#restoreBtn")
};

let chart;
let fallbackCanvas;

function init() {
  el.deviceId.textContent = DEVICE_ID;
  initChart();
  bindEvents();
  updateClock();
  renderAll();

  setInterval(updateClock, 1000);
  setInterval(refreshSmokeValue, 3000);
  window.addEventListener("resize", resizeChart);
}

function bindEvents() {
  el.riskPreset.addEventListener("change", (event) => {
    const riskKey = event.target.value;
    applyRiskPreset(riskKey);
  });

  document.querySelectorAll(".device-toggle").forEach((button) => {
    button.addEventListener("click", () => {
      const key = button.dataset.device;
      state.devices[key] = !state.devices[key];
      renderDevices();
    });
  });

  el.raiseSmokeBtn.addEventListener("click", () => {
    state.currentSmoke = 452 + Math.round(Math.random() * 22);
    state.mode = "alarm";
    state.devices.buzzer = true;
    state.devices.alarmLight = true;
    state.devices.fan = true;
    addAlarmRecord("设备联动告警");
    syncLatestTrendPoint();
    renderAll();
  });

  el.restoreBtn.addEventListener("click", () => {
    state.currentSmoke = 60 + Math.round(Math.random() * 16);
    state.mode = "safe";
    state.devices.buzzer = false;
    state.devices.alarmLight = false;
    state.devices.fan = false;
    syncLatestTrendPoint();
    renderAll();
  });

  el.rangeTabs.addEventListener("click", (event) => {
    const button = event.target.closest("button");
    if (!button) return;

    state.currentRange = button.dataset.range;
    state.trendData = createTrendData(state.currentRange);
    syncLatestTrendPoint();
    document.querySelectorAll("#rangeTabs button").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    renderChart();
  });
}

function applyRiskPreset(riskKey) {
  state.mode = riskKey === "high" ? "alarm" : riskKey;
  state.currentSmoke = presetSmokeValue(riskKey);

  if (riskKey === "high") {
    state.devices.buzzer = true;
    state.devices.alarmLight = true;
    state.devices.fan = true;
    addAlarmRecord("设备联动告警");
  }

  syncLatestTrendPoint();
  renderAll();
}

function refreshSmokeValue() {
  state.currentSmoke = nextSmokeValue(state.mode, state.currentSmoke);

  const riskKey = getRiskKey(state.currentSmoke);
  if (riskKey === "medium" || riskKey === "high") {
    const last = state.alarmRecords[0];
    const lastMinute = last ? last.time.slice(0, 16) : "";
    const currentMinute = formatDateTime().slice(0, 16);
    if (lastMinute !== currentMinute) addAlarmRecord("烟雾浓度超标");
  }

  appendTrendPoint();
  renderAll();
}

function initChart() {
  const chartEl = document.querySelector("#smokeChart");
  if (window.echarts) {
    chart = window.echarts.init(chartEl);
    return;
  }

  fallbackCanvas = document.createElement("canvas");
  fallbackCanvas.className = "fallback-chart";
  chartEl.appendChild(fallbackCanvas);
}

function renderAll() {
  const riskKey = getRiskKey(state.currentSmoke);
  const copy = riskCopy[riskKey];
  const now = new Date();

  el.body.className = `${riskKey}-theme`;
  el.currentSmoke.textContent = state.currentSmoke;
  el.updateTime.textContent = formatClock(now);
  el.riskLevel.textContent = copy.level;
  el.riskDescription.textContent = copy.description;
  el.alarmStatus.textContent = copy.alarm;
  el.alarmDescription.textContent = copy.alarmDescription;
  el.sideSystemStatus.textContent = riskKey === "high" ? "系统状态：告警中" : "系统状态：运行正常";
  el.onlineStatus.textContent = riskKey === "high" ? "告警联动中" : "系统运行正常";
  el.riskPreset.value = riskKey;

  renderTodayStats();
  renderDevices();
  renderTable();
  renderChart();
}

function renderDevices() {
  document.querySelectorAll(".device-toggle").forEach((button) => {
    const key = button.dataset.device;
    const isOn = state.devices[key];
    button.classList.toggle("on", isOn);
    button.querySelector("em").textContent = isOn ? "开" : "关";
  });
}

function renderTodayStats() {
  const today = formatDateTime().slice(0, 10);
  const todayAlarms = state.alarmRecords.filter((record) => {
    return record.time.startsWith(today) && ["中风险", "高风险"].includes(record.risk);
  });

  el.todayAlarmCount.textContent = todayAlarms.length;
  el.handledCount.textContent = todayAlarms.filter((record) => record.status === "已处理").length;
  el.dayChange.textContent = todayAlarms.length >= 2 ? `+${todayAlarms.length - 1}` : "0";
}

function renderTable() {
  el.alarmTableBody.innerHTML = state.alarmRecords
    .slice(0, 5)
    .map((record) => {
      const riskClass = record.risk === "高风险" ? "high" : record.risk === "中风险" ? "medium" : "low";
      return `
        <tr>
          <td>${record.time}</td>
          <td>${record.deviceId}</td>
          <td>${record.type}</td>
          <td>${record.smoke} ppm</td>
          <td><span class="tag ${riskClass}">${record.risk}</span></td>
          <td><span class="status-tag">${record.status}</span></td>
          <td><button class="detail-btn" type="button">查看详情</button></td>
        </tr>
      `;
    })
    .join("");
}

function renderChart() {
  if (chart) {
    chart.setOption(
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
          data: state.trendData.map((item) => item.time),
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
            data: state.trendData.map((item) => item.value),
            areaStyle: { color: "rgba(101,255,154,0.16)" },
            lineStyle: { width: 3 },
            markLine: {
              silent: true,
              symbol: "none",
              label: { color: "rgba(255,255,255,0.76)" },
              data: [
                {
                  yAxis: RISK_THRESHOLDS.lowMax,
                  name: "中风险阈值",
                  lineStyle: { color: "#ffe14f", type: "dashed", width: 1.5 }
                },
                {
                  yAxis: RISK_THRESHOLDS.mediumMax,
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
            data: state.trendData.map(() => RISK_THRESHOLDS.lowMax),
            lineStyle: { color: "#ffe14f", type: "dashed", width: 1 },
            tooltip: { show: false }
          },
          {
            name: "高风险阈值(400ppm)",
            type: "line",
            showSymbol: false,
            data: state.trendData.map(() => RISK_THRESHOLDS.mediumMax),
            lineStyle: { color: "#ff5a57", type: "dashed", width: 1 },
            tooltip: { show: false }
          }
        ]
      },
      true
    );
    return;
  }

  drawFallbackChart();
}

function drawFallbackChart() {
  if (!fallbackCanvas) return;

  const parent = fallbackCanvas.parentElement;
  const rect = parent.getBoundingClientRect();
  const ratio = window.devicePixelRatio || 1;
  fallbackCanvas.width = rect.width * ratio;
  fallbackCanvas.height = rect.height * ratio;
  fallbackCanvas.style.width = `${rect.width}px`;
  fallbackCanvas.style.height = `${rect.height}px`;

  const ctx = fallbackCanvas.getContext("2d");
  ctx.scale(ratio, ratio);
  ctx.clearRect(0, 0, rect.width, rect.height);
  ctx.strokeStyle = "rgba(255,255,255,0.14)";
  ctx.lineWidth = 1;

  const padding = { top: 28, right: 20, bottom: 28, left: 40 };
  const width = rect.width - padding.left - padding.right;
  const height = rect.height - padding.top - padding.bottom;

  [0, 100, 200, 300, 400, 500].forEach((tick) => {
    const y = padding.top + height - (tick / 520) * height;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(padding.left + width, y);
    ctx.stroke();
  });

  drawThreshold(ctx, padding, width, height, RISK_THRESHOLDS.lowMax, "#ffe14f");
  drawThreshold(ctx, padding, width, height, RISK_THRESHOLDS.mediumMax, "#ff5a57");

  ctx.strokeStyle = "#65ff9a";
  ctx.lineWidth = 3;
  ctx.beginPath();
  state.trendData.forEach((point, index) => {
    const x = padding.left + (index / (state.trendData.length - 1)) * width;
    const y = padding.top + height - (point.value / 520) * height;
    if (index === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.stroke();
}

function drawThreshold(ctx, padding, width, height, value, color) {
  const y = padding.top + height - (value / 520) * height;
  ctx.save();
  ctx.strokeStyle = color;
  ctx.setLineDash([6, 6]);
  ctx.beginPath();
  ctx.moveTo(padding.left, y);
  ctx.lineTo(padding.left + width, y);
  ctx.stroke();
  ctx.restore();
}

function updateClock() {
  el.currentTime.textContent = formatDateTime();
}

function appendTrendPoint() {
  state.trendData.push({
    time: formatClock().slice(0, 5),
    value: state.currentSmoke
  });
  state.trendData = state.trendData.slice(-getMaxPoints());
}

function syncLatestTrendPoint() {
  state.trendData[state.trendData.length - 1] = {
    time: formatClock().slice(0, 5),
    value: state.currentSmoke
  };
}

function addAlarmRecord(type) {
  const riskKey = getRiskKey(state.currentSmoke);
  const copy = riskCopy[riskKey];

  state.alarmRecords.unshift({
    time: formatDateTime(),
    deviceId: DEVICE_ID,
    type,
    smoke: state.currentSmoke,
    risk: copy.level,
    status: "已处理"
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
  return maxPoints[state.currentRange] || 25;
}

function resizeChart() {
  if (chart) chart.resize();
  else drawFallbackChart();
}

init();
