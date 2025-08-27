import { createRouter, createWebHistory } from 'vue-router'

import ChatRoomForm from '@/components/chat/ChatRoomForm.vue'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import ChatRoom from '@/components/chat/ChatRoom.vue'
import SignalTest from '@/components/meeting/SignalTest.vue'
import Meeting from '@/components/meeting/Meeting.vue'
import MeetingTest from '@/components/meeting/MeetingTest.vue'
import LoginForm from '@/auth/components/LoginForm.vue'
import Register from '@/member/components/Register.vue'
import TeamList from '@/member/components/TeamList.vue'
import TeamMain from '@/main/components/TeamMain.vue'
import TeamCreate from '@/member/components/TeamCreate.vue'


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
  { path: '/teamList', name: 'teamList', component: TeamList,
    children : [
      {
        path: '/teamCreate',
        name: 'teamCreate',
        component: TeamCreate
      }
    ]
  },
  { path: '/signal-test', component: SignalTest },
  { path: '/meeting', name: 'Meeting', component: Meeting},
  { path: '/meeting-test', name: 'MeetingTest', component: MeetingTest},
  { path: '/register', name: 'register', component: Register},
  { path: '/teamMain/:id', name: 'TeamMain', component: TeamMain},
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
