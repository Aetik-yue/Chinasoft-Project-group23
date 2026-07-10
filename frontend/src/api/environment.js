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

/**
 * 按自然日/周/月读取成长报告数据（只返回指定或最近一个已结束的完整周期）。
 * 返回 [{ time, temperature, humidity, dust }]：
 *   - daily：每小时一点（最多 24 点）
 *   - weekly / monthly：每天一点
 * @param {string} deviceId 设备编号
 * @param {string} range daily / weekly / monthly
 * @param {string} date YYYY-MM-DD；为空时 daily=昨天、weekly=上周日、monthly=上月末
 */
export const getEnvironmentReport = (deviceId, range, date) =>
  request('/environment/report', { query: { deviceId, range, date } })
