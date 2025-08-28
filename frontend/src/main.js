import { createApp } from 'vue'
import App from './App.vue'

import router from './router'
import { createPinia } from 'pinia'
import piniaPersistedstate from 'pinia-plugin-persistedstate'
// import '@fullcalendar/common/main.css'
// import '@fullcalendar/daygrid/main.css'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPersistedstate)

app.use(pinia)
app.use(router)
app.mount('#app')