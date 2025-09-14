<template>
  <div>
    <h2>채팅방 목록</h2>
    <ul>
      <li
        v-for="room in chatRooms"
        :key="room.roomNo"
        @click="enterRoom(room.roomNo)"
        class="chat-room-item"
      >
        {{ room.roomName }}
        <span v-if="hasUnreadByRoom[room.roomNo]" class="dot">●</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
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

const enterRoom = async (roomNo) => {
  const unread = alarms.value.filter(a => a.referenceNo === roomNo && a.isRead === 'N')
  await Promise.all(unread.map(a => instance.put(`/alarms/${a.alarmNo}/read`)))
  unread.forEach(a => a.isRead = 'Y')
  unreadCount.value = alarms.value.filter(a => a.isRead === 'N').length

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

      // 채팅 초대 알림이면 채팅방 목록 다시 불러오기
    if (alarm.alarmType === 'CHAT') {
      console.log('📥 초대 알림 수신, 채팅방 목록 갱신')
      fetchChatRooms()
    }
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
const hasUnreadByRoom = computed(() => {
  const map = {}
  alarms.value.forEach(alarm => {
    if (alarm.isRead === 'N') {
      map[alarm.referenceNo] = true
    }
  })
  return map
})

// 초기 실행
onMounted(() => {
  if (!userNo) return
  fetchChatRooms()
  fetchAlarms()
  connectWebSocket()
})
</script>

<style scoped>
.dot {
  color: red;
  font-size: 14px;
  margin-left: 6px;
}
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
