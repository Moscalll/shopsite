import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import type { ProxyOptions } from 'vite'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

  const apiProxy: ProxyOptions = {
    target: apiTarget,
    changeOrigin: true,
    configure(proxy) {
      proxy.on('error', (err, req) => {
        const url = req?.url ?? ''
        console.error(
          `\n[Vite proxy] ${url} -> ${apiTarget} failed: ${err.message}`,
          '\n  Hint: start Spring Boot on that host/port, or set VITE_API_PROXY_TARGET in frontend/.env.development',
          '\n  Example: VITE_API_PROXY_TARGET=http://127.0.0.1:8080\n',
        )
      })
    },
  }

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      proxy: {
        '/api': apiProxy,
      },
    },
  }
})
