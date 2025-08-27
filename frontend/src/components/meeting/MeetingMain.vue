<template>
  <div>
    <button @click="toggleOpen">
      회의 {{ open ? '▲' : '▼' }}
    </button>

    <div v-if="open" style="margin-top:8px;">
      <button @click="modalOpen = true">회의 생성하기</button>

      <div style="margin-top:8px;">
        <button @click="loadActive" :disabled="loading">
          {{ loading ? '불러오는 중…' : '회의 목록' }}
        </button>

        <div v-if="error" style="color:red; white-space:pre-line; margin-top:6px">{{ error }}</div>

        <ul v-if="rooms.length" style="margin-top:8px;">
          <li v-for="m in rooms" :key="m.roomNo" style="margin-bottom:6px;">
            <button @click="onEnter(m)">
              {{ m.title }}
              | {{ fmtDateTime(m.scheduledTime) }}
              ~ {{ m.scheduledEndTime ? fmtDateTime(m.scheduledEndTime) : '(미지정)' }}
            </button>
          </li>
        </ul>

        <div v-else-if="loaded && !loading" style="margin-top:8px;">
          표시할 회의가 없습니다.
        </div>
      </div>
    </div>

    <MeetingCreateModal v-model:open="modalOpen" :team-no="10" @created="onCreated" />
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

// 날짜/시간 포맷: "YYYY-MM-DD HH:mm" (로컬 타임존)
function fmtDateTime(v) {
  if (!v) return ''
  try {
    const d = new Date(v)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mi = String(d.getMinutes()).padStart(2, '0')
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
  } catch {
    return String(v)
  }
}

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

function toggleOpen () {
  open.value = !open.value
  if (open.value && !loaded.value && !loading.value) {
    loadActive()
  }
}

function onEnter (m) {
  router.push({ name: 'MeetingPrejoin', params: { roomCode: m.roomCode } })
}
</script>
