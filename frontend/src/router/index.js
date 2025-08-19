import { createRouter, createWebHistory } from 'vue-router'
import ChatRoomForm from '@/components/chat/ChatRoomForm.vue'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import ChatRoom from '@/components/chat/ChatRoom.vue'

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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
