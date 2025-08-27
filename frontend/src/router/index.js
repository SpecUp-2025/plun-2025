import { createRouter, createWebHistory } from 'vue-router'

import ChatRoomForm from '@/components/chat/ChatRoomForm.vue'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import ChatRoom from '@/components/chat/ChatRoom.vue'
import MeetingMain from '@/components/meeting/MeetingMain.vue'
import LoginForm from '@/auth/components/LoginForm.vue'
import Register from '@/member/components/Register.vue'
import TeamList from '@/member/components/TeamList.vue'
import TeamMain from '@/main/components/TeamMain.vue'
import MeetingPrejoinModal from '@/components/meeting/MeetingPrejoinModal.vue'
import MeetingRoom from '@/components/meeting/MeetingRoom.vue'


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
  { path: '/TeamList', name: 'TeamList', component: TeamList },
  { path: '/signal-test', component: SignalTest },
  { path: '/meeting', name: 'Meeting', component: Meeting},
  { path: '/meeting-test', name: 'MeetingTest', component: MeetingTest},
  { path: '/register', name: 'register', component: Register},
  { path: '/TeamMain/:id', name: 'TeamMain', component: TeamMain},
  { path: '/meeting-room/:roomCode', name: 'MeetingRoom', component: MeetingRoom},
  { path: '/meeting-main', name: 'MeetingMain', component: MeetingMain,
    children: [{ path: ':roomCode/prejoin', name: 'MeetingPrejoin', component: MeetingPrejoinModal }],
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
