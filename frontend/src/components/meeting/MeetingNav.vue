<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

import MeetingCreateModal from './MeetingCreateModal.vue'
import MeetingPrejoinModal from './MeetingPrejoinModal.vue'
import MeetingInfoModal from './MeetingInfoModal.vue'   // ⬅️ 추가

const route = useRoute()
const userNo = computed(() => useUserStore().user?.userNo ?? null)
const teamNo = computed(() => Number(route.params.teamNo))

const openMeet = ref(false)
const openSched = ref(false)
const modalOpen = ref(false)

const infoOpen = ref(false)              // ⬅️ 추가: 상세 모달
const prejoinOpen = ref(false)           // 기존 프리조인 모달
const selectedRoomCode = ref('')

const rooms = ref([])
const loading = ref(false)
const loaded = ref(false)
const error = ref('')

// 날짜/시간 포맷
const p2 = (n) => String(n).padStart(2, '0')
function toDate(v) { const d = new Date(v); return isNaN(d) ? null : d }
function fmtDate(v) { const d = toDate(v); if (!d) return ''; return `${d.getFullYear()}.${p2(d.getMonth() + 1)}.${p2(d.getDate())}` }
function fmtTime(v) { const d = toDate(v); if (!d) return ''; return `${p2(d.getHours())}:${p2(d.getMinutes())}` }

// API
async function loadActive() {
  if (!teamNo.value || !userNo.value) return
  loading.value = true; loaded.value = false; error.value = ''
  try {
    const { data } = await instance.get('/meeting-rooms/active', {
      params: { teamNo: teamNo.value, userNo: userNo.value }
    })
    rooms.value = Array.isArray(data) ? data : []
    loaded.value = true
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    loading.value = false
  }
}

// UI
function toggleMeet() { openMeet.value = !openMeet.value; if (openMeet.value && !loaded.value && !loading.value) loadActive() }
function toggleSched() { openSched.value = !openSched.value; if (openSched.value && !loaded.value && !loading.value) loadActive() }

// 기존: 바로 프리조인 → 변경: 상세 모달
function onClickRoom(m) {
  selectedRoomCode.value = m.roomCode
  infoOpen.value = true
}

// 상세 모달 → “마이크·카메라 설정하고 입장”
function handleOpenPrejoin(detail) {
  // detail.roomCode가 안전
  selectedRoomCode.value = detail.roomCode || selectedRoomCode.value
  infoOpen.value = false
  prejoinOpen.value = true
}

async function onCreated() {
  await loadActive()
  openSched.value = true
}

function handleEdit(detail) {
  // TODO: 수정 모달 열기 (다음 단계에 구현)
}

function handleDelete(detail) {
  // TODO: 삭제 확인 후 API 호출 (다음 단계에 구현)
}

// 상세 모달 닫힐 때 코드 정리
watch(infoOpen, (v) => {
  if (!v && !prejoinOpen.value) selectedRoomCode.value = ''
})
// 프리조인 모달 닫힐 때 코드 정리
watch(prejoinOpen, (v) => {
  if (!v) selectedRoomCode.value = ''
})

watch(teamNo, () => {
  rooms.value = []
  loaded.value = false
  openSched.value = false
  infoOpen.value = false
  prejoinOpen.value = false
  selectedRoomCode.value = ''
})
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:6px;">
      <button @click="toggleMeet">회의 {{ openMeet ? '▾' : '▸' }}</button>
      <button @click.stop="modalOpen = true" title="회의 생성">＋</button>
    </div>

    <div v-if="openMeet" style="margin-top:8px;">
      <button @click="modalOpen = true">회의 생성하기</button>

      <div style="margin-top:8px;">
        <button @click="toggleSched" :disabled="loading">
          예정된 회의 {{ openSched ? '▾' : '▸' }} <span v-if="loading">…</span>
        </button>

        <div v-if="error" style="color:red;white-space:pre-line;margin-top:6px">{{ error }}</div>

        <ul v-if="openSched && rooms.length" style="margin-top:8px;list-style:none;padding:0;">
          <li v-for="m in rooms" :key="m.roomNo || m.roomCode" style="margin:8px 0;">
            <button @click="onClickRoom(m)" style="text-align:left;">
              <div style="font-weight:600;">{{ m.title || '(제목 없음)' }}</div>
              <div style="font-size:12px;color:#666;">날짜 {{ fmtDate(m.scheduledTime) }}</div>
              <div style="font-size:12px;color:#666;">
                시간 {{ fmtTime(m.scheduledTime) }} ~ {{ m.scheduledEndTime ? fmtTime(m.scheduledEndTime) : '(미지정)' }}
              </div>
            </button>
          </li>
        </ul>

        <div v-else-if="openSched && loaded && !loading" style="margin-top:8px;">
          표시할 회의가 없습니다.
        </div>
      </div>
    </div>

    <!-- 생성 -->
    <MeetingCreateModal v-model:open="modalOpen" :team-no="teamNo" @created="onCreated" />

    <!-- 상세 보기(신규) -->
    <MeetingInfoModal v-if="selectedRoomCode" v-model:open="infoOpen" :room-code="selectedRoomCode"
      @open-prejoin="handleOpenPrejoin" @request-edit="handleEdit" @request-delete="handleDelete" />

    <!-- 프리조인 -->
    <MeetingPrejoinModal v-if="selectedRoomCode" v-model:open="prejoinOpen" :room-code="selectedRoomCode"
      :team-no="teamNo" />
  </div>
</template>
