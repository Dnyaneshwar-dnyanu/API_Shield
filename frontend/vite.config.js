import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // Default microservice targets for standalone mode
  const gatewayTarget = env.VITE_GATEWAY_URL || 'http://127.0.0.1:8080'
  const userServiceTarget = env.VITE_USER_SERVICE_URL || 'http://127.0.0.1:8081'
  const registryServiceTarget = env.VITE_REGISTRY_SERVICE_URL || 'http://127.0.0.1:8082'
  const subscriptionServiceTarget = env.VITE_SUBSCRIPTION_SERVICE_URL || 'http://127.0.0.1:8083'
  const apiKeyServiceTarget = env.VITE_APIKEY_SERVICE_URL || 'http://127.0.0.1:8084'
  const paymentServiceTarget = env.VITE_PAYMENT_SERVICE_URL || 'http://127.0.0.1:8085'
  const notificationServiceTarget = env.VITE_NOTIFICATION_SERVICE_URL || 'http://127.0.0.1:8086'
  const analyticsServiceTarget = env.VITE_ANALYTICS_SERVICE_URL || 'http://127.0.0.1:8087'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api/v1/apis': {
          target: registryServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api/v1/catalog': {
          target: registryServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api/v1/subscriptions': {
          target: subscriptionServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api/v1/keys': {
          target: apiKeyServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api/v1/payments': {
          target: paymentServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api/v1/notifications': {
          target: notificationServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api/v1/analytics': {
          target: analyticsServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/api': {
          target: userServiceTarget,
          changeOrigin: true,
          secure: false
        },
        '/gateway': {
          target: gatewayTarget,
          changeOrigin: true,
          secure: false
        }
      }
    }
  }
})
