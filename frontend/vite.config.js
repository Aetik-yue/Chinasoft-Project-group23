import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import http from 'node:http'

// 主备后端自动切换 + 手动切换（开机连同学，不开机连本地；调试自己改动时可强制走本地）：
//   主后端 = .env 的 VITE_BACKEND_HOST（团队共享，默认同学 192.168.20.35，装了识别模型）
//   本地兜底 = VITE_FALLBACK_HOST（默认 localhost，本机后端）
// 机制：vite 插件中间件自己转发 /api（不依赖 http-proxy 的 router，那个在 vite 下不可靠）。
//   - 每 10s 健康检查主后端缓存 primaryAlive，用于快速路由；
//   - 单请求主连不上 1.5s 内 fallback 另一个并重试，并即时标离线加速后续；
//   - 手动切换：dev 终端输入字母 l=本地 / t=同学 / a=自动（避开 vite 自带快捷键 r/o/c/q）。
//     本地=强制走本机后端（调自己的改动）；同学=强制走远程不 fallback；自动=主备+fallback。
// 成长报告等读库接口两边都能用；VLM 识别只有主后端（有模型）能用，本地兜底时 MonitorCard 会优雅降级。
const BACKEND_PORT = 8080
const PROBE_PATH = '/api/environment/report?deviceId=device-001&range=daily'
const PROBE_TIMEOUT_MS = 1500   // 健康检查超时：1.5s 没响应判离线
const CHECK_INTERVAL_MS = 10000  // 健康检查周期
const CONNECT_TIMEOUT_MS = 1500  // 单请求连接超时：1.5s 连不上主就 fallback，避免长时间卡等

let primaryAlive = false
let lastStatus = null
let probeStarted = false
let stdinStarted = false
let primaryHost = 'localhost'
let fallbackHost = 'localhost'
let manualOverride = 'auto'  // 'auto' | 'remote' | 'local'，终端字母切换设置

// 探测主后端：能连上并返回任意 HTTP 响应就算在线；ECONNREFUSED/ETIMEDOUT 算离线。
function probePrimary() {
  if (!primaryHost) { primaryAlive = false; logStatus(false); return }
  const req = http.get(
    { host: primaryHost, port: BACKEND_PORT, path: PROBE_PATH, timeout: PROBE_TIMEOUT_MS },
    (res) => { res.resume(); primaryAlive = true; logStatus(true) },
  )
  req.on('timeout', () => req.destroy()) // 超时 destroy 触发 error
  req.on('error', () => { primaryAlive = false; logStatus(false) })
}

function logStatus(alive) {
  if (alive === lastStatus) return // 状态没变不刷屏
  lastStatus = alive
  console.log(
    `[proxy] 主后端 ${primaryHost}:${BACKEND_PORT} ${alive ? '在线 ✓ -> 走远程' : '离线 ✗ -> 走本地'}`,
  )
}

// 转发到指定 host：拿到响应头就 pipe 回去并 resolve(true)；连接失败/超时 resolve(false)（让外层 fallback）。
function forwardTo(req, res, host, body) {
  return new Promise((resolve) => {
    let settled = false
    const proxyReq = http.request(
      {
        host,
        port: BACKEND_PORT,
        method: req.method,
        path: req.url,
        headers: { ...req.headers, host: `${host}:${BACKEND_PORT}` },
      },
      (proxyRes) => {
        settled = true
        res.writeHead(proxyRes.statusCode, proxyRes.headers)
        proxyRes.pipe(res)
        resolve(true)
      },
    )
    // 连接阶段超时：1.5s 连不上就放弃，连上后取消超时（避免误杀慢响应）
    proxyReq.on('socket', (socket) => {
      socket.setTimeout(CONNECT_TIMEOUT_MS)
      socket.on('connect', () => socket.setTimeout(0))
      socket.on('timeout', () => { if (!settled) proxyReq.destroy() })
    })
    proxyReq.on('error', () => { if (!settled) resolve(false) })
    if (body && body.length) proxyReq.end(body)
    else proxyReq.end()
  })
}

// 决定本次请求试哪些 host、什么顺序：手动模式强制单一目标，自动模式按健康检查主备 + fallback。
function decideOrder() {
  if (manualOverride === 'local') return [fallbackHost]
  if (manualOverride === 'remote') return [primaryHost]
  return primaryAlive ? [primaryHost, fallbackHost] : [fallbackHost, primaryHost]
}

async function handleApi(req, res) {
  // 先缓冲请求体（POST/文件上传也要能 fallback 重试）
  const body = await new Promise((resolve) => {
    const chunks = []
    req.on('data', (c) => chunks.push(c))
    req.on('end', () => resolve(Buffer.concat(chunks)))
    req.on('error', () => resolve(Buffer.concat(chunks)))
  })

  const order = decideOrder()
  for (const host of order) {
    if (!host) continue
    const ok = await forwardTo(req, res, host, body)
    if (ok) {
      if (host !== order[0] && manualOverride === 'auto') {
        console.log(`[proxy] 主后端连接失败，本次已 fallback 到${host === fallbackHost ? '本地' : '同学'}`)
      }
      return
    }
    // 只有自动模式下主连不上才即时标离线（手动模式不干预健康检查状态）
    if (host === primaryHost && primaryAlive && manualOverride === 'auto') {
      primaryAlive = false
      logStatus(false)
    }
  }
  if (!res.headersSent) {
    res.writeHead(502, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: 502, message: '后端不可达（当前模式：' + manualOverride + '）' }))
  }
}

function setOverride(t) {
  manualOverride = t
  const label = {
    auto: '自动（主备 + fallback）',
    remote: '同学（强制远程，不 fallback）',
    local: '本地（强制本机后端）',
  }[t]
  console.log(`[proxy] 切换 -> ${label}`)
}

// dev 终端输入字母切换后端：l=本地 / t=同学(tongxue) / a=自动
// 避开 vite 自带快捷键 r(restart)/o(open)/c(clear)/q(quit)，用 l/t/a。
// vite 自己也监听 stdin 的 'data'，这里再加一个 listener 与之共存，互不干扰。
function setupStdinSwitch() {
  if (stdinStarted) return
  stdinStarted = true
  if (!process.stdin || !process.stdin.isTTY) {
    console.log('[proxy] 当前终端非交互式，字母切换不可用')
    return
  }
  console.log('[proxy] 输入字母切换后端：l=本地  t=同学  a=自动  （当前: 自动）')
  process.stdin.on('data', (chunk) => {
    const k = chunk.toString().trim().toLowerCase()
    if (k === 'l') setOverride('local')
    else if (k === 't') setOverride('remote')
    else if (k === 'a') setOverride('auto')
  })
}

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  primaryHost = env.VITE_BACKEND_HOST || 'localhost'
  fallbackHost = env.VITE_FALLBACK_HOST || 'localhost'

  // 健康检查和终端切换只服务开发代理；build 阶段若保留定时器会导致构建进程无法退出。
  // vite 解析开发配置可能多次调用工厂，用 probeStarted 守卫，setInterval / stdin 只起一次。
  if (command === 'serve' && !probeStarted) {
    probeStarted = true
    probePrimary()
    setInterval(probePrimary, CHECK_INTERVAL_MS)
    setupStdinSwitch()
  }

  return {
    plugins: [
      vue(),
      {
        name: 'backend-failover-proxy',
        apply: 'serve', // 只在 dev serve 生效，build 时不动
        configureServer(server) {
          server.middlewares.use((req, res, next) => {
            if (req.url && req.url.startsWith('/api')) {
              handleApi(req, res).catch((e) => {
                console.error('[proxy] 转发异常：', e?.message)
                if (!res.headersSent) {
                  res.writeHead(502, { 'Content-Type': 'application/json' })
                  res.end(JSON.stringify({ code: 502, message: '代理转发异常' }))
                }
              })
            } else {
              next()
            }
          })
        },
      },
    ],
    server: {
      host: '127.0.0.1',
      port: 5173,
    },
  }
})
