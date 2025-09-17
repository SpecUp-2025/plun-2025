<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import MeetingNav from '@/components/meeting/MeetingNav.vue'
import CalendarView from '@/components/calendar/CalendarView.vue'
import ChatNav from '@/components/chat/ChatNav.vue'
import ChatMain from '@/components/chat/ChatMain.vue'
import SideNav from '@/components/layout/SideNav.vue'


const route = useRoute()
const router = useRouter()
const teamNo = computed(() => Number(route.params.teamNo)) // :id 사용

const selectedRoomNo = ref(null)

const alarms = ref([]);

const activeTab = ref('calendar')

const setTab = (tab) => {
  activeTab.value = tab
  selectedRoomNo.value = null
}

const toggleChatRoomForm = () => {
  if (activeTab.value !== 'createChat') {
    activeTab.value = 'createChat'
    selectedRoomNo.value = null
  } else {
    activeTab.value = 'calendar'
  }
}

const onRoomCreated = (roomNo) => {
  selectedRoomNo.value = Number(roomNo)
  activeTab.value = 'chatRoom'
}

const openChatRoom = (roomNo) => {
  selectedRoomNo.value = Number(roomNo)
  activeTab.value = 'chatRoom'
}

const closeChatRoom = () => {
  selectedRoomNo.value = null
}

const onAlarmRead = (alarmNo) => {
  console.log('teamMain에서 알림 읽음 완료 처리:', alarmNo)
  alarms.value = alarms.value.map(alarm =>
    alarm.alarmNo === alarmNo ? { ...alarm, isRead: 'Y' } : alarm
  );
};

const goToProfile = () => {
  router.push({ name: 'detail' })
  console.log('프로필 페이지로 이동 (구현)');
};

const goToNotifications = () => {
  // 예를 들어 팀 메인에서 알림 탭으로 이동하거나 알림 페이지가 있으면 이동
  // 여기선 알림 탭으로 강제 이동하는 예시
  activeTab.value = 'notifications'  // TeamMain.vue 에 알림 탭이 따로 구현되어 있다면 사용
  // 만약 알림 페이지가 따로 있다면 router.push({ name: 'alarmPage' }) 등으로 변경
  console.log('알림 페이지 이동 (미구현 - 필요시 수정)')
}

const logout = () => {
  // 로그아웃 처리(토큰 삭제 등)
  console.log('로그아웃 처리 (미구현)')
  // 로그아웃 후 로그인 페이지로 이동
  router.push({ name: 'login' })
}
</script>

<template>
  <DefaultLayout
    @go-profile="goToProfile"
    @alarmClicked="openChatRoom"
    @logout="logout"
    >
    <template #header><strong>Team Workspace</strong></template>
      <template #sidebar>
        <SideNav>
          <router-link :to="{ name: 'teamList' }" class="nav-button">←← 팀 리스트
          </router-link>
          <ChatNav :team-no="teamNo" :alarms="alarms" @roomSelected="openChatRoom" @openCreateForm="toggleChatRoomForm" />
          <MeetingNav :team-no="teamNo" />
          <button @click="setTab('calendar')">📅 캘린더</button>
        </SideNav>
      </template>

        <div>
          <CalendarView v-if="activeTab === 'calendar'" :team-no="teamNo" />
          <ChatMain v-if="activeTab !== 'calendar'" :team-no="teamNo" :room-no="selectedRoomNo" :active-tab="activeTab" :alarms="alarms" 
            @roomCreated="onRoomCreated" @closeRoom="closeChatRoom" @alarmRead="onAlarmRead"/>
        </div>

    <template #footer>© PLUN</template>
  </DefaultLayout>
</template>
