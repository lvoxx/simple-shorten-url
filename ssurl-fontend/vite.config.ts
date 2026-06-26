import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// Backend api_service. Override with VITE_API_TARGET for non-default setups.
const API_TARGET = process.env.VITE_API_TARGET ?? 'http://localhost:8080'

// Dashboard service (analytics REST + WebSocket). Override with VITE_DASHBOARD_TARGET.
const DASHBOARD_TARGET = process.env.VITE_DASHBOARD_TARGET ?? 'http://localhost:8083'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // Same-origin proxy avoids backend CORS and lets the HTTP-only refresh
    // cookie (path /api/v1/auth) flow with credentials: 'include'.
    // NOTE: the dashboard rules MUST precede the generic `/api` rule so that
    // analytics traffic is routed to the dashboard service, not api_service.
    proxy: {
      '/api/v1/dashboard': {
        target: DASHBOARD_TARGET,
        changeOrigin: true,
      },
      '/ws/dashboard': {
        target: DASHBOARD_TARGET,
        changeOrigin: true,
        ws: true,
      },
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },
})
