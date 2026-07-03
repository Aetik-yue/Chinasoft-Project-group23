export const RISK_THRESHOLDS = {
  normalMax: 100,
  lowMax: 200,
  mediumMax: 400
};

export const DEVICE_ID = "SMOKE-001";

export const riskCopy = {
  safe: {
    level: "正常",
    alarm: "安全",
    description: "空气安全，请保持当前状态",
    alarmDescription: "系统运行正常"
  },
  low: {
    level: "低风险",
    alarm: "低风险",
    description: "烟雾浓度轻微升高，请继续观察",
    alarmDescription: "系统处于预警观察"
  },
  medium: {
    level: "中风险",
    alarm: "中风险",
    description: "烟雾浓度超过中风险阈值，建议现场确认",
    alarmDescription: "联动设备待命中"
  },
  high: {
    level: "高风险",
    alarm: "告警中",
    description: "烟雾浓度严重超标，请立即处理",
    alarmDescription: "蜂鸣器与报警灯已联动"
  }
};

export const initialAlarmRecords = [
  {
    time: "2026-06-30 09:15:32",
    deviceId: DEVICE_ID,
    type: "烟雾浓度超标",
    smoke: 218,
    risk: "中风险",
    status: "已处理"
  },
  {
    time: "2026-06-30 07:42:18",
    deviceId: DEVICE_ID,
    type: "烟雾浓度超标",
    smoke: 356,
    risk: "中风险",
    status: "已处理"
  },
  {
    time: "2026-06-29 21:06:44",
    deviceId: DEVICE_ID,
    type: "设备联动告警",
    smoke: 426,
    risk: "高风险",
    status: "已处理"
  },
  {
    time: "2026-06-29 18:22:09",
    deviceId: DEVICE_ID,
    type: "烟雾浓度超标",
    smoke: 238,
    risk: "中风险",
    status: "已处理"
  },
  {
    time: "2026-06-28 14:33:51",
    deviceId: DEVICE_ID,
    type: "烟雾浓度超标",
    smoke: 198,
    risk: "低风险",
    status: "已处理"
  }
];

const rangeConfigs = {
  realtime: { points: 16, stepMinutes: 1, base: 72, wave: 18 },
  "6h": { points: 13, stepMinutes: 30, base: 84, wave: 30 },
  "12h": { points: 13, stepMinutes: 60, base: 94, wave: 38 },
  "24h": { points: 25, stepMinutes: 60, base: 82, wave: 42 },
  "7d": { points: 8, stepMinutes: 1440, base: 96, wave: 52 }
};

export function getRiskKey(smoke) {
  if (smoke <= RISK_THRESHOLDS.normalMax) return "safe";
  if (smoke <= RISK_THRESHOLDS.lowMax) return "low";
  if (smoke <= RISK_THRESHOLDS.mediumMax) return "medium";
  return "high";
}

export function createTrendData(range = "24h") {
  const config = rangeConfigs[range] || rangeConfigs["24h"];
  const now = new Date();

  return Array.from({ length: config.points }, (_, index) => {
    const distance = config.points - 1 - index;
    const date = new Date(now.getTime() - distance * config.stepMinutes * 60 * 1000);
    const curve = Math.sin(index * 0.72) * config.wave;
    const pulse = index === Math.floor(config.points * 0.64) ? config.wave * 1.4 : 0;
    const value = Math.max(32, Math.round(config.base + curve + pulse + randomBetween(-10, 10)));

    return {
      time: formatAxisTime(date, range),
      value
    };
  });
}

export function formatDateTime(date = new Date()) {
  const yyyy = date.getFullYear();
  const mm = pad(date.getMonth() + 1);
  const dd = pad(date.getDate());
  const hh = pad(date.getHours());
  const mi = pad(date.getMinutes());
  const ss = pad(date.getSeconds());
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

export function formatClock(date = new Date()) {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function nextSmokeValue(mode, current) {
  if (mode === "alarm") return clamp(Math.round(current + randomBetween(-20, 24)), 430, 520);
  if (mode === "medium") return clamp(Math.round(current + randomBetween(-18, 18)), 230, 360);
  if (mode === "low") return clamp(Math.round(current + randomBetween(-12, 12)), 120, 190);
  return clamp(Math.round(current + randomBetween(-8, 8)), 45, 92);
}

export function presetSmokeValue(key) {
  const values = {
    safe: 86,
    low: 168,
    medium: 286,
    high: 452
  };
  return values[key] || values.safe;
}

function formatAxisTime(date, range) {
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  if (range === "7d") return `${month}-${day}`;
  return `${hour}:${minute}`;
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}
