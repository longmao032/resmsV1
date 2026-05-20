import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 5173,
      proxy: {
        [env.VITE_APP_BASE_API]: {
          target: 'http://localhost:8082',
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp('^' + env.VITE_APP_BASE_API), '/api'),
        },
        // 代理后端返回的静态资源 URL（如 /api/profile/...），直接转发到后端
        '/api': {
          target: 'http://localhost:8082',
          changeOrigin: true,
        },
      },
    },
  }
})
