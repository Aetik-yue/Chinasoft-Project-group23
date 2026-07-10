/**
 * 鹦鹉行为识别 API：上传图片到 /api/parrot/behavior。
 *
 * 后端返回 { code, message, data }，data 含：
 *   { deviceId, parrotDetected, parrotConfidence, behavior, behaviorConfidence, imageUrl, checkedAt }
 *
 * 用 FormData 上传文件（不走 request.js 的 JSON 封装），由 vite.config.js 的 /api 代理转发到 Spring Boot 8080。
 * 成功返回 data，失败抛 Error。
 */
const BASE = '/api'

export async function recognizeParrotBehavior(file, deviceId) {
  if (!file) throw new Error('请先选择或拍摄一张鹦鹉图片')
  const fd = new FormData()
  fd.append('file', file)
  if (deviceId) fd.append('deviceId', deviceId)

  let res
  try {
    res = await fetch(`${BASE}/parrot/behavior`, { method: 'POST', body: fd })
  } catch (e) {
    throw new Error(`网络请求失败：/api/parrot/behavior（${e.message}）`)
  }

  let payload = null
  try {
    payload = await res.json()
  } catch (e) {
    // 非 JSON 响应（如网关错误页）
  }
  if (!res.ok || (payload && typeof payload.code === 'number' && payload.code !== 0)) {
    throw new Error(payload?.message || `HTTP ${res.status} ${res.statusText} @ /api/parrot/behavior`)
  }
  return payload?.data
}

/**
 * 3D 模拟模式 VLM 识别：发送 3D canvas 截图（base64 JPEG）给后端 Qwen-VL。
 * 后端返回 { species, behavior, confidence }。
 */
export async function analyzeWithVlm(base64Image) {
  if (!base64Image) throw new Error('缺少 image 数据')
  let res
  try {
    res = await fetch(`${BASE}/parrot/vision/vlm`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image: base64Image }),
    })
  } catch (e) {
    throw new Error(`网络请求失败：/api/parrot/vision/vlm（${e.message}）`)
  }
  let payload = null
  try { payload = await res.json() } catch { /* 非 JSON 响应 */ }
  if (!res.ok || (payload && typeof payload.code === 'number' && payload.code !== 0)) {
    throw new Error(payload?.message || `HTTP ${res.status} ${res.statusText} @ /api/parrot/vision/vlm`)
  }
  return payload?.data
}
