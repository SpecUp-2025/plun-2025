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
        💬 
        <span v-if="alarm.alarmType === 'CHAT'">새 메시지가 도착했습니다.</span>
        <span v-else>{{ alarm.content }}</span>
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
    alarm.isRead = 'Y'
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
  font-family: 'Segoe UI', sans-serif;
}
.icon-button {
  position: relative;
  background: none;
  border: none;
  font-size: 28px;
  color: #153E75;
  cursor: pointer;
  transition: color 0.3s ease;
}
.icon-button:hover {
  color: #2678cc;
}
.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background-color: red;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 50%;
  line-height: 1;
  box-shadow: 0 0 0 2px white;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}
.dropdown-content {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 12px;
  width: 280px;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid #ddd;
  border-radius: 12px;
  z-index: 100;
  padding: 10px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  animation: dropdownFade 0.25s ease-out;
}
@keyframes dropdownFade {
  from {
    transform: translateY(-10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
.dropdown-content ul {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 260px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #ccc transparent;
}
.dropdown-content ul::-webkit-scrollbar {
  width: 6px;
}
.dropdown-content ul::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 3px;
}
.alarm-item {
  padding: 12px 14px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
  border-radius: 6px;
  word-break: break-word;
  white-space: normal;
}
.alarm-item:hover {
  background-color: #e6f2ff;
  transform: translateX(4px);
}

.alarm-item:last-child {
  border-bottom: none;
}

.no-alarm {
  text-align: center;
  padding: 25px 10px;
  font-size: 14px;
  color: #aaa;
}

.dropdown-content button {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  background: linear-gradient(to right, #153E75, #153E75);
  color: white;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s ease;
}
.dropdown-content button:hover {
  background: linear-gradient(to right, #2678cc, #3e9fff);
}
</style>
