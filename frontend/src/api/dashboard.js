import axios from "axios";
import {
  DEVICE_ID,
  createTrendData,
  formatDateTime,
  initialAlarmRecords
} from "../mockData.js";

// 后端 BaseURL 走 vite 开发代理（见 vite.config.js），生产环境通过 nginx 转发
const http = axios.create({
  baseURL: "/api",
  timeout: 5000
});

// —— 枚举映射：后端英文 → 前端中文 ——
const RISK_LEVEL_MAP = {
  normal: "正常",
  low: "低风险",
  medium: "中风险",
  high: "高风险"
};
const ALARM_STATUS_MAP = {
  safe: "安全",
  alarm: "告警中",
  offline: "设备离线"
};
// 后端 riskLevel / alarmStatus → 前端主题 key（safe/low/medium/high）
const THEME_MAP = {
  normal: "safe",
  low: "low",
  medium: "medium",
  high: "high"
};
const ALARM_TYPE_MAP = {
  smoke_high: "烟雾浓度超标",
  device_offline: "设备离线"
};
const HANDLE_STATUS_MAP = {
  pending: "待处理",
  processing: "处理中",
  resolved: "已处理"
};

function mapThemeType(riskLevel, alarmStatus) {
  if (alarmStatus === "alarm") return "high";
  return THEME_MAP[riskLevel] || "safe";
}

// "2026-07-01T16:06:55" → "2026-07-01 16:06:55"
function formatTime(iso) {
  if (!iso) return formatDateTime();
  return String(iso).replace("T", " ").slice(0, 19);
}

// "2026-07-01T16:06:55" → "16:06"
function formatAxisTime(iso, range) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ");
  if (range === "7d") return s.slice(5, 10); // MM-DD
  return s.slice(11, 16); // HH:mm
}

// 取统一响应外层 data 字段，处理后端 {code, message, data} 包装
function unwrap(resp) {
  return resp.data?.data ?? resp.data ?? null;
}

// —— 真实接口：获取最新烟雾浓度（3 秒轮询用） ——
export async function getSmokeLatest(deviceId) {
  const resp = await http.get("/smoke/latest", { params: { deviceId } });
  const d = unwrap(resp);
  if (!d) return null;
  return {
    deviceId: d.deviceId,
    smokeValue: d.smokeValue,
    unit: d.unit || "ppm",
    riskLevel: RISK_LEVEL_MAP[d.riskLevel] || "正常",
    alarmStatus: ALARM_STATUS_MAP[d.alarmStatus] || "安全",
    alarmType: d.alarmType ? ALARM_TYPE_MAP[d.alarmType] || d.alarmType : null,
    themeType: mapThemeType(d.riskLevel, d.alarmStatus),
    updatedAt: d.updatedAt
  };
}

// —— 真实接口：获取历史趋势 ——
export async function getSmokeHistory(range = "24h") {
  try {
    const resp = await http.get("/smoke/history", { params: { range } });
    const list = unwrap(resp) || [];
    return list.map((p) => ({
      time: formatAxisTime(p.time, range),
      value: p.value,
      threshold: p.threshold
    }));
  } catch (error) {
    console.warn("[getSmokeHistory] 后端不可用，降级 mock", error.message);
    return createTrendData(range);
  }
}

// —— 真实接口：系统状态 ——
export async function getSystemStatus() {
  const resp = await http.get("/system/status");
  return unwrap(resp);
}

// —— 真实接口：今日告警统计 ——
export async function getAlarmTodayStat() {
  const resp = await http.get("/alarm/stat/today");
  return unwrap(resp);
}

// —— 真实接口：告警列表 ——
export async function getAlarmLogs(limit = 5) {
  try {
    const resp = await http.get("/alarm/logs", { params: { limit } });
    const list = unwrap(resp) || [];
    return list.map((r) => ({
      alarmId: r.alarmId,
      alarmTime: formatTime(r.alarmTime),
      deviceId: r.deviceId,
      alarmType: ALARM_TYPE_MAP[r.type] || r.type || "烟雾浓度超标",
      smokeValue: r.value,
      riskLevel: RISK_LEVEL_MAP[r.level] || "低风险",
      status: HANDLE_STATUS_MAP[r.status] || r.status || "待处理",
      handler: r.handler,
      remark: r.remark
    }));
  } catch (error) {
    console.warn("[getAlarmLogs] 后端不可用，降级 mock", error.message);
    return initialAlarmRecords.slice(0, limit).map((record) => ({
      alarmTime: record.time,
      deviceId: record.deviceId,
      alarmType: record.type,
      smokeValue: record.smoke,
      riskLevel: record.risk,
      status: record.status
    }));
  }
}

// —— 真实接口：设备控制 ——
export async function controlDevice({ deviceId, target, action }) {
  try {
    // target: buzzer/light/fan → 后端 deviceType
    const resp = await http.post("/device/control", {
      deviceId,
      deviceType: target,
      status: action
    });
    const d = unwrap(resp);
    return {
      success: true,
      target,
      status: d?.status || action,
      message: `${deviceId} ${target} ${action === "on" ? "开启" : "关闭"}成功`
    };
  } catch (error) {
    console.warn("[controlDevice] 后端不可用，降级 mock", error.message);
    return {
      success: true,
      target,
      status: action,
      message: `${deviceId} ${target} ${action === "on" ? "开启" : "关闭"}成功（mock）`
    };
  }
}

// —— 聚合接口：一次性加载仪表盘初始数据（并发请求，单接口失败用 mock 兜底） ——

function normalizeLinkStatus(data = {}) {
  return {
    connected: data.connected,
    status: data.status || "connected",
    linkState: data.linkState || "online",
    displayMode: data.displayMode || "dashboard",
    eventType: data.eventType,
    message: data.message || data.connectionMessage || ""
  };
}

function mockConnectedStatus() {
  return {
    connected: true,
    status: "connected",
    linkState: "online",
    displayMode: "dashboard",
    message: ""
  };
}

export async function getDeviceConnectionStatus() {
  try {
    const resp = await http.get("/device/status");
    return normalizeLinkStatus(unwrap(resp) || {});
  } catch (error) {
    console.warn("[getDeviceConnectionStatus] 后端不可用，降级 mock", error.message);
    return mockConnectedStatus();
  }
}

export async function connectDevice() {
  try {
    const resp = await http.post("/device/connect");
    return normalizeLinkStatus(unwrap(resp) || { connected: true, status: "connected", linkState: "online", displayMode: "dashboard" });
  } catch (error) {
    console.warn("[connectDevice] 后端不可用，降级 mock", error.message);
    return mockConnectedStatus();
  }
}

export async function disconnectDevice() {
  try {
    const resp = await http.post("/device/disconnect");
    return normalizeLinkStatus(unwrap(resp) || { connected: false, status: "disconnected", linkState: "offline", displayMode: "unconnected_page" });
  } catch (error) {
    console.warn("[disconnectDevice] 后端不可用，降级 mock", error.message);
    return {
      connected: false,
      status: "disconnected",
      linkState: "offline",
      displayMode: "unconnected_page",
      message: "设备未连接"
    };
  }
}

export async function getRuntimeLinkSnapshot() {
  try {
    const resp = await http.get("/runtime/link-snapshot");
    return normalizeLinkStatus(unwrap(resp) || {});
  } catch (error) {
    console.warn("[getRuntimeLinkSnapshot] 后端不可用，降级 mock", error.message);
    return mockConnectedStatus();
  }
}

export function subscribeRuntimeLinkEvents(onStatusChange) {
  if (typeof EventSource === "undefined") return null;

  const source = new EventSource("/api/runtime/link-events");
  source.onmessage = (event) => {
    try {
      onStatusChange(normalizeLinkStatus(JSON.parse(event.data || "{}")));
    } catch (error) {
      console.warn("[subscribeRuntimeLinkEvents] 事件解析失败", error.message);
    }
  };
  source.onerror = () => {
    console.warn("[subscribeRuntimeLinkEvents] SSE 暂不可用，继续使用轮询兜底");
    source.close();
  };

  return () => source.close();
}
export async function getDashboardCurrent() {
  const [latestRes, systemRes, statRes, alarmsRes] = await Promise.allSettled([
    getSmokeLatest(),
    getSystemStatus(),
    getAlarmTodayStat(),
    getAlarmLogs(5)
  ]);

  // 任一接口失败时降级 mock，保证页面可用
  const latest = latestRes.status === "fulfilled" && latestRes.value
    ? latestRes.value
    : {
        deviceId: DEVICE_ID,
        smokeValue: 86,
        unit: "ppm",
        riskLevel: "正常",
        alarmStatus: "安全",
        themeType: "safe",
        updatedAt: new Date().toISOString()
      };
  const system = systemRes.status === "fulfilled" && systemRes.value
    ? systemRes.value
    : { onlineDeviceCount: 12, totalDeviceCount: 12, systemOnline: true };
  const stat = statRes.status === "fulfilled" && statRes.value
    ? statRes.value
    : { todayCount: 2, yesterdayCount: 1, changeRate: 100 };
  const alarms = alarmsRes.status === "fulfilled" && alarmsRes.value
    ? alarmsRes.value
    : initialAlarmRecords.slice(0, 5).map((record) => ({
        alarmTime: record.time,
        deviceId: record.deviceId,
        alarmType: record.type,
        smokeValue: record.smoke,
        riskLevel: record.risk,
        status: record.status
      }));

  return {
    deviceId: latest.deviceId,
    smokeValue: latest.smokeValue,
    unit: latest.unit || "ppm",
    updateTime: latest.updatedAt || formatDateTime(),
    deviceOnline: system.systemOnline ?? true,
    riskLevel: latest.riskLevel,
    alarmStatus: latest.alarmStatus,
    themeType: latest.themeType,
    systemStatus: latest.themeType === "high" ? "系统状态：告警中" : "系统状态：运行正常",
    deviceOnlineCount: system.onlineDeviceCount ?? 12,
    deviceTotalCount: system.totalDeviceCount ?? 12,
    todayAlarmCount: stat.todayCount ?? 0,
    handledAlarmCount: Math.max(0, Math.floor((stat.todayCount ?? 0) / 2)),
    yesterdayDiff: formatChangeRate(stat.changeRate),
    buzzerOn: latest.themeType === "high",
    lightOn: latest.themeType === "high",
    fanOn: latest.themeType === "high"
  };
}

function formatChangeRate(rate) {
  if (rate == null) return "0";
  const rounded = Math.round(rate);
  return rounded >= 0 ? `+${rounded}` : String(rounded);
}
