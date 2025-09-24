<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import MeetingNav from '@/components/meeting/MeetingNav.vue'
import CalendarView from '@/components/calendar/CalendarView.vue'
import ChatNav from '@/components/chat/ChatNav.vue'
import ChatMain from '@/components/chat/ChatMain.vue'
import SideNav from '@/components/layout/SideNav.vue'
import instance from '@/util/interceptors'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useAlarmStore } from '@/store/useAlarmStore';

const route = useRoute()
const router = useRouter()
const teamNo = computed(() => Number(route.params.teamNo))

const selectedRoomNo = ref(null)
const activeTab = ref('calendar')

const teamName = ref('')
const roomNameUpdate = ref(null)

const alarmStore = useAlarmStore();
const alarms = computed(() => alarmStore.alarms);

const userStore = useUserStore()
const userNo = computed(() => userStore.user?.userNo)

const stompClient = ref(null)
const isWebSocketConnected = ref(false)

const connectWebSocket = () => {
  if (!userNo.value || isWebSocketConnected.value) {
    console.log('WebSocket 연결 스킵:', { userNo: userNo.value, isConnected: isWebSocketConnected.value });
    return;
  }

  console.log('WebSocket 연결 시도...');
  const socket = new SockJS('/ws-chat')
  stompClient.value = Stomp.over(socket)

  stompClient.value.connect({}, () => {
    console.log('📡 WebSocket 연결 성공')
    isWebSocketConnected.value = true;

    stompClient.value.subscribe(`/topic/notifications/${userNo.value}`, (msg) => {
      const alarm = JSON.parse(msg.body)
      console.log('🔔 수신된 알림 원본:', msg.body)
      console.log('🔔 수신된 알림 파싱:', alarm)
      console.log('🔔 알림 content 필드:', alarm.content)
      console.log('🔔 알림 alarmType:', alarm.alarmType)
      alarmStore.addAlarm(alarm);
    })
    stompClient.value.subscribe(`/topic/team/${teamNo.value}/roomNameUpdate`, (msg) => {
      const data = JSON.parse(msg.body);
      console.log('팀 레벨에서 채팅방 이름 변경 수신:', data);
      onRoomNameChanged(data);
    })
  }, (err) => {
    console.error('❌ WebSocket 연결 실패:', err)
    isWebSocketConnected.value = false;
  })
}

const disconnectWebSocket = () => {
  if (stompClient.value && stompClient.value.connected) {
    stompClient.value.disconnect(() => {
      console.log('📴 WebSocket 연결 해제됨')
      isWebSocketConnected.value = false;
    })
  }
}

const fetchAlarms = async () => {
  if (!userNo.value) return
  try {
    await alarmStore.fetchAlarms(userNo.value);
    console.log('초기 알림 불러오기 성공:', alarms.value.length + '개');
  } catch (error) {
    console.error('초기 알림 불러오기 실패:', error)
  }
}

const fetchTeamName = async () => {
  try {
    const res = await instance.get(`/teams/teamDetail/${teamNo.value}`);
    teamName.value = res.data.list.teamName;
    console.log('✅ 팀 이름 불러오기 성공:', teamName.value);
  } catch (error) {
    console.error('❌ 팀 이름 불러오기 실패:', error);
  }
}

const isInitialized = ref(false);

const initializeUser = async () => {
  if (!userNo.value || isInitialized.value) return;
  
  console.log('사용자 초기화 시작:', userNo.value);
  isInitialized.value = true;
  
  await fetchAlarms();
  connectWebSocket();
};

watch(() => userNo.value, (newUserNo) => {
  if (newUserNo && !isInitialized.value) {
    initializeUser();
  }
}, { immediate: true })

onMounted(() => {
  if (userNo.value && !isInitialized.value) {
    initializeUser();
  }
  fetchTeamName();
})

onBeforeUnmount(() => {
  disconnectWebSocket()
})

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
  console.log('teamMain에서 알림 읽음 완료 처리:', alarmNo);
  alarmStore.markAsRead(alarmNo);
}



const onRoomNameChanged = (data) => {
  console.log('TeamMain에서 채팅방 이름 변경 이벤트 수신:', data);
  roomNameUpdate.value = data;
  console.log('roomNameUpdate.value 업데이트됨:', roomNameUpdate.value);
}

const onAlarmClick = (alarm) => {
  console.log('알림 클릭:', alarm);
  
  if (alarm.alarmType === 'CALENDAR_INVITE') {

    console.log('캘린더 초대 알림 클릭 - 캘린더 탭으로 이동');
    activeTab.value = 'calendar';
    selectedRoomNo.value = null;
    
    alarmStore.markAsRead(alarm.alarmNo);
    
  } else if (alarm.alarmType === 'CHAT' || alarm.alarmType === 'CHAT_MENTION') {
    console.log('채팅 알림 클릭 - 채팅방으로 이동:', alarm.referenceNo);
    openChatRoom(alarm.referenceNo);
    
  } else {
    console.log('알 수 없는 알림 타입:', alarm.alarmType);
  }
}

const goToNotifications = () => {
  // 예를 들어 팀 메인에서 알림 탭으로 이동하거나 알림 페이지가 있으면 이동
  // 여기선 알림 탭으로 강제 이동하는 예시
  activeTab.value = 'notifications'  // TeamMain.vue 에 알림 탭이 따로 구현되어 있다면 사용
  // 만약 알림 페이지가 따로 있다면 router.push({ name: 'alarmPage' }) 등으로 변경
  console.log('알림 페이지 이동 (미구현 - 필요시 수정)')
}


</script>

<template>
  <DefaultLayout
    @alarmClicked="onAlarmClick"
  >
    <template #header><strong>{{ teamName }}'s Workspace</strong></template>

    <template #sidebar>
      <SideNav>
        <router-link :to="{ name: 'teamList' }" class="nav-button">← Back</router-link>
        <MeetingNav :team-no="teamNo" />
        <ChatNav
          :team-no="teamNo"
          :alarms="alarms"
          :room-name-update="roomNameUpdate"
          @roomSelected="openChatRoom"
          @openCreateForm="toggleChatRoomForm"
        />
        <button @click="setTab('calendar')" class="calendar-button">캘린더</button>
      </SideNav>
    </template>

    <div>
      <CalendarView v-if="activeTab === 'calendar'" :team-no="teamNo" />
      <ChatMain
        v-if="activeTab !== 'calendar'"
        :team-no="teamNo"
        :room-no="selectedRoomNo"
        :active-tab="activeTab"
        :alarms="alarms"
        @roomCreated="onRoomCreated"
        @closeRoom="closeChatRoom"
        @alarmRead="onAlarmRead"
        @roomNameChanged="onRoomNameChanged"
      />
    </div>

    <template #footer>© PLUN</template>
  </DefaultLayout>
</template>

<style scoped>
.calendar-button {
  margin-top: auto;
}
</style>

