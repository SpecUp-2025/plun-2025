<template>
  <div class="side">
    <ul>
      <li
        v-for="room in chatRooms"
        :key="room.roomNo"
        @click="enterRoom(room.roomNo)"
        class="chat-room-item"
      >
        {{ room.roomName }}
        <span v-if="hasUnreadByRoom[room.roomNo]" class="badge-new">New</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, defineEmits } from 'vue'
import { useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useUserStore } from '@/store/userStore'
import { useAlarmStore } from '@/store/useAlarmStore'

const alarmStore = useAlarmStore()
const alarms = computed(() => alarmStore.alarms)
const unreadCount = computed(() => alarmStore.unreadCount)

const emit = defineEmits(['roomSelected', 'updateAlarms'])
const chatRooms = ref([])
const router = useRouter()

const showDropdown = ref(false)
const stompClient = ref(null)
const userStore = useUserStore()
const userNo = userStore.user?.userNo

const enterRoom = async (roomNo) => {
  const unread = alarms.value.filter(a => a.referenceNo === roomNo && a.isRead === 'N')
  
  await Promise.all(unread.map(a => instance.put(`/alarms/${a.alarmNo}/read`)))
  
  alarmStore.alarms = alarmStore.alarms.map(a => {
    if (a.referenceNo === roomNo) {
      return { ...a, isRead: 'Y' }
    }
    return a
  })

  emit('updateAlarms', alarms.value)
  unreadCount.value = alarms.value.filter(a => a.isRead === 'N').length

  emit('roomSelected', Number(roomNo))  // 부모 컴포넌트에 방 번호 전달
}

const fetchChatRooms = async () => {
  try {
    const res = await instance.get('/chat/rooms')
    chatRooms.value = res.data
  } catch (error) {
    console.error('채팅방 목록 불러오기 실패:', error)
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

      emit('updateAlarms', alarms.value)

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
const hasUnreadByRoom = computed(() => {
  const map = {}
  alarmStore.alarms.forEach(alarm => {
    if (alarm.isRead === 'N') {
      map[alarm.referenceNo] = true
    }
  })
  return map
})

onMounted(() => {
  if (!userNo) return
  fetchChatRooms()
  alarmStore.fetchAlarms(userNo)
  connectWebSocket()
})
</script>

<style scoped>

.side {
  background-color: #e9eef5; /* SideNav과 동일 */
  border-right: 1px solid #c5d4e7;
  padding: 0px;
  color: #2c3e50;
  font-weight: 550;
  font-size: 16px;
  overflow: auto;
}

.side ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.side li {
  padding: 0px 8px;
  margin-bottom: 6px;
  cursor: pointer;
  border-radius: 6px;
  font-weight: 700;
  font-size: 18px;
  color: #2c3e50;
  transition: background-color 0.2s ease;
  user-select: none;
}

.side li:hover {
  background-color: #e0efff;
  color: #4A90E2;
}

.badge-new {
  display: inline-block;
  background-color: #ff7a45; /* 부드러운 주황색 */
  color: white;
  font-weight: 600;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: 6px;
  user-select: none;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
</style>
