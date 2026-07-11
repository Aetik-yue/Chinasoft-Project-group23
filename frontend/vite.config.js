import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import http from 'node:http'

// 主备后端自动切换（开机连同学，不开机连本地）：
//   主后端 = .env 的 VITE_BACKEND_HOST（团队共享，填跑后端+有识别模型的那台机器的 IP，默认同学 192.168.20.35）
//   本地兜底 = VITE_FALLBACK_HOST（默认 localhost，本机也开着后端时才有东西可连）
// 机制：用 vite 插件中间件自己转发 /api（不依赖 http-proxy 的 router，那个在 vite 下不可靠）。
//   - 每 10s 健康检查主后端，缓存 primaryAlive 用于快速路由（避免每请求都等超时）。
//   - 每个请求按 primaryAlive 先试主/备，主连接失败 1.5s 内立刻 fallback 到另一个并重试，
//     并即时把主标"离线"加速后续请求。健康检查滞后或主后端间歇抽风都不影响。
// 成长报告等读库接口两边都能用；VLM 识别只有主后端（有模型）能用，本地兜底时 MonitorCard 会优雅降级。
const BACKEND_PORT = 8080
const PROBE_PATH = '/api/environment/report?deviceId=device-001&range=daily'
const PROBE_TIMEOUT_MS = 1500   // 健康检查超时：1.5s 没响应判离线
const CHECK_INTERVAL_MS = 10000  // 健康检查周期
const CONNECT_TIMEOUT_MS = 1500  // 单请求连接超时：1.5s 连不上主就 fallback，避免长时间卡等

let primaryAlive = false
let lastStatus = null
let probeStarted = false
let primaryHost = 'localhost'
let fallbackHost = 'localhost'

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

async function handleApi(req, res) {
  // 先缓冲请求体（POST/文件上传也要能 fallback 重试）
  const body = await new Promise((resolve) => {
    const chunks = []
    req.on('data', (c) => chunks.push(c))
    req.on('end', () => resolve(Buffer.concat(chunks)))
    req.on('error', () => resolve(Buffer.concat(chunks)))
  })

  // 在线先试主后试备；离线先试备后试主（兜底万一主又恢复）
  const order = primaryAlive
    ? [primaryHost, fallbackHost]
    : [fallbackHost, primaryHost]

  for (const host of order) {
    if (!host) continue
    const ok = await forwardTo(req, res, host, body)
    if (ok) {
      if (host !== order[0]) {
        console.log(`[proxy] 主后端连接失败，本次已 fallback 到${host === fallbackHost ? '本地' : '同学'}`)
      }
      return
    }
    // 主连不上 -> 即时标离线，加速后续请求直接走本地（不等 10s 健康检查）
    if (host === primaryHost && primaryAlive) {
      primaryAlive = false
      logStatus(false)
    }
  }
  if (!res.headersSent) {
    res.writeHead(502, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: 502, message: '主备后端均不可达' }))
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  primaryHost = env.VITE_BACKEND_HOST || 'localhost'
  fallbackHost = env.VITE_FALLBACK_HOST || 'localhost'

  // vite 解析配置可能多次调用工厂，用 probeStarted 守卫，setInterval 只起一次。
  if (!probeStarted) {
    probeStarted = true
    probePrimary()
    setInterval(probePrimary, CHECK_INTERVAL_MS)
  }

  return {
    plugins: [
      vue(),
      {
        // 自定义 /api 代理：主备自动切换 + 单请求失败 fallback
        name: 'backend-failover-proxy',
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
