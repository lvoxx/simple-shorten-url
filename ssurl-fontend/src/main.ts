import '@fontsource-variable/outfit'
import '@fontsource/geist-mono/400.css'
import '@fontsource/geist-mono/500.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
