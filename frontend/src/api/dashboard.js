import {
  DEVICE_ID,
  createTrendData,
  formatDateTime,
  initialAlarmRecords
} from "../mockData.js";

export async function getDashboardCurrent() {
  return {
    deviceId: DEVICE_ID,
    smokeValue: 86,
    unit: "ppm",
    updateTime: formatDateTime(),
    deviceOnline: true,
    riskLevel: "正常",
    alarmStatus: "安全",
    themeType: "safe",
    systemStatus: "系统状态：运行正常",
    deviceOnlineCount: 12,
    deviceTotalCount: 12,
    todayAlarmCount: 2,
    handledAlarmCount: 2,
    yesterdayDiff: "+1",
    buzzerOn: false,
    lightOn: false,
    fanOn: false
  };
}

export async function getSmokeHistory(range = "24h") {
  return createTrendData(range).map((item) => ({
    time: item.time,
    value: item.value
  }));
}

export async function getRecentAlarms(limit = 5) {
  return initialAlarmRecords.slice(0, limit).map((record) => ({
    alarmTime: record.time,
    deviceId: record.deviceId,
    alarmType: record.type,
    smokeValue: record.smoke,
    riskLevel: record.risk,
    status: record.status
  }));
}

export async function controlDevice({ deviceId, target, action }) {
  return {
    success: true,
    target,
    status: action,
    message: `${deviceId} ${target} ${action === "on" ? "开启" : "关闭"}成功`
  };
}
