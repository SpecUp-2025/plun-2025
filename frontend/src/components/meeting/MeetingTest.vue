<template>
  <div>
    <button @click="open = !open">
      회의 {{ open ? '▲' : '▼' }}
    </button>

    <div v-if="open">
      <button @click="modalOpen = true">회의 생성하기</button>

      <div>
        <button @click="loadActive" :disabled="loading">
          {{ loading ? '불러오는 중…' : '회의 목록' }}
        </button>
        <div v-if="error" style="color:red; white-space:pre-line">{{ error }}</div>
        <ul v-if="rooms.length">
          <li v-for="m in rooms" :key="m.roomNo">
            <button @click="onEnter(m)">
              {{ m.title }} | {{ m.scheduledTime }} ~ {{ m.scheduledEndTime || '(미지정)' }}
            </button>
          </li>
        </ul>
        <div v-else-if="loaded && !loading">표시할 회의가 없습니다.</div>
      </div>
    </div>

    <!-- 모달 -->
    <MeetingCreateModal v-model:open="modalOpen" :team-no="10" @created="onCreated" />
  </div>
</template>

<script setup>
import { ref } from "vue";
import instance from '@/util/interceptors'
import MeetingCreateModal from './MeetingCreateModal.vue'

const open = ref(false)
const modalOpen = ref(false)

const loading = ref(false)
const loaded = ref(false)
const error = ref('')
const rooms = ref([])

async function loadActive() {
  loading.value = true
  loaded.value = false
  error.value = ''
  try {
    const res = await instance.get('/meeting-rooms/active')
    rooms.value = res.data || []
    loaded.value = true
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    loading.value = false
  }
}

function onEnter(m) {
  console.log('입장 클릭:', m.roomNo, m.roomCode)
  // TODO: 프리조인/라우팅 연결
}

async function onCreated(created) {
  // created: { roomNo, roomCode }
  await loadActive()
}
</script>
