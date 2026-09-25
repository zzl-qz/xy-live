import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const backend = env.VITE_BACKEND_ORIGIN || 'http://localhost:9191'
  const prefix = env.VITE_BACKEND_PREFIX ?? ''
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: backend,
          changeOrigin: true,
          rewrite: path => `${prefix}${path.slice(4)}`,
        },
      },
    },
  }
})
