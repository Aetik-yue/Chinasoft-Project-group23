/**
 * 统一 HTTP 请求封装（基于 axios 实例）。
 *
 * 后端约定（见《智慧烟感API接口文档》2.1）：
 *   所有接口返回 { code, message, data }，code=0 成功，非 0 失败。
 *   BaseURL 为 /api，由 vite.config.js 的自定义代理转发到 Spring Boot 8080。
 *
 * request() 成功时直接返回 data（已解包），失败时抛 Error。
 * 错误文案与历史 fetch 版本保持一致：
 *   - 网络失败：`网络请求失败：/api<path>?<query>（<errMsg>）`
 *   - HTTP 非 2xx：`<后端 message> || HTTP <status> <statusText> @ /api<path>?<query>`
 *   - 业务码非 0：`<message> || 业务错误 code=<code>`
 */

import axios from 'axios'

const BASE = '/api'

function buildQuery(params) {
  if (!params) return ''
  const sp = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      sp.append(key, value)
    }
  }
  const qs = sp.toString()
  return qs ? `?${qs}` : ''
}

export const http = axios.create({
  baseURL: BASE,
  // 与历史 buildQuery 行为一致：跳过 undefined/null/''，用 URLSearchParams 编码。
  paramsSerializer: {
    serialize: (params) => buildQuery(params).replace(/^\?/, ''),
  },
})

// 请求拦截：附 Bearer token（FormData 请求不手动设 Content-Type，让 axios 自动带 boundary）。
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('parrotAuthToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function fullUrlOf(cfg) {
  const base = (cfg && cfg.baseURL) || ''
  const url = (cfg && cfg.url) || ''
  return `${base}${url}${buildQuery(cfg && cfg.params)}`
}

// 响应拦截：解包 { code, message, data }；失败按现状格式化文案。
http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      throw new Error(payload.message || `业务错误 code=${payload.code}`)
    }
    return payload?.data
  },
  (error) => {
    const url = fullUrlOf(error.config)
    if (error.response) {
      const { status, statusText } = error.response
      const backendMessage = error.response.data && error.response.data.message
      throw new Error(backendMessage || `HTTP ${status} ${statusText} @ ${url}`)
    }
    // 无 response：网络不可达 / 超时 / 取消等
    throw new Error(`网络请求失败：${url}（${error.message}）`)
  },
)

export async function request(path, { method = 'GET', query, body } = {}) {
  return http({
    url: path,
    method,
    params: query,
    data: body,
  })
}
