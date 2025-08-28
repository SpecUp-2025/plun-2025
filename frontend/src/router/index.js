import { createRouter, createWebHistory } from 'vue-router'

import ChatRoomForm from '@/components/chat/ChatRoomForm.vue'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import ChatRoom from '@/components/chat/ChatRoom.vue'
import LoginForm from '@/auth/components/LoginForm.vue'
import Register from '@/member/components/Register.vue'
import TeamList from '@/member/components/TeamList.vue'
import TeamMain from '@/main/components/TeamMain.vue'
import TeamCreate from '@/member/components/TeamCreate.vue'
import MeetingRoom from '@/components/meeting/MeetingRoom.vue'


const routes = [
  { path: '/chat', name: 'ChatRoomList', component: ChatRoomList },
  { path: '/room/new', name: 'ChatRoomForm', component: ChatRoomForm },
  { path: '/room/:roomNo', name: 'ChatRoom', component: ChatRoom },
  { path: '/', name: 'login', component: LoginForm },
  { path: '/teamList', name: 'teamList', component: TeamList,
    children : [{ path: '/teamCreate', name: 'teamCreate', component: TeamCreate }]
  },
  { path: '/register', name: 'register', component: Register},
  { path: '/teamMain/:teamNo', name: 'TeamMain', component: TeamMain},
  { path: '/meeting-room/:roomCode', name: 'MeetingRoom', component: MeetingRoom},
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
