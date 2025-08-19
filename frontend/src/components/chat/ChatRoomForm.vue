<template>
  <div>
    <h3>채팅방 생성</h3>
    <input v-model="roomName" placeholder="채팅방 이름 입력" />
    <button @click="createRoom">생성</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const roomName = ref('')

const createRoom = async () => {
  if (!roomName.value.trim()) {
    alert('방 이름을 입력하세요')
    return
  }

  try {
    const response = await axios.post('/api/chat/room', {
      roomName: roomName.value
    })
    console.log('채팅방 생성 성공:', response.data)
    // 생성된 채팅방 정보를 상위로 emit하거나 리스트에 추가
  } catch (error) {
    console.error('채팅방 생성 실패:', error)
  }
}
</script>
