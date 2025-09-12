<template>
  <!-- 모달은 body 루트로 텔레포트(레이아웃 간섭 방지). 스크립트는 그대로 사용 -->
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="close">
      <div class="modal">
        <div class="modal__header">
          <h3 class="modal__title">회의 생성</h3>
          <button type="button" class="modal__close" @click="close" aria-label="닫기">✕</button>
        </div>

        <div class="modal__body">
          <form class="form" @submit.prevent="submit" novalidate>
            <!-- (A) 회의명 -->
            <section class="section">
              <label class="field__label field__label--lg" for="title">회의명</label>
              <div class="field__control">
                <input id="title" v-model="form.title" type="text" placeholder="예: 주간 회의" />
              </div>
            </section>

            <!-- (B) 회의 예정 시간 -->
            <section class="section">
              <h4 class="section__title">회의 예정 시간</h4>
              <div class="grid-2">
                <div class="field">
                  <label class="field__label" for="start">시작 예정 시간</label>
                  <div class="field__control">
                    <input
                      id="start"
                      type="datetime-local"
                      v-model="form.startLocal"
                      :min="minStartLocal"
                      step="60"
                    />
                  </div>
                </div>
                <div class="field">
                  <label class="field__label" for="end">종료 예정 시간</label>
                  <div class="field__control">
                    <input
                      id="end"
                      type="datetime-local"
                      v-model="form.endLocal"
                      :min="form.startLocal || minStartLocal"
                      step="60"
                      required
                    />
                  </div>
                </div>
              </div>
            </section>

            <!-- (C) 참여자 선택 -->
            <section class="section">
              <h4 class="section__title" id="participants-label">참여자 선택</h4>
              <div class="field__hint">본인은 자동 포함됩니다.</div>

              <div class="participants" role="group" aria-labelledby="participants-label">
                <div v-if="mLoading">팀원 목록 불러오는 중…</div>
                <template v-else>
                  <div class="participants-grid">
                    <!-- 스크립트 의존: form.participantIds 그대로 유지 -->
                    <label
                      v-for="m in members"
                      :key="m.userNo"
                      class="p-tile"
                      :class="{ 'is-me': m.userNo === myUserNo }"
                    >
                      <input
                        type="checkbox"
                        :value="m.userNo"
                        v-model="form.participantIds"
                        :disabled="m.userNo === myUserNo"
                      />
                      <span class="p-name">{{ m.name }}</span>
                      <span v-if="m.userNo === myUserNo" class="p-tag">나</span>
                    </label>
                  </div>

                  <div v-if="mError" class="field__error" style="white-space:pre-line">{{ mError }}</div>
                  <div v-if="!members.length && !mLoading" class="field__hint">팀원 없음</div>
                </template>
              </div>
            </section>
          </form>

          <!-- 서버 전체 오류(스크립트 그대로 활용) -->
          <p v-if="err" class="field__error" style="margin-top:8px; white-space:pre-line">{{ err }}</p>
        </div>

        <div class="modal__footer btn-group">
          <button class="btn" type="button" @click="close" :disabled="creating">취소</button>
          <button class="btn btn--primary" type="button" @click="submit" :disabled="creating || !canSubmit">
            {{ creating ? '생성 중…' : '생성' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

const props = defineProps({
  open:   { type: Boolean, default: false },
  teamNo: { type: Number,  required: true }
})
const emit = defineEmits(['update:open', 'created'])

const userStore = useUserStore()
const myUserNo = computed(() => Number(userStore.user?.userNo ?? 0))

const members   = ref([])          // [{ userNo:number, name:string }]
const mLoading  = ref(false)
const mError    = ref('')
const creating  = ref(false)
const err       = ref('')

const form = ref({
  title: '',
  startLocal: '',
  endLocal: '',
  participantIds: [] // number[]
})

// 지금 시각(분 단위) 이상만 선택 가능
const minStartLocal = computed(() => toLocalDT(floorToMinute(new Date())))

watch(() => props.open, (v) => {
  if (v) {
    initDefaults()
    loadMembersByTeam()
  }
})

// 시작 시간이 바뀌면 종료 시간이 앞설 수 없도록 보정
watch(() => form.value.startLocal, (v) => {
  if (!v) return
  if (!form.value.endLocal || new Date(form.value.endLocal).getTime() < new Date(v).getTime()) {
    // 기본 30분 뒤로 맞춤
    const base = new Date(v)
    const end  = new Date(base.getTime() + 30 * 60 * 1000)
    form.value.endLocal = toLocalDT(end)
  }
})

function initDefaults () {
  err.value = ''
  const now = floorToMinute(new Date())
  const end = new Date(now.getTime() + 30 * 60 * 1000)
  form.value = {
    title: '',
    startLocal: toLocalDT(now),
    endLocal: toLocalDT(end),
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

function floorToMinute(d) {
  const n = new Date(d)
  n.setSeconds(0, 0)
  return n
}
function pad(n){ return String(n).padStart(2,'0') }
function toLocalDT(d) {
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 서버가 UTC/ISO8601(타임존 포함)을 원하면 아래처럼 한 줄로 바꾸세요:
//   return new Date(localStr).toISOString();
function toISO(localStr) {
  return localStr || null; // 현재는 'YYYY-MM-DDTHH:mm' 그대로 보냄
}

function close () {
  emit('update:open', false)
}

// 유효성: 제목, 종료시간 존재 + 종료≥시작
const canSubmit = computed(() => {
  if (!form.value.title?.trim()) return false
  if (!form.value.endLocal) return false
  if (form.value.startLocal && form.value.endLocal) {
    const s = new Date(form.value.startLocal).getTime()
    const e = new Date(form.value.endLocal).getTime()
    if (Number.isFinite(s) && Number.isFinite(e) && e < s) return false
  }
  return true
})

async function submit () {
  err.value = ''

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