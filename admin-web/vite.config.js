import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8090,
    proxy: {
      // 开发环境把 /admin 开头的请求转发到后端 sky-take-out
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})