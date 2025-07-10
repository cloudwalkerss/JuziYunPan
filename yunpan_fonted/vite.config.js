import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers';
import vueDevTools from 'vite-plugin-vue-devtools'
// 引入Element Plus自动导入插件
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // vueDevTools(),
    // 自动导入Element Plus组件
    AutoImport({
      resolvers: [ElementPlusResolver()
      , AntDesignVueResolver({
          importStyle: false, // css in js
        })]
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  define: {
    'import.meta.env.VITE_API_URL': JSON.stringify('http://localhost:8080')
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/file': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/showShare': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
       '/user':{
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
