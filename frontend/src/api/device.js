import { request } from './request'

/**
 * 设备联动控制接口（对应后端 /api/device/*）。
 * 用于"安全联动"面板：烟雾超标时一键控制蜂鸣器/报警灯/排风扇保护鹦鹉。
 * - controlDevice({ deviceId, deviceType, status })：deviceType ∈ buzzer/alarm_light/fan
 * - getDeviceStatus(deviceId)
 */

export const controlDevice = (body) =>
  request('/device/control', { method: 'POST', body })

export const getDeviceStatus = (deviceId) =>
  request('/device/status', { query: { deviceId } })

export const getDeviceInfo = (deviceId) =>
  request('/device/info', { query: { deviceId } })
