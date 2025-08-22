<!-- src/components/meeting/MeetingMain.vue -->
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

    <!-- 생성 모달 -->
    <MeetingCreateModal v-model:open="modalOpen" :team-no="10" @created="onCreated" />
    <!-- 프리조인 모달은 라우터로 처리 -->
    <router-view />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import MeetingCreateModal from './MeetingCreateModal.vue'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const userNo = computed(() => userStore.user?.userNo ?? null)

const open = ref(false)
const modalOpen = ref(false)

const loading = ref(false)
const loaded = ref(false)
const error = ref('')
const rooms = ref([])

async function loadActive () {
  loading.value = true
  loaded.value = false
  error.value = ''
  try {
    const { data } = await instance.get('/meeting-rooms/active', {
      params: { teamNo: 10, userNo: userNo.value }
    })
    rooms.value = Array.isArray(data) ? data : []
    loaded.value = true
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    loading.value = false
  }
}

async function onCreated () {
  await loadActive()
}

// 프리조인 라우팅
function onEnter (m) {
  router.push({ name: 'MeetingPrejoin', params: { roomCode: m.roomCode } })
}
</script>
