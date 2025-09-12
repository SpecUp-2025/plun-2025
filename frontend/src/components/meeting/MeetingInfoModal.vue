<template>
  <div v-if="open" style="position:fixed; inset:0; background:rgba(0,0,0,.35); z-index:9999;">
    <div style="background:#fff; padding:12px; margin:40px auto; max-width:520px;">
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h3 style="margin:0;">회의 정보</h3>
        <button @click="close">✕</button>
      </div>

      <div v-if="loading" style="margin-top:10px;">불러오는 중…</div>
      <div v-else-if="error" style="margin-top:10px; color:#d33; white-space:pre-line;">{{ error }}</div>

      <div v-else style="margin-top:10px;">
        <div style="font-weight:700; font-size:16px; margin-bottom:8px;">
          {{ detail.title || '(제목 없음)' }}
        </div>

        <div style="font-size:13px; color:#666; margin-bottom:6px;">
          날짜: {{ fmtDate(detail.scheduledTime) }}
        </div>
        <div style="font-size:13px; color:#666; margin-bottom:12px;">
          시간: {{ fmtTime(detail.scheduledTime) }} ~ {{ fmtTime(detail.scheduledEndTime) }}
        </div>

        <div style="margin-top:8px;">
          <div style="font-weight:600; margin-bottom:4px;">참여자</div>
          <div v-if="(detail.participants || []).length === 0" style="font-size:13px; color:#888;">참여자 없음</div>
          <ul v-else style="padding-left:16px; margin:4px 0;">
            <li v-for="p in detail.participants" :key="p.userNo">
              {{ p.name || ('사용자 #' + p.userNo) }}
              <span v-if="p.userNo === detail.creatorUserNo" style="font-size:12px; color:#888;">(생성자)</span>
            </li>
          </ul>
        </div>

        <div style="display:flex; gap:8px; margin-top:14px;">
          <button @click="openPrejoin" :disabled="loading">마이크·카메라 설정하고 입장</button>

          <template v-if="detail.isCreator">
            <button @click="$emit('request-edit', detail)" :disabled="loading">수정</button>
            <button @click="$emit('request-delete', detail)" :disabled="loading" style="color:#b00;">삭제</button>
          </template>

          <div style="flex:1"></div>
          <button @click="close">닫기</button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

const props = defineProps({
  open: { type: Boolean, default: false },
  roomCode: { type: String, required: true }
})
const emit = defineEmits(['update:open', 'open-prejoin', 'request-edit', 'request-delete'])

const userNo = computed(() => useUserStore().user?.userNo ?? null)

const loading = ref(false)
const error = ref('')
const detail = ref({
  roomNo: null,
  roomCode: '',
  title: '',
  scheduledTime: null,
  scheduledEndTime: null,
  calDetailNo: null,
  creatorUserNo: null,
  isCreator: false,
  participants: []
})

watch(() => props.open, async (v) => {
  if (v) {
    await fetchDetail()
  } else {
    reset()
  }
})

async function fetchDetail () {
  loading.value = true
  error.value = ''
  try {
    const { data } = await instance.get(`/meeting-rooms/${props.roomCode}`, {
      params: { userNo: userNo.value }
    })
    // 기대 스키마: { roomNo, roomCode, title, scheduledTime, scheduledEndTime, calDetailNo, creatorUserNo, isCreator, participants:[{userNo,name}] }
    detail.value = {
      roomNo: data?.roomNo ?? null,
      roomCode: data?.roomCode ?? props.roomCode,
      title: data?.title ?? '',
      scheduledTime: data?.scheduledTime ?? null,
      scheduledEndTime: data?.scheduledEndTime ?? null,
      calDetailNo: data?.calDetailNo ?? null,
      creatorUserNo: data?.creatorUserNo ?? null,
      isCreator: !!data?.isCreator,
      participants: Array.isArray(data?.participants) ? data.participants : []
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    loading.value = false
  }
}

function reset () {
  loading.value = false
  error.value = ''
  detail.value = {
    roomNo: null,
    roomCode: props.roomCode,
    title: '',
    scheduledTime: null,
    scheduledEndTime: null,
    calDetailNo: null,
    creatorUserNo: null,
    isCreator: false,
    participants: []
  }
}

function close () {
  emit('update:open', false)
}

function openPrejoin () {
  emit('open-prejoin', detail.value) // 부모가 프리조인 모달을 열도록
}

// 날짜/시간 포맷(MeetingNav에 있던 간단 함수 재사용)
const p2 = (n) => String(n).padStart(2, '0')
function toDate(v){ const d=new Date(v); return isNaN(d)?null:d }
function fmtDate(v){ const d=toDate(v); if(!d) return ''; return `${d.getFullYear()}.${p2(d.getMonth()+1)}.${p2(d.getDate())}` }
function fmtTime(v){ const d=toDate(v); if(!d) return ''; return `${p2(d.getHours())}:${p2(d.getMinutes())}` }
</script>
