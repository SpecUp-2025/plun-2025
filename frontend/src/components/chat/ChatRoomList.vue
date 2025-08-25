<template>
  <div>
    <h2>채팅방 목록</h2>
    <ul>
      <li v-for="room in chatRooms" :key="room.roomNo" @click="enterRoom(room.roomNo)">
        {{ room.roomName }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import instance from '@/util/interceptors'

const chatRooms = ref([])
const router = useRouter()

const fetchChatRooms = async () => {
  try {
    const res = await instance.get('/chat/rooms')  // 백엔드에서 채팅방 리스트 가져오는 API 경로 맞게 변경
    console.log('받은 채팅방 데이터:',res.data)
    chatRooms.value = res.data
  } catch (error) {
    console.error('채팅방 목록 불러오기 실패:', error)
  }
}

const enterRoom = (roomNo) => {
  router.push(`/room/${roomNo}`)
  console.log('채팅방 입장:', roomNo)
}

onMounted(() => {
  fetchChatRooms()
})
</script>
