<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

import MeetingCreateModal from './MeetingCreateModal.vue'
import MeetingPrejoinModal from './MeetingPrejoinModal.vue'
import MeetingInfoModal from './MeetingInfoModal.vue'
import MeetingEditModal from './MeetingEditModal.vue'

/* ---------- 상태 ---------- */
const route = useRoute()
const userNo = computed(() => useUserStore().user?.userNo ?? null)
const teamNo = computed(() => Number(route.params.teamNo))

const openMeet = ref(false)
const openSched = ref(false)
const modalOpen = ref(false)

const infoOpen = ref(false)
const prejoinOpen = ref(false)
const editOpen = ref(false)

const selectedRoomCode = ref('')  // Info/Prejoin에 사용
const selected = ref(null)         // EditModal에 사용(요약 객체)

/* 목록 상태 */
const rooms = ref([])
const loading = ref(false)
const loaded = ref(false)
const error = ref('')

/* ---------- 포맷 ---------- */
const p2 = (n) => String(n).padStart(2, '0')
function toDate(v) { const d = new Date(v); return isNaN(d) ? null : d }
function fmtDate(v) { const d = toDate(v); if (!d) return ''; return `${d.getFullYear()}.${p2(d.getMonth() + 1)}.${p2(d.getDate())}` }
function fmtTime(v) { const d = toDate(v); if (!d) return ''; return `${p2(d.getHours())}:${p2(d.getMinutes())}` }

/* ---------- API: 예정 회의 목록 ---------- */
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

/* ---------- UI 토글 ---------- */
function toggleMeet() {
  openMeet.value = !openMeet.value
  if (openMeet.value && !loaded.value && !loading.value) loadActive()
}
function toggleSched() {
  openSched.value = !openSched.value
  if (openSched.value && !loaded.value && !loading.value) loadActive()
}

/* ---------- 클릭: 회의 카드 ---------- */
function onClickRoom(m) {
  selectedRoomCode.value = m.roomCode
  infoOpen.value = true   // 상세는 MeetingInfoModal 내부가 roomCode로 조회
}

/* ---------- 생성 완료 ---------- */
async function onCreated() {
  await loadActive()
  openSched.value = true
}

/* ---------- InfoModal 이벤트 ---------- */
function onOpenPrejoin(detail) {
  // detail.roomCode가 오면 사용, 없으면 현재 선택 유지
  selectedRoomCode.value = detail?.roomCode || selectedRoomCode.value
  infoOpen.value = false
  prejoinOpen.value = true
}

function onRequestEdit(detail) {
  // Info → Edit 전환(겹침 방지)
  selected.value = detail || selected.value
  infoOpen.value = false
  nextTick(() => { editOpen.value = true })
}

async function onRequestDelete(detail) {
  const roomNo = Number(detail?.roomNo || 0)
  if (!roomNo) return
  const ok = window.confirm('회의를 삭제할까요?\n(달력 일정도 함께 정리됩니다)')
  if (!ok) return
  try {
    await instance.delete(`/meeting-rooms/${roomNo}`)
    infoOpen.value = false
    editOpen.value = false
    await loadActive()
  } catch (e) {
    alert(e?.response?.data?.message || e.message || String(e))
  }
}

/* ---------- EditModal 이벤트 ---------- */
async function onEdited(updated) {
  editOpen.value = false
  await loadActive()
  // 필요하면 상세 재열기
  const code = updated?.roomCode || selectedRoomCode.value
  if (code) {
    selectedRoomCode.value = code
    infoOpen.value = true
  }
}
async function onDeleted() {
  editOpen.value = false
  infoOpen.value = false
  await loadActive()
}

/* ---------- 워처: 팀 전환/창 닫힘 정리 ---------- */
watch(infoOpen, (v) => {
  if (!v && !prejoinOpen.value) {
    // 필요한 경우 선택값 정리
    // selectedRoomCode.value = ''
  }
})

watch(prejoinOpen, (v) => {
  if (!v) {
    // selectedRoomCode.value = ''
  }
})
watch(teamNo, () => {
  rooms.value = []
  loaded.value = false
  openSched.value = false
  infoOpen.value = false
  prejoinOpen.value = false
  editOpen.value = false
  selectedRoomCode.value = ''
  selected.value = null
})
</script>

<template>
  <div>
    <div style="display: flex; align-items: center; justify-content: space-between; gap: 8px;">
      <button @click="toggleMeet" class="side-main-button">
        {{ openMeet ? '▾' : '▸' }} 회의
      </button>
      <button @click.stop="modalOpen = true" class="small-icon-button" title="회의 생성">＋</button>
    </div>

    <div v-if="openMeet" style="margin-top:8px;">
    <div v-if="error" style="color:red;white-space:pre-line;margin-top:6px">{{ error }}</div>

    <ul v-if="rooms.length" style="margin-top:8px;list-style:none;padding:0;">
      <li v-for="m in rooms" :key="m.roomNo || m.roomCode" style="margin:8px 0;">
        <button @click="onClickRoom(m)" class="side-main-button">
          <div style="font-weight:normal;">{{ m.title || '(제목 없음)' }}</div>
          <div style="font-size:12px;color:#666;">날짜 {{ fmtDate(m.scheduledTime) }}</div>
          <div style="font-size:12px;color:#666;">
            시간 {{ fmtTime(m.scheduledTime) }} ~ {{ m.scheduledEndTime ? fmtTime(m.scheduledEndTime) : '(미지정)' }}
          </div>
        </button>
      </li>
    </ul>

    <div v-else-if="loaded && !loading" style="margin-top: 8px; margin-bottom: 16px; color: #888;">
      &nbsp;&nbsp;&nbsp;표시할 회의가 없습니다.
    </div>
    </div>

    <!-- 생성 -->
    <MeetingCreateModal v-model:open="modalOpen" :team-no="teamNo" @created="onCreated" />

    <!-- 상세: roomCode 필수 → 열릴 때만 렌더(v-if) + prop 전달 -->
    <MeetingInfoModal v-if="infoOpen" :open="infoOpen" :room-code="selectedRoomCode" @update:open="v => infoOpen = v"
      @request-edit="onRequestEdit" @request-delete="onRequestDelete" @open-prejoin="onOpenPrejoin" />

    <!-- 프리조인 -->
    <MeetingPrejoinModal v-if="selectedRoomCode" v-model:open="prejoinOpen" :room-code="selectedRoomCode"
      :team-no="teamNo" />

    <!-- 수정 -->
    <MeetingEditModal :open="editOpen" :detail="selected || {}" :team-no="teamNo" @update:open="v => editOpen = v"
      @updated="onEdited" @deleted="onDeleted" />
  </div>
</template>
