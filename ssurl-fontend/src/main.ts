import '@fontsource-variable/outfit'
import '@fontsource/geist-mono/400.css'
import '@fontsource/geist-mono/500.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { configureHttp } from './lib/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)

app.use(createPinia())

// Wire the HTTP client to the auth store (token, refresh, unauthorized).
const auth = useAuthStore()
configureHttp({
  getToken: () => auth.accessToken,
  refresh: () => auth.attemptRefresh(),
  onUnauthorized: () => {
    auth.clearSession()
    const current = router.currentRoute.value
    if (current.meta.requiresAuth) {
      void router.push({ name: 'login', query: { redirect: current.fullPath } })
    }
  },
})

app.use(router)

app.mount('#app')
