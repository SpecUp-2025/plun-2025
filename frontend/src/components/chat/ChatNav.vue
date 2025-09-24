<script setup>
import { ref } from 'vue'
import ChatRoomList from './ChatRoomList.vue'
import { useAlarmStore } from '@/store/useAlarmStore'

const props = defineProps({
  teamNo: { type: Number, required: true },
  alarms: { type: Array, default: () => [] },
  roomNameUpdate: { type: Object, default: null }
})

const alarmStore = useAlarmStore()

const emits = defineEmits(['roomSelected', 'openCreateForm'])

const showChatRoomList = ref(false)

const toggleChatRoomList = () => {
  showChatRoomList.value = !showChatRoomList.value
}
const handleCreateForm = () => {
  emits('openCreateForm')
}
</script>

<template>
    <div>
        <!-- 채팅 목록 토글 + 채팅방 생성 버튼 -->
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 8px;">
          <!-- 채팅 목록 토글 버튼 -->
          <button @click="toggleChatRoomList" class="side-main-button">
            {{ showChatRoomList ? '▾' : '▸' }} 채팅
          </button>
          <button class="small-icon-button" @click="handleCreateForm" title="채팅방 생성">＋</button>
        </div>

    <div v-if="showChatRoomList" style="margin-left: 12px; margin-top: 8px;">
      <ChatRoomList :team-no="teamNo" :room-name-update="roomNameUpdate"
      @roomSelected="roomNo => emits('roomSelected', roomNo)" />
    </div>
  </div>
</template>
