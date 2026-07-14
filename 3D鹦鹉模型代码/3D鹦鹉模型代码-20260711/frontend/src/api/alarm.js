import { request } from './request'

/**
 * 告警相关接口（对应后端 /api/alarm/*）。
 * - getTodayAlarmStat(): AlarmTodayStatResponse { todayCount, yesterdayCount, changeRate }
 * - getAlarmLogs(params): 告警记录列表，支持 { limit, page, pageSize, deviceId, status, level }
 */

export const getTodayAlarmStat = () =>
  request('/alarm/stat/today')

export const getAlarmLogs = (params) =>
  request('/alarm/logs', { query: params })
