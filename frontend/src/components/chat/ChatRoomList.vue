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

.side {
  background-color: #e9eef5;
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
  background: linear-gradient(135deg, #ff9a3c, #ff5e62); /* 그라데이션 배경 */
  color: white;
  font-weight: bold;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 999px;
  margin-left: 8px;
  user-select: none;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15); /* 살짝 떠있는 느낌 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  animation: pop-badge 0.3s ease-out;
}

@keyframes pop-badge {
  0% {
    transform: scale(0.6);
    opacity: 0;
  }
  80% {
    transform: scale(1.1);
    opacity: 1;
  }
  100% {
    transform: scale(1);
  }
}
</style>
