<template>
    <link rel="stylesheet"
    href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css"/>

    <div class="alarm-dropdown">
    <div class="icon-button " @click="toggleDropdown">
      <i class="fas fa-bell"></i>
      <span v-if="alarmStore.unreadCount > 0" class="badge">{{ alarmStore.unreadCount }}</span>
    </div>

    <div v-if="showDropdown" class="dropdown-content">
      <div v-if="alarmStore.alarms.length === 0" class="no-alarm">알림이 없습니다</div>

      <ul v-else>
        <li
          v-for="alarm in alarmStore.alarms.filter(a => a.isRead === 'N')"
          :key="alarm.alarmNo"
          @click="goToChatRoom(alarm)"
          class="alarm-item"
        >
          💬 새로운 메시지가 도착했습니다.
        </li>
      </ul>

      <button v-if="alarmStore.alarms.length" @click="markAllAsRead">모두 읽음</button>
    </div>
  </div>
</template>

<script setup>
import { useAlarmStore } from '@/store/useAlarmStore'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const emit = defineEmits(['alarmClicked'])
const router = useRouter()
const alarmStore = useAlarmStore()

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const showDropdown = ref(false)

const goToChatRoom = async (alarm) => {
  try {
    await alarmStore.markAsRead(alarm.alarmNo)
    // 읽음 처리 후 라우팅
    alarm.isRead = 'Y' // 혹은 이 부분은 스토어에서 업데이트 됐으니 필요 없을 수도 있음
    emit('alarmClicked', alarm.referenceNo)
    showDropdown.value = false
  } catch (error) {
    console.error('❌ 알림 읽음 처리 실패', error)
  }
}

const markAllAsRead = async () => {
  const unreadAlarms = alarmStore.alarms.filter(a => a.isRead === 'N')
  for (const alarm of unreadAlarms) {
    try {
      await alarmStore.markAsRead(alarm.alarmNo)
      alarm.isRead = 'Y'
    } catch (e) {
      console.error('❌ 알림 읽음 처리 실패', e)
    }
  }
}
</script>

<style scoped>
.alarm-dropdown {
  position: relative;
  display: inline-block;
  margin-left: 20px;
}
.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background-color: red;
  color: white;
  font-size: 10px;
  padding: 2px 5px;
  border-radius: 50%;
  line-height: 1;
  box-shadow: 0 0 0 2px white; /* 흰 테두리 */
}

.icon-button {
  position: relative;
  background: none;
  border: none;
  font-size: 20px;
  color: #3399FF;
  cursor: pointer;
}

.dropdown-content {
  position: absolute;
  background-color: white;
  border: 1px solid #ccc;
  width: 250px;
  right: 0;
  margin-top: 10px;
  z-index: 100;
  padding: 10px;
}
.alarm-item {
  padding: 8px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}
.alarm-item:hover {
  background-color: #f5f5f5;
}
.no-alarm {
  text-align: center;
  padding: 10px;
  color: gray;
}
</style>
