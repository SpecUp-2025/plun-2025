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
import FindPassword from '@/member/components/FindPassword.vue'
import CalendarView from '@/components/calendar/CalendarView.vue'
import MemberDetail from '@/member/components/MemberDetail.vue'
import TeamSetting from '@/member/components/TeamSetting.vue'


const routes = [
  { path: '/chat', name: 'ChatRoomList', component: ChatRoomList },
  { path: '/room/new', name: 'ChatRoomForm', component: ChatRoomForm },
  { path: '/room/:roomNo', name: 'ChatRoom', component: ChatRoom },
  { path: '/', name: 'login', component: LoginForm },
  { path: '/teamList', name: 'teamList', component: TeamList,
    children : [{ path: '/teamCreate', name: 'teamCreate', component: TeamCreate }]
  },
  { path: '/register', name: 'register', component: Register},
  { path: '/teamMain/:teamNo', name: 'teamMain', component: TeamMain},
  { path: '/meeting-room/:roomCode', name: 'MeetingRoom', component: MeetingRoom},
  {path: '/findPassword', name: 'find', component : FindPassword},
  { path: '/calendar', name: 'Calendar', component: CalendarView },
  {path : '/memberDetail', name: 'detail', component:MemberDetail},
  {path : '/setting/:teamNo', name: 'setting', component:TeamSetting},
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
