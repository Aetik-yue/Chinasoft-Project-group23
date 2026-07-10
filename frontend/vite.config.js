import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址由 VITE_BACKEND_HOST 控制（默认 localhost）：
//   - 本机开发：不设即可
//   - 连同学的后端：在 frontend/.env.local 写 VITE_BACKEND_HOST=同学IP
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const backendHost = env.VITE_BACKEND_HOST || 'localhost'
  return {
    plugins: [vue()],
    server: {
      host: '127.0.0.1',
      port: 5173,
      proxy: {
        // 后端 API 统一前缀 /api，转发到 Spring Boot 8080，避免浏览器跨域
        '/api': {
          target: `http://${backendHost}:8080`,
          changeOrigin: true,
        },
      },
    },
  }
})