import { request } from './request'

/**
 * 查询鹦鹉笼舍预聚合的小时报表（温度 / 小时均湿度 / 粉尘），成长报告主路径。
 * 返回按时间升序排列的小时聚合序列，每项形如 { time, temperature, humidity, dust }，
 * 缺项为 null；无采样的时段不会出现在结果中（前端据此断开折线）。
 *
 * @param {string} deviceId 设备编号（鹦鹉笼舍绑定的监测设备）
 * @param {string} range 时间范围：24h / 7d / 30d
 */
export const getEnvironmentHourly = (deviceId, range) =>
  request('/environment/hourly', { query: { deviceId, range } })
