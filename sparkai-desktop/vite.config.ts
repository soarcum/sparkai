import { defineConfig } from 'vite'
import path from 'path'
import vue from '@vitejs/plugin-vue'
import electron from 'vite-plugin-electron'
import renderer from 'vite-plugin-electron-renderer'

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    electron([
      {
        // Electron 主进程入口
        entry: 'electron/main.ts',
        vite: {
          build: {
            rollupOptions: {
              output: {
                format: 'cjs'
              }
            }
          }
        }
      },
      {
        // Electron Preload 脚本入口
        entry: 'electron/preload.ts',
        onclean(options) {
          options.clean()
        },
        vite: {
          build: {
            rollupOptions: {
              output: {
                format: 'cjs'
              }
            }
          }
        }
      },
    ]),
    renderer(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
  }
})
