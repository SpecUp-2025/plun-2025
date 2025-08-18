import { createRouter, createWebHistory } from 'vue-router'

import LoginForm from '@/components/LoginForm.vue'
import Success from '@/components/Success.vue'


const routes = [
  { path: '/', name: 'login', component: LoginForm },
  { path: '/s', name: 's', component: Success },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
