import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import fs from 'fs'
import path from 'path'

const enableHttps = process.env.VITE_LOCAL_HTTPS === '1'

// 인증서가 없으면 자동으로 HTTP로 떨어지게(팀원 보호)
const keyPath  = path.resolve(__dirname, 'certs/localhost+1-key.pem')
const certPath = path.resolve(__dirname, 'certs/localhost+1.pem')
const hasCerts = fs.existsSync(keyPath) && fs.existsSync(certPath)

export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    ...(enableHttps && hasCerts ? {
      https: {
        key:  fs.readFileSync(keyPath),
        cert: fs.readFileSync(certPath),
      },
      host: '127.0.0.1',   // 로컬 루프백 고정(개발자마다 다르게 안 잡히게)
    } : {}),
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: p => p.replace(/^\/api/, '')
      },
      '/ws-chat': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  define: { global: 'globalThis' }
})
