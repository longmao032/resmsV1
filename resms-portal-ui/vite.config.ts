import { fileURLToPath, URL } from 'node:url'
import path from 'node:path'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const srcDir = fileURLToPath(new URL('./src', import.meta.url))

/** Vite plugin: 在 PostCSS 之前将 @reference 中的 @/ 别名解析为相对路径 */
function resolveAliasPlugin() {
  return {
    name: 'vite-resolve-alias',
    enforce: 'pre' as const,
    transform(code: string, id: string) {
      if (!id.includes('vue&type=style')) return null
      const file = id.split('?')[0]
      return code.replace(/@reference\s+["'](@\/[^"']+)["']/g, (_: string, alias: string) => {
        const absolute = path.resolve(srcDir, alias.slice(2))
        const relative = path.relative(path.dirname(file), absolute)
        return `@reference "${relative.split(path.sep).join('/')}"`
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [
      resolveAliasPlugin(),
      vue(),
      vueDevTools(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    resolve: {
      alias: {
        '@': srcDir,
      },
    },
    server: {
      port: 5174,
      proxy: {
        [env.VITE_APP_BASE_API]: {
          target: 'http://localhost:8082',
          changeOrigin: true,
          rewrite: (path: string) => path.replace(new RegExp('^' + env.VITE_APP_BASE_API), ''),
        },
        '/api': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          ws: true,
        },
      },
    },
  }
})
