import { request } from './request'

function normalizeAuthData(data) {
  if (typeof data === 'string') {
    return { token: data, user: null }
  }
  return {
    token: data?.token || data?.accessToken || data?.jwt || '',
    user: data?.user || data?.profile || null,
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

export const deleteAccount = () =>
  request('/auth/account', { method: 'DELETE' })
