<template>
  <div v-if="open" class="modal-backdrop">
    <div class="modal modal--solid meeting-info-modal">
      <!-- 헤더 -->
      <div class="modal__header">
        <h3 class="modal__title">회의 정보</h3>
        <button class="btn btn--secondary" @click="close">✕</button>
      </div>

      <!-- 바디 -->
      <div class="modal__body" v-if="!loading && !error">
        <!-- 회의명 -->
        <div class="mi-section">
          <div class="mi-label">회의명</div>
          <div class="mi-title">{{ detail.title || '(제목 없음)' }}</div>
        </div>

        <div class="divider"></div>

        <!-- 날짜 / 시간 -->
        <div class="mi-meta">
          <div class="mi-meta__item">
            <div class="mi-meta__label">날짜</div>
            <div class="mi-meta__value">{{ fmtDate(detail.scheduledTime) }}</div>
          </div>
          <div class="mi-meta__item">
            <div class="mi-meta__label">시간</div>
            <div class="mi-meta__value">
              {{ fmtTime(detail.scheduledTime) }} ~ {{ fmtTime(detail.scheduledEndTime) }}
            </div>
          </div>
        </div>

        <!-- 참여자 -->
        <div class="mi-section">
          <div class="mi-label">참여자</div>
          <div v-if="(detail.participants || []).length === 0" class="mi-empty">참여자 없음</div>
          <ul v-else class="mi-people" role="list">
            <li v-for="p in detail.participants" :key="p.userNo" class="mi-person">
              <div class="mi-avatar">{{ (p.name || '회').slice(0, 1) }}</div>
              <div class="mi-person__body">
                <div class="mi-person__name">{{ p.name || ('사용자 #' + p.userNo) }}</div>
                <div v-if="p.userNo === detail.creatorUserNo" class="mi-person__sub">생성자</div>
              </div>
            </li>
          </ul>
        </div>
      </div>

      <!-- 로딩/에러 -->
      <div class="modal__body" v-else-if="loading">불러오는 중…</div>
      <div class="modal__body" v-else style="color:#d33; white-space:pre-line;">{{ error }}</div>

      <!-- 푸터 -->
      <div class="modal__footer">
        <button class="btn btn--primary" :disabled="loading" @click="openPrejoin">마이크·카메라 설정하고 입장</button>

        <template v-if="detail.isCreator">
          <button class="btn btn--secondary" @click="$emit('request-edit', detail)">수정</button>
        </template>

        <div class="spacer"></div>
        <button class="btn btn--secondary" @click="close">닫기</button>
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

watch(
  () => props.open,
  async (v) => { if (v && props.roomCode) await fetchDetail(); else reset() },
  { immediate: true }
)
watch(
  () => props.roomCode,
  async (v, ov) => { if (v && v !== ov && props.open) await fetchDetail() }
)

async function fetchDetail() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await instance.get(`/meeting-rooms/${props.roomCode}`, {
      params: { userNo: userNo.value }
    })
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

function reset() {
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

function close() { emit('update:open', false) }
function openPrejoin() { emit('open-prejoin', detail.value) }

/* 포맷/표시 유틸 */
const p2 = (n) => String(n).padStart(2, '0')
function toDate(v) { const d = new Date(v); return isNaN(d) ? null : d }
function fmtDate(v) { const d = toDate(v); return d ? `${d.getFullYear()}.${p2(d.getMonth() + 1)}.${p2(d.getDate())}` : '' }
function fmtTime(v) { const d = toDate(v); return d ? `${p2(d.getHours())}:${p2(d.getMinutes())}` : '' }
function fmtTimeRange(s, e) { const a = fmtTime(s); const b = fmtTime(e); return a && b ? `${a} ~ ${b}` : (a || b || '') }
function initials(name, userNo) { if (name && name.trim()) return name.trim().slice(0, 2); return String(userNo ?? '').slice(-2).padStart(2, '0') }
</script>
