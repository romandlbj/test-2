import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  cacheDir: 'C:/Users/roman/.codex/visualizations/2026/09/08/01a080b9-ea50-74b2-a0b0-7d0230d3b3fb/vite-cache',
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8082'
    }
  }
})
