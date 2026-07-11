import { request } from './request'

export const listParrots = () =>
  request('/parrots')

export const getParrot = (petId) =>
  request(`/parrots/${encodeURIComponent(petId)}`)

export const createParrot = (body) =>
  request('/parrots', { method: 'POST', body })

export const updateParrot = (petId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}`, { method: 'PUT', body })

export const listWeights = (petId) =>
  request(`/parrots/${encodeURIComponent(petId)}/weights`)

export const createWeight = (petId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/weights`, { method: 'POST', body })

export const updateWeight = (petId, id, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/weights/${encodeURIComponent(id)}`, { method: 'PUT', body })

export const listMedicalRecords = (petId) =>
  request(`/parrots/${encodeURIComponent(petId)}/medical-records`)

export const createMedicalRecord = (petId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/medical-records`, { method: 'POST', body })

export const updateMedicalRecord = (petId, recordId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/medical-records/${encodeURIComponent(recordId)}`, { method: 'PUT', body })

export const listLedgerRecords = (petId) =>
  request(`/parrots/${encodeURIComponent(petId)}/ledger-records`)

export const createLedgerRecord = (petId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/ledger-records`, { method: 'POST', body })

export const updateLedgerRecord = (petId, ledgerId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/ledger-records/${encodeURIComponent(ledgerId)}`, { method: 'PUT', body })

export const deleteLedgerRecord = (petId, ledgerId) =>
  request(`/parrots/${encodeURIComponent(petId)}/ledger-records/${encodeURIComponent(ledgerId)}`, { method: 'DELETE' })

export const listPhotos = (petId) =>
  request(`/parrots/${encodeURIComponent(petId)}/photos`)

export const createPhoto = (petId, body) =>
  request(`/parrots/${encodeURIComponent(petId)}/photos`, { method: 'POST', body })

export const deletePhoto = (petId, mediaId) =>
  request(`/parrots/${encodeURIComponent(petId)}/photos/${encodeURIComponent(mediaId)}`, { method: 'DELETE' })

export const deleteParrot = (petId) =>
  request(`/parrots/${encodeURIComponent(petId)}`, { method: 'DELETE' })

/** 今日行为统计：按 behavior 分组 count。 */
export const getBehaviorTodayStats = (deviceId) =>
  request('/parrot/behavior/today-stats', { query: { deviceId } })

/**
 * 今日睡眠时长汇总（占位实现）。
 * 后端接口补齐前返回默认值，避免仪表盘空数据。
 */
export const getTodaySleepSummary = async (deviceId) => {
  try {
    return await request('/parrot/behavior/today-sleep-summary', { query: { deviceId } })
  } catch {
    return { sleepDurationMinutes: 0 }
  }
}
