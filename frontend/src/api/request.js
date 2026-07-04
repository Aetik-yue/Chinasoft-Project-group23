/**
 * 统一 HTTP 请求封装（基于浏览器原生 fetch，无需 axios）。
 *
 * 后端约定（见《智慧烟感API接口文档》2.1）：
 *   所有接口返回 { code, message, data }，code=0 成功，非 0 失败。
 *   BaseURL 为 /api，由 vite.config.js 的 proxy 转发到 Spring Boot 8080。
 *
 * request() 成功时直接返回 data（已解包），失败时抛 Error。
 */

const BASE = '/api'

function buildQuery(query) {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, value)
    }
  }
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

export async function request(path, { method = 'GET', query, body } = {}) {
  const url = `${BASE}${path}${buildQuery(query)}`
  const opts = { method, headers: {} }

  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json;charset=utf-8'
    opts.body = JSON.stringify(body)
  }

  let res
  try {
    res = await fetch(url, opts)
  } catch (e) {
    throw new Error(`网络请求失败：${url}（${e.message}）`)
  }

  if (!res.ok) {
    throw new Error(`HTTP ${res.status} ${res.statusText} @ ${url}`)
  }

  const payload = await res.json()
  if (payload && typeof payload.code === 'number' && payload.code !== 0) {
    throw new Error(payload.message || `业务错误 code=${payload.code}`)
  }
  return payload?.data
}
