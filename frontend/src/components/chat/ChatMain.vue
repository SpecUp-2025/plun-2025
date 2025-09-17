<script setup>
import { ref } from 'vue';
import ChatRoom from './ChatRoom.vue'
import ChatRoomForm from './ChatRoomForm.vue'
import { useAlarmStore } from '@/store/useAlarmStore' 

//const alarms = ref([]);

const alarmStore = useAlarmStore()

const props = defineProps({
  teamNo: { type: Number, required: true },
  roomNo: { type: Number, default: null },
  activeTab: { type: String, default: 'calendar' }
})

const emits = defineEmits(['roomCreated', 'closeRoom'])


// 🔧 이 부분이 없으면 Vue가 경고 띄움!
const handleAlarmRead = (alarmNo) => {
  alarmStore.markAsRead(alarmNo)
}
</script>

<template>
  <div style="margin-top: 12px;">
    <ChatRoom v-if="roomNo" 
      :key="roomNo" 
      :team-no="teamNo" 
      :room-no="roomNo"
      @closeRoom="() => emits('closeRoom')"
      @alarmRead="handleAlarmRead"/>

    <ChatRoomForm v-else-if="activeTab === 'createChat'" 
    :team-no="teamNo" @roomCreated="roomNo => emits('roomCreated', roomNo)"
    />
  </div>
</template>
