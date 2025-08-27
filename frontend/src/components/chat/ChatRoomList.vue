<template>
  <div>
    <h2>채팅방 목록</h2>

    <!-- 통합 알림 뱃지 -->
    <div class="alarm-dropdown">
      <div class="alarm-icon" @click="toggleDropdown">
        🔔
        <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
      </div>

      <div v-if="showDropdown" class="dropdown-content">
        <div v-if="alarms.length === 0" class="no-alarm">알림이 없습니다</div>

        <ul v-else>
          <li
            v-for="alarm in alarms"
            :key="alarm.alarmNo"
            @click="goToChatRoom(alarm)"
            class="alarm-item"
            :class="{ read: alarm.isRead === 'Y' }"
            >

            <strong>{{ alarm.senderName }}</strong> : {{ alarm.content }}
          </li>
        </ul>

        <button v-if="alarms.length" @click="markAllAsRead">모두 읽음</button>
      </div>
    </div>

    <ul>
      <li v-for="room in chatRooms" :key="room.roomNo" @click="enterRoom(room.roomNo)">
        {{ room.roomName }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useUserStore } from '@/store/userStore'

const chatRooms = ref([])
const router = useRouter()

const alarms = ref([])         // 전체 알림 목록
const unreadCount = ref(0)     // 읽지 않은 알림 수
const showDropdown = ref(false)
const stompClient = ref(null)
const userStore = useUserStore()
const userNo = userStore.user?.userNo

const enterRoom = (roomNo) => {
  router.push(`/room/${roomNo}`)
}

const fetchChatRooms = async () => {
  try {
    const res = await instance.get('/chat/rooms')
    chatRooms.value = res.data
  } catch (error) {
    console.error('채팅방 목록 불러오기 실패:', error)
  }
}

// 🔔 안 읽은 알림 목록 불러오기
const fetchAlarms = async () => {
  try {
    const res = await instance.get(`/alarms/${userNo}`)
    alarms.value = res.data
    unreadCount.value = alarms.value.filter(a => a.isRead === 'N').length
  } catch (e) {
    console.error('알림 불러오기 실패:', e)
  }
}

// 🔔 WebSocket 연결
const connectWebSocket = () => {
  const socket = new SockJS('/ws-chat')
  stompClient.value = Stomp.over(socket)

  stompClient.value.connect({}, () => {
    console.log('📡 알림 WebSocket 연결 성공')

    stompClient.value.subscribe(`/topic/notifications/${userNo}`, (msg) => {
      const alarm = JSON.parse(msg.body)
      console.log('🔔 수신된 알림:', alarm)

      alarms.value.unshift(alarm)
      unreadCount.value++
    })
  }, (err) => {
    console.error('❌ 알림 WebSocket 연결 실패:', err)
  })
}

// 알림 목록 토글
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

// 알림 클릭 → 채팅방 이동
const goToChatRoom = async (alarm) => {
  if (alarm.alarmNo) {
    await instance.put(`/alarms/${alarm.alarmNo}/read`) // 읽음 처리
    alarm.isRead = 'Y'
    unreadCount.value = alarms.value.filter(a => a.isRead === 'N').length
  }

  router.push(`/room/${alarm.referenceNo}`)
}

// 모든 알림 읽음 처리
const markAllAsRead = async () => {
  const unread = alarms.value.filter(a => a.isRead === 'N')

  await Promise.all(unread.map(alarm =>
    instance.put(`/alarms/${alarm.alarmNo}/read`)
  ))

  alarms.value.forEach(a => a.isRead = 'Y')
  unreadCount.value = 0
}

// 초기 실행
onMounted(() => {
  if (!userNo) return
  fetchChatRooms()
  fetchAlarms()
  connectWebSocket()
})
</script>


<style scoped>
.badge {
  background-color: red;
  color: white;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 12px;
  margin-left: 4px;
}

.alarm-dropdown {
  position: relative;
  display: inline-block;
  margin-bottom: 10px;
}

.dropdown-content {
  position: absolute;
  background-color: white;
  border: 1px solid #ccc;
  width: 200px;
  max-height: 300px;
  overflow-y: auto;
  z-index: 999;
  padding: 10px;
}

.alarm-item {
  cursor: pointer;
  padding: 5px;
  border-bottom: 1px solid #eee;
}

.alarm-item.read {
  color: gray;
  font-style: italic;
}
</style>
