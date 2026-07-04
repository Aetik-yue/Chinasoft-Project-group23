import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      // 后端 API 统一前缀 /api，转发到 Spring Boot (8080)，避免浏览器跨域。
      // 与《智慧烟感API接口文档》BaseURL http://localhost:8080/api 对齐。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
