import { request } from './request'

function normalizeAuthData(data) {
  if (typeof data === 'string') {
    return { token: data, user: null }
  }
  // 后端登录/获取资料接口把用户信息平铺在 data 中（username / userRole / phone / email 等），
  // 如果 data 自身包含这些字段，则把整个 data 作为 user 返回。
  const userFromFlat = data && (data.username != null || data.userRole != null || data.phone != null || data.email != null)
    ? data
    : null
  return {
    token: data?.token || data?.accessToken || data?.jwt || '',
    user: userFromFlat || data?.user || data?.profile || null,
  }
}

export async function loginBySms({ phone, code }) {
  const data = await request('/auth/sms-login', {
    method: 'POST',
    body: { phone, code },
  })
  return normalizeAuthData(data)
}

export const sendSmsCode = (phone) =>
  request('/auth/sms-code', {
    method: 'POST',
    body: { phone },
  })

export async function loginByPassword({ account, password }) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: { account, password },
  })
  return normalizeAuthData(data)
}

export async function register({ account, password, phone }) {
  const data = await request('/auth/register', {
    method: 'POST',
    body: { account, password, phone },
  })
  return normalizeAuthData(data)
}

export function fetchUserProfile() {
  return request('/auth/me')
}

export const updateUserProfile = (body) =>
  request('/auth/me', { method: 'PUT', body })

export const deleteAccount = () =>
  request('/auth/account', { method: 'DELETE' })

export const changePassword = ({ oldPassword, newPassword }) =>
  request('/auth/change-password', { method: 'POST', body: { oldPassword, newPassword } })
