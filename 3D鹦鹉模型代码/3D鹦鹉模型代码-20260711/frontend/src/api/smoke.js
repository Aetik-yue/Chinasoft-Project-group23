import { request } from './request'

/**
 * 烟雾浓度相关接口（对应后端 /api/smoke/*）。
 * SmokeRealtimeResponse 字段：deviceId, connected, smokeValue(ppm),
 *   temperature, humidity, riskLevel, alarmStatus, themeType, updateTime, message。
 * 注意：后端目前 temperature/humidity 返回 null，前端按"待接入"展示。
 */

export const getRealtimeSmoke = (deviceId) =>
  request('/smoke/realtime', { query: { deviceId } })

export const getLatestSmoke = (deviceId) =>
  request('/smoke/latest', { query: { deviceId } })

export const getSmokeHistory = (params) =>
  request('/smoke/history', { query: params })

export const simulateSmoke = (body) =>
  request('/smoke/simulate', { method: 'POST', body })

export const restoreSmoke = (body) =>
  request('/smoke/restore', { method: 'POST', body })
