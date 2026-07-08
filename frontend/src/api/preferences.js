import { request } from './request'

export const getUserPreferences = () =>
  request('/user/preferences')

export const updateUserPreferences = (body) =>
  request('/user/preferences', { method: 'PUT', body })
