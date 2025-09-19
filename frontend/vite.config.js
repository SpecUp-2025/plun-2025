// vite.config.js
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import fs from 'fs'
import path from 'path'

const enableHttps = process.env.VITE_LOCAL_HTTPS === '1'

const keyPath = path.resolve(__dirname, 'certs/localhost+1-key.pem')
const certPath = path.resolve(__dirname, 'certs/localhost+1.pem')
const hasCerts = fs.existsSync(keyPath) && fs.existsSync(certPath)

export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: '127.0.0.1',//'0.0.0.0', 
    port: 5173,//3000, 
    // https: true, //임시 허용
    ...(enableHttps && hasCerts ? {
      https: {
        key: fs.readFileSync(keyPath),
        cert: fs.readFileSync(certPath),
      },
    } : {}),
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
      '/ws-chat': { target: 'http://127.0.0.1:8080', changeOrigin: true, ws: true },
      '/sfu': { target: 'http://127.0.0.1:4000', changeOrigin: true, ws: true },
      '/stt': { target: 'http://127.0.0.1:8000', changeOrigin: true },
      // '/api': { target: 'http://172.30.1.15:8080', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
      // '/ws-chat': { target: 'http://172.30.1.15:8080', changeOrigin: true, ws: true },
      // '/sfu': { target: 'http://172.30.1.15:4000', changeOrigin: true, ws: true },
      // '/stt': { target: 'http://172.30.1.15:8000', changeOrigin: true },
    }
  },
  define: { global: 'globalThis' }
})
