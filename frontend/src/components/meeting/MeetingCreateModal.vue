<template>
  <div v-if="open" style="position:fixed; inset:0; background:rgba(0,0,0,.3);">
    <div style="background:#fff; padding:12px; margin:40px auto; max-width:420px;">
      <h3>회의 생성</h3>

      <div style="margin-bottom:8px;">팀번호: {{ teamNo }}</div>

      <div style="margin-bottom:6px;">
        <label>제목:
          <input v-model="form.title" placeholder="예: 주간 회의" />
        </label>
      </div>

      <div style="margin-bottom:6px;">
        <label>참여자 선택:</label>
        <div v-if="mLoading">팀원 목록 불러오는 중…</div>
        <div v-else>
          <div v-for="m in members" :key="m.userNo">
            <label>
              <input
                type="checkbox"
                :value="m.userNo"
                v-model="form.participantIds"
                :disabled="m.userNo === myUserNo"
              />
              {{ m.name }} <span v-if="m.userNo === myUserNo"> (나)</span>
            </label>
          </div>
          <div v-if="mError" style="color:#d33; white-space:pre-line">{{ mError }}</div>
          <div v-if="!members.length && !mLoading">팀원 없음</div>
        </div>
      </div>

      <div style="margin-bottom:6px;">회의 시간</div>

      <div style="margin-bottom:6px;">
        <label>시작 예정시간:
          <input type="datetime-local" v-model="form.startLocal" />
        </label>
      </div>

      <div style="margin-bottom:6px;">
        <label>종료 예정시간:
          <input type="datetime-local" v-model="form.endLocal" required />
        </label>
      </div>

      <div style="margin-bottom:6px;">
        <label>
          <input type="checkbox" v-model="form.privateRoom" />
          비공개 방
        </label>
      </div>

      <div v-if="form.privateRoom" style="margin-bottom:6px;">
        <label>비밀번호:
          <input type="password" v-model="form.roomPassword" />
        </label>
      </div>

      <div style="margin-top:8px; display:flex; gap:8px;">
        <button @click="submit" :disabled="creating">
          {{ creating ? '생성 중…' : '생성' }}
        </button>
        <button @click="close" :disabled="creating">취소</button>
      </div>

      <div v-if="err" style="color:#d33; white-space:pre-line; margin-top:6px;">{{ err }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

// props / emits
const props = defineProps({
  open:   { type: Boolean, default: false },
  teamNo: { type: Number,  required: true }
})
const emit = defineEmits(['update:open', 'created'])

// 내 정보
const userStore = useUserStore()
const myUserNo = computed(() => Number(userStore.user?.userNo ?? 0))

// state
const members   = ref([])          // [{ userNo:number, name:string }]
const mLoading  = ref(false)
const mError    = ref('')
const creating  = ref(false)
const err       = ref('')

const form = ref({
  title: '',
  startLocal: '',
  endLocal: '',
  privateRoom: false,
  roomPassword: '',
  participantIds: [] // number[]
})

// 모달 열릴 때 초기화 + 팀원 로드
watch(() => props.open, (v) => {
  if (v) {
    initDefaults()
    loadMembersByTeam()
  }
})

function initDefaults () {
  err.value = ''
  const now = new Date()
  const end = new Date(now.getTime() + 30 * 60 * 1000)
  form.value = {
    title: '',
    startLocal: toLocalDT(now),
    endLocal: toLocalDT(end),
    privateRoom: false,
    roomPassword: '',
    // 본인 자동 선택
    participantIds: myUserNo.value ? [myUserNo.value] : []
  }
}

async function loadMembersByTeam () {
  mLoading.value = true
  mError.value = ''
  members.value = []
  try {
    const { data } = await instance.get(`/teams/${props.teamNo}/members`)
    members.value = (data ?? []).map(x => ({
      userNo: Number(x.userNo),
      name: String(x.name ?? '')
    }))
  } catch (e) {
    mError.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    mLoading.value = false
  }
}

function toLocalDT(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function toISO(localStr) {
  return localStr || null; // 그냥 로컬 문자열을 보냄
}

function close () {
  emit('update:open', false)
}

async function submit () {
  err.value = ''

  if (!form.value.title?.trim()) {
    err.value = '제목을 입력하세요.'
    return
  }
  if (!form.value.endLocal) {
    err.value = '종료 예정시간은 필수입니다.'
    return
  }

  if (form.value.startLocal && form.value.endLocal) {
    const s = new Date(form.value.startLocal).getTime()
    const e = new Date(form.value.endLocal).getTime()
    if (Number.isFinite(s) && Number.isFinite(e) && e < s) {
      err.value = '종료 시간이 시작 시간보다 빠릅니다.'
      return
    }
  }

  // 숫자화 + 중복 제거 + 본인 강제 포함
  const set = new Set((form.value.participantIds || [])
    .map(n => Number(n))
    .filter(n => Number.isFinite(n)))
  if (myUserNo.value) set.add(myUserNo.value)
  const participantIds = Array.from(set)

  const body = {
    teamNo: props.teamNo,
    title: form.value.title.trim(),
    scheduledTime: form.value.startLocal ? toISO(form.value.startLocal) : null,
    scheduledEndTime: toISO(form.value.endLocal),
    privateRoom: !!form.value.privateRoom,
    roomPassword: form.value.privateRoom ? form.value.roomPassword : null,
    participantIds,
    creatorUserNo: myUserNo.value 
  }

  creating.value = true
  try {
    const { data } = await instance.post('/meeting-rooms', body)
    emit('created', data)     
    emit('update:open', false) 
  } catch (e) {
    err.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    creating.value = false
  }
}
</script>
