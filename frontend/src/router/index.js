import { createRouter, createWebHistory } from 'vue-router'

import ChatRoomForm from '@/components/chat/ChatRoomForm.vue'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import ChatRoom from '@/components/chat/ChatRoom.vue'
import LoginForm from '@/components/LoginForm.vue'
import Success from '@/components/Success.vue'
import SignalTest from '@/components/meeting/SignalTest.vue'
import Meeting from '@/components/meeting/Meeting.vue'

const routes = [
  {
    path: '/chat',
    name: 'ChatRoomList',
    component: ChatRoomList
  },
  {
    path: '/room/new',
    name: 'ChatRoomForm',
    component: ChatRoomForm
  },
  {
    path: '/room/:roomNo',
    name: 'ChatRoom',
    component: ChatRoom,
    props: true
  },
  { path: '/', name: 'login', component: LoginForm },
  { path: '/s', name: 's', component: Success },
  { path: '/signal-test', component: SignalTest },
  { path: '/meeting', name: 'Meeting', component: Meeting},
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
