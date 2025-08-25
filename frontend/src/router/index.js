import { createRouter, createWebHistory } from 'vue-router'

import ChatRoomForm from '@/components/chat/ChatRoomForm.vue'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import ChatRoom from '@/components/chat/ChatRoom.vue'
import SignalTest from '@/components/meeting/SignalTest.vue'
import MeetingMain from '@/components/meeting/MeetingMain.vue'
import LoginForm from '@/auth/components/LoginForm.vue'
import Success from '@/auth/components/Success.vue'
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
  { path: '/s', name: 's', component: Success },
  { path: '/signal-test', component: SignalTest },
  { path: '/meeting-room/:roomCode', name: 'MeetingRoom', component: MeetingRoom},
  { path: '/meeting-main', name: 'MeetingMain', component: MeetingMain,
    children: [{ path: ':roomCode/prejoin', name: 'MeetingPrejoin', component: MeetingPrejoinModal }],
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
