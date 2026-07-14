/**
 * 鹦鹉行为识别 API：上传图片到 /api/parrot/behavior。
 *
 * 后端返回 { code, message, data }，data 含：
 *   { deviceId, parrotDetected, parrotConfidence, behavior, behaviorConfidence, imageUrl, checkedAt }
 *
 * 走 request.js 的共享 axios 实例（http）：拦截器统一处理 Bearer token、信封解包、错误文案。
 * FormData 上传不手动设 Content-Type，由 axios 自动带 multipart boundary。
 * 成功返回 data，失败抛 Error。
 */
import { http } from './request'

export async function recognizeParrotBehavior(file, deviceId) {
  if (!file) throw new Error('请先选择或拍摄一张鹦鹉图片')
  const fd = new FormData()
  fd.append('file', file)
  if (deviceId) fd.append('deviceId', deviceId)
  return http.post('/parrot/behavior', fd)
}

/**
 * 3D 模拟模式 VLM 识别：发送 3D canvas 截图（base64 JPEG）给后端 Qwen-VL。
 * 后端返回 { species, behavior, confidence }。
 */
export async function analyzeWithVlm(base64Image) {
  if (!base64Image) throw new Error('缺少 image 数据')
  return http.post('/parrot/vision/vlm', { image: base64Image })
}
