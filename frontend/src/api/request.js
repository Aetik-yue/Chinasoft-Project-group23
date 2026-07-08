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
  const token = localStorage.getItem('parrotAuthToken')

  if (token) {
    opts.headers.Authorization = `Bearer ${token}`
  }

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

  // 后端无论 HTTP 状态码是什么，body 都尽量按 { code, message, data } 返回。
  // 所以先解析 body，优先取业务 message；只在 body 读不到文案时再降级为 HTTP 通用错误。
  let payload = null
  try {
    payload = await res.json()
  } catch {
    // 空 body 或非 JSON 响应时 payload 保持 null
  }

  if (!res.ok) {
    const backendMessage = payload && payload.message
    throw new Error(backendMessage || `HTTP ${res.status} ${res.statusText} @ ${url}`)
  }

  if (payload && typeof payload.code === 'number' && payload.code !== 0) {
    throw new Error(payload.message || `业务错误 code=${payload.code}`)
  }
  return payload?.data
}
