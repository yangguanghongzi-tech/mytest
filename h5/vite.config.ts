import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'
  return {
    plugins: [vue()],
    server: {
      port: 5174,
      proxy: {
        '/h5/api': { target, changeOrigin: true },
        '/local-storage': { target, changeOrigin: true },
      },
    },
  }
})
