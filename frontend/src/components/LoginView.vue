<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { loginByPassword, loginBySms, register, sendSmsCode } from '../api/auth'

const emit = defineEmits(['login-success'])

// flow: 'login' | 'register'
const flow = ref('login')
const mode = ref('password')
const phone = ref('')
const code = ref('')
const account = ref('')
const password = ref('')
const registerPhone = ref('')
const registerPassword = ref('')
const registerPasswordConfirm = ref('')
const errorMessage = ref('')
const countdown = ref(0)
const isSubmitting = ref(false)
const isCodeSending = ref(false)

const isRegisterFlow = computed(() => flow.value === 'register')
const isLoginFlow = computed(() => flow.value === 'login')

const headerSubtitle = computed(() => {
  if (isRegisterFlow.value) return '注册新账号，开启鹦鹉养护之旅'
  return isSmsMode.value ? '手机号验证码登录' : '账号密码登录'
})

let timer = 0

const isSmsMode = computed(() => mode.value === 'sms')

function sanitizePhone(value) {
  return String(value || '').replace(/\D/g, '').slice(0, 11)
}

function updatePhone(value) {
  phone.value = sanitizePhone(value)
  errorMessage.value = ''
}

async function startCodeCountdown() {
  errorMessage.value = ''

  if (!/^\d{11}$/.test(phone.value)) {
    errorMessage.value = '请输入正确的手机号'
    return
  }

  if (countdown.value > 0 || isCodeSending.value) return

  isCodeSending.value = true
  try {
    await sendSmsCode(phone.value)
  } catch (error) {
    errorMessage.value = error?.message || '验证码发送失败，请稍后重试'
    isCodeSending.value = false
    return
  }

  isCodeSending.value = false
  countdown.value = 60
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = 0
    }
  }, 1000)
}

async function handleLogin() {
  errorMessage.value = ''

  if (isSmsMode.value) {
    if (!/^\d{11}$/.test(phone.value)) {
      errorMessage.value = '请输入正确的手机号'
      return
    }

    if (!code.value.trim()) {
      errorMessage.value = '请输入验证码'
      return
    }
  } else {
    if (!account.value.trim()) {
      errorMessage.value = '请输入账号或手机号'
      return
    }

    if (!password.value.trim()) {
      errorMessage.value = '请输入密码'
      return
    }
  }

  isSubmitting.value = true
  try {
    const session = isSmsMode.value
      ? await loginBySms({ phone: phone.value, code: code.value.trim() })
      : await loginByPassword({ account: account.value.trim(), password: password.value })

    if (!session.token) {
      throw new Error('登录接口未返回 token')
    }

    localStorage.setItem('parrotAuthToken', session.token)
    if (session.user) {
      localStorage.setItem('parrotAuthUser', JSON.stringify(session.user))
    }
    emit('login-success', session)
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}

function toggleMode() {
  errorMessage.value = ''
  mode.value = isSmsMode.value ? 'password' : 'sms'
}

function switchFlow(target) {
  errorMessage.value = ''
  flow.value = target
}

async function handleRegister() {
  errorMessage.value = ''

  if (!account.value.trim()) {
    errorMessage.value = '请输入账号'
    return
  }

  if (!registerPassword.value) {
    errorMessage.value = '请输入密码'
    return
  }

  if (registerPassword.value.length < 6) {
    errorMessage.value = '密码长度至少 6 位'
    return
  }

  if (registerPassword.value !== registerPasswordConfirm.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  isSubmitting.value = true
  try {
    const session = await register({
      account: account.value.trim(),
      password: registerPassword.value,
      phone: registerPhone.value.trim() || undefined,
    })

    if (!session.token) {
      throw new Error('注册接口未返回 token')
    }

    localStorage.setItem('parrotAuthToken', session.token)
    if (session.user) {
      localStorage.setItem('parrotAuthUser', JSON.stringify(session.user))
    }
    emit('login-success', session)
  } catch (error) {
    errorMessage.value = error?.message || '注册失败，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}

onBeforeUnmount(() => {
  window.clearInterval(timer)
})
</script>

<template>
  <main class="login-shell">
    <section class="login-card" aria-label="登录">
      <div class="brand-mark" aria-hidden="true">
        <span class="wing wing-left"></span>
        <span class="wing wing-right"></span>
        <span class="eye"></span>
      </div>

      <header class="login-header">
        <p class="login-kicker">Parrot Care</p>
        <h1>鹦鹉安全</h1>
        <p>{{ headerSubtitle }}</p>
      </header>

      <nav class="login-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          :aria-selected="isLoginFlow"
          class="login-tab"
          :class="{ active: isLoginFlow }"
          @click="switchFlow('login')"
        >
          登录
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="isRegisterFlow"
          class="login-tab"
          :class="{ active: isRegisterFlow }"
          @click="switchFlow('register')"
        >
          注册
        </button>
      </nav>

      <form v-if="isLoginFlow" class="login-form" @submit.prevent="handleLogin">
        <template v-if="isSmsMode">
          <label class="login-field">
            <span>手机号</span>
            <input
              :value="phone"
              inputmode="numeric"
              maxlength="11"
              autocomplete="tel"
              placeholder="请输入手机号"
              @input="updatePhone($event.target.value)"
            />
          </label>

          <label class="login-field">
            <span>验证码</span>
            <div class="code-row">
              <input v-model="code" inputmode="numeric" autocomplete="one-time-code" placeholder="验证码" />
              <button
                type="button"
                class="code-button"
                :disabled="countdown > 0 || isCodeSending"
                @click="startCodeCountdown"
              >
                {{ isCodeSending ? '发送中...' : countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </label>
        </template>

        <template v-else>
          <label class="login-field">
            <span>账号</span>
            <input v-model="account" autocomplete="username" placeholder="请输入账号或手机号" />
          </label>

          <label class="login-field">
            <span>密码</span>
            <input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" />
          </label>
        </template>

        <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

        <button type="submit" class="login-submit" :disabled="isSubmitting">
          {{ isSubmitting ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else class="login-form" @submit.prevent="handleRegister">
        <label class="login-field">
          <span>账号</span>
          <input v-model="account" autocomplete="username" placeholder="请输入账号（3-64 位）" />
        </label>

        <label class="login-field">
          <span>密码</span>
          <input v-model="registerPassword" type="password" autocomplete="new-password" placeholder="请输入密码（至少 6 位）" />
        </label>

        <label class="login-field">
          <span>确认密码</span>
          <input v-model="registerPasswordConfirm" type="password" autocomplete="new-password" placeholder="请再次输入密码" />
        </label>

        <label class="login-field">
          <span>手机号（选填）</span>
          <input
            :value="registerPhone"
            inputmode="numeric"
            maxlength="11"
            autocomplete="tel"
            placeholder="可用于短信登录"
            @input="registerPhone = sanitizePhone($event.target.value)"
          />
        </label>

        <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

        <button type="submit" class="login-submit" :disabled="isSubmitting">
          {{ isSubmitting ? '注册中...' : '注册' }}
        </button>
      </form>

      <button v-if="isLoginFlow" type="button" class="mode-switch" @click="toggleMode">
        {{ isSmsMode ? '账号密码登录' : '验证码登录' }}
      </button>

      <button v-else type="button" class="mode-switch" @click="switchFlow('login')">
        已有账号？返回登录
      </button>
    </section>
  </main>
</template>

<style scoped>
.login-shell {
  position: fixed;
  inset: 0;
  z-index: 10000;
  min-width: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  color: #352525;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 255, 255, .92), transparent 28%),
    radial-gradient(circle at 82% 16%, rgba(236, 225, 255, .72), transparent 30%),
    linear-gradient(135deg, #fbf6ed 0%, #fffaf2 55%, #f4e3d2 100%);
}

.login-card {
  width: min(520px, calc(100vw - 48px));
  min-height: 560px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 42px;
  border: 1px solid rgba(255, 255, 255, .82);
  border-radius: 34px;
  background: rgba(255, 252, 246, .72);
  box-shadow:
    0 28px 70px rgba(91, 61, 41, .18),
    inset 0 1px 0 rgba(255, 255, 255, .92);
  backdrop-filter: blur(22px);
}

.brand-mark {
  position: relative;
  width: 78px;
  height: 78px;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: #fff4cf;
  box-shadow: 0 16px 28px rgba(111, 75, 43, .14);
  overflow: hidden;
}

.brand-mark::before {
  content: "";
  position: absolute;
  left: 26px;
  top: 18px;
  width: 54px;
  height: 28px;
  border-radius: 50% 62% 52% 48%;
  background: #fffdf7;
  transform: rotate(-8deg);
}

.brand-mark::after {
  content: "";
  position: absolute;
  right: 10px;
  top: 35px;
  border-left: 20px solid #ff7a24;
  border-top: 10px solid transparent;
  border-bottom: 10px solid transparent;
}

.wing {
  position: absolute;
  bottom: 12px;
  width: 42px;
  height: 26px;
  border-radius: 70% 20% 70% 30%;
  background: #f97322;
}

.wing-left {
  left: 14px;
  transform: rotate(-34deg);
}

.wing-right {
  right: 14px;
  transform: rotate(34deg);
}

.eye {
  position: absolute;
  right: 28px;
  top: 28px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #1f1a18;
  box-shadow: 3px -3px 0 -1px #fff;
}

.login-header {
  text-align: center;
  margin-bottom: 22px;
}

.login-kicker {
  margin: 0 0 6px;
  color: rgba(91, 61, 41, .58);
  font-size: 15px;
  font-weight: 800;
}

.login-header h1 {
  margin: 0;
  color: #2f2626;
  font-size: 48px;
  line-height: 1.05;
  letter-spacing: 0;
}

.login-header p:last-child {
  margin: 12px 0 0;
  color: #7c6046;
  font-size: 18px;
  font-weight: 800;
}

.login-form {
  display: grid;
  gap: 14px;
}

.login-field {
  display: grid;
  gap: 8px;
  color: #704b27;
  font-size: 18px;
  font-weight: 900;
}

.login-field input {
  width: 100%;
  min-width: 0;
  height: 52px;
  border: 1px solid rgba(119, 86, 53, .14);
  border-radius: 20px;
  padding: 0 20px;
  color: #2f2626;
  background: rgba(255, 252, 246, .84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .92);
  font-size: 20px;
  font-weight: 800;
  outline: none;
  transition:
    border-color .18s ease,
    box-shadow .18s ease,
    transform .18s ease;
}

.login-field input:focus {
  border-color: rgba(88, 65, 120, .38);
  box-shadow:
    0 0 0 4px rgba(119, 91, 163, .12),
    inset 0 1px 0 rgba(255, 255, 255, .95);
  transform: translateY(-1px);
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 138px;
  gap: 12px;
}

.code-button,
.login-submit,
.mode-switch {
  border: 0;
  appearance: none;
  cursor: pointer;
  transition:
    transform .18s ease,
    box-shadow .18s ease,
    filter .18s ease,
    opacity .18s ease;
}

.code-button {
  height: 52px;
  border-radius: 20px;
  color: #fff;
  background: #6c5ac4;
  box-shadow: 0 14px 26px rgba(93, 78, 170, .22);
  font-size: 17px;
  font-weight: 900;
}

.code-button:disabled,
.login-submit:disabled {
  cursor: default;
  opacity: .62;
  box-shadow: none;
}

.code-button:not(:disabled):hover,
.login-submit:not(:disabled):hover,
.mode-switch:hover {
  transform: translateY(-2px);
  filter: brightness(1.02);
}

.login-error {
  min-height: 24px;
  margin: -4px 0 0;
  color: #c44332;
  font-size: 15px;
  font-weight: 800;
}

.login-submit {
  width: 100%;
  height: 58px;
  margin-top: 6px;
  border-radius: 24px;
  color: #fff;
  background: #2f2938;
  box-shadow: 0 18px 34px rgba(47, 41, 56, .22);
  font-size: 22px;
  font-weight: 900;
}

.mode-switch {
  align-self: center;
  margin-top: 22px;
  padding: 14px 22px;
  border: 1px solid rgba(111, 75, 43, .15);
  border-radius: 999px;
  color: #704b27;
  background: rgba(255, 252, 246, .58);
  font-size: 17px;
  font-weight: 900;
}

.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(143, 91, 42, .1);
}

.login-tab {
  flex: 1;
  height: 44px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  color: #704b27;
  font-size: 16px;
  font-weight: 900;
  background: transparent;
  transition:
    color .18s ease,
    background .18s ease,
    box-shadow .18s ease;
}

.login-tab.active {
  color: #fff;
  background: #2f2938;
  box-shadow: 0 10px 22px rgba(47, 41, 56, .2);
  cursor: default;
}

.login-tab:not(.active):hover {
  color: #2f2938;
  background: rgba(143, 91, 42, .12);
}

@media (max-width: 640px) {
  .login-shell {
    padding: 18px;
  }

  .login-card {
    width: 100%;
    min-height: auto;
    padding: 30px 22px;
    border-radius: 28px;
  }

  .login-header h1 {
    font-size: 40px;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
