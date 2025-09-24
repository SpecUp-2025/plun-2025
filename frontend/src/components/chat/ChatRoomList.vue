<template>
  <div class="chatroom-container">
    <div
      v-for="room in chatRooms"
      :key="room.roomNo"
      @click="enterRoom(room.roomNo)"
      class="chatroom-item"
    >
      <div class="room-content">
        <span class="room-name">{{ room.roomName }}</span>
        <span v-if="hasUnreadByRoom[room.roomNo]" class="badge-new">New</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, defineEmits, watch } from 'vue'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'
import { useAlarmStore } from '@/store/useAlarmStore'

const alarmStore = useAlarmStore()

const emit = defineEmits(['roomSelected', 'updateAlarms'])
const chatRooms = ref([])
const userStore = useUserStore()
const userNo = userStore.user?.userNo

const props = defineProps({
  teamNo: { type: Number, required: true },
  roomNameUpdate: { type: Object, default: null }
})

const enterRoom = async (roomNo) => {
  await alarmStore.markAlarmsAsReadByRoom(roomNo)
  emit('updateAlarms', alarmStore.alarms)
  emit('roomSelected', Number(roomNo))
}

const fetchChatRooms = async () => {
  try {
    const res = await instance.get('/chat/rooms')
    chatRooms.value = res.data
  } catch (error) {
    console.error('채팅방 목록 불러오기 실패:', error)
  }
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

watch(() => props.roomNameUpdate, (newUpdate) => {
  if (newUpdate && newUpdate.roomNo && newUpdate.roomName) {
    console.log('ChatRoomList에서 이름 변경 감지:', newUpdate);
    
    const roomIndex = chatRooms.value.findIndex(room => room.roomNo === newUpdate.roomNo);
    if (roomIndex !== -1) {
      chatRooms.value[roomIndex].roomName = newUpdate.roomName;
      console.log('채팅방 목록 이름 업데이트 완료:', newUpdate.roomName);
    }
  }
}, { deep: true })

onMounted(() => {
  if (!userNo) return
  fetchChatRooms()
  alarmStore.fetchAlarms(userNo)
})
</script>

<style scoped>
.chatroom-container {
   max-height: calc(100vh - 40px); 
   overflow-y: auto; 
  }

.chatroom-item {
  display: block;
  width: 100%;
  padding: 5px 5px;
  margin-bottom: 6px;
  border: none;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  color: #2d3748;
  font-size: 15px;
  font-weight: normal;
  border-radius: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.8);
  position: relative;
  overflow: hidden;
  text-align: center;
}

.chatroom-item::before {
  content: none;
}

.chatroom-item:hover {
  background: rgba(0, 105, 217, 0.05);
  color: #004080;
  border-color: rgba(0, 105, 217, 0.1);
}

.chatroom-item:hover::before {
  left: 100%;
}

.chatroom-item:active {
  transform: scale(0.99);
  transition-duration: 0.1s;
}

.room-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.room-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
}

.badge-new {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6b6b, #ee5a52);
  color: white;
  font-weight: 700;
  font-size: 9px;
  padding: 3px 7px;
  border-radius: 12px;
  min-width: 28px;
  height: 18px;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
  animation: pulse 2s infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0% {
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
  }
  50% {
    box-shadow: 0 2px 15px rgba(255, 107, 107, 0.5);
  }
  100% {
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
  }
}

/* 읽지 않은 메시지가 있는 채팅방 강조 */
.chatroom-item:has(.badge-new) {
  border-left: 3px solid #ff6b6b;
  background: rgba(255, 107, 107, 0.05);
}

/* 반응형 */
@media (max-width: 768px) {
  .chatroom-item {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .badge-new {
    font-size: 8px;
    padding: 2px 5px;
    min-width: 24px;
    height: 16px;
  }
}
</style>