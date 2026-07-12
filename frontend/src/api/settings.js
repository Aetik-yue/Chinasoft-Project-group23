import { request } from './request'

/**
 * 系统阈值配置接口（对应后端 /api/settings/threshold）。
 * 修改烟雾报警上限时调用，postData 服务会轮询发现变化并推送到板子。
 */
export const updateThreshold = (body) =>
  request('/settings/threshold', { method: 'POST', body })
