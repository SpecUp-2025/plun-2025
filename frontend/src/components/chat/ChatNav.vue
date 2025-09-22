<script setup>
import { ref } from 'vue'
import ChatRoomList from './ChatRoomList.vue'
import { useAlarmStore } from '@/store/useAlarmStore'
import SideNav from '../layout/SideNav.vue'


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
        <div style="display: flex; align-items: center; gap: 100px;">
        <!-- 채팅 목록 토글 -->
        <button @click="toggleChatRoomList">{{ showChatRoomList  ? '▾' : '▸' }} 채팅</button>
        <!-- 채팅방 생성 버튼 (항상 + 유지) -->
        <button @click="handleCreateForm" title="채팅방 생성">＋</button>
    </div>

    <div v-if="showChatRoomList" style="margin-left: 12px; margin-top: 8px;">
    <SideNav>
      <ChatRoomList :team-no="teamNo" :room-name-update="roomNameUpdate"
      @roomSelected="roomNo => emits('roomSelected', roomNo)" />
    </SideNav>
    </div>
  </div>
</template>
