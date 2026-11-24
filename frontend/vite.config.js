import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // Default to 8081 (user-service standalone) or 8080 (API Gateway when full stack runs)
  const target = env.VITE_BACKEND_URL || 'http://127.0.0.1:8081'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: target,
          changeOrigin: true,
          secure: false
        },
        '/gateway': {
          target: target,
          changeOrigin: true,
          secure: false
        }
      }
    }
  }
})
