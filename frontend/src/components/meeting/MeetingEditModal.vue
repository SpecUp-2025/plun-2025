<!-- src/components/meeting/MeetingEditModal.vue -->
<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="close">
      <div class="modal modal--solid meeting-edit-modal">
        <!-- 헤더 -->
        <div class="modal__header">
          <h3 class="modal__title">회의 수정</h3>
          <button class="btn btn--secondary" type="button" @click="close">✕</button>
        </div>

        <!-- 바디 -->
        <div class="modal__body">
          <form class="form" @submit.prevent="submit" novalidate>
            <!-- (A) 회의명 -->
            <section class="section">
              <label class="field__label field__label--lg" for="title">회의명</label>
              <div class="field__control">
                <input id="title" v-model="form.title" type="text" placeholder="예: 주간 회의" />
              </div>
              <p class="field__error" v-if="tried && !form.title.trim()">제목을 입력해 주세요.</p>
            </section>

            <!-- (B) 회의 예정 시간 -->
            <section class="section">
              <h4 class="section__title">회의 예정 시간</h4>
              <div class="grid-2">
                <div class="field">
                  <label class="field__label" for="start">시작 예정 시간</label>
                  <div class="field__control">
                    <input id="start" type="datetime-local" v-model="form.startLocal" :min="minStartLocal" step="60" />
                  </div>
                </div>
                <div class="field">
                  <label class="field__label" for="end">종료 예정 시간</label>
                  <div class="field__control">
                    <input id="end" type="datetime-local" v-model="form.endLocal"
                      :min="form.startLocal || minStartLocal" step="60" required />
                  </div>
                  <p class="field__error" v-if="tried && timeInvalid">종료 시간이 시작보다 빠를 수 없습니다.</p>
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
                    <label v-for="m in members" :key="m.userNo" class="p-tile"
                      :class="{ 'is-me': m.userNo === myUserNo }">
                      <input type="checkbox" :value="m.userNo" v-model="form.participantUserNos"
                        :disabled="m.userNo === myUserNo" />
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

          <p v-if="err" class="field__error" style="margin-top:8px; white-space:pre-line">{{ err }}</p>
        </div>

        <!-- 푸터 -->
        <div class="modal__footer btn-group">
          <button class="btn" type="button" @click="close" :disabled="saving || deleting">취소</button>
          <div class="spacer"></div>
          <button class="btn btn--danger" type="button" @click="confirmDelete" :disabled="saving || deleting">
            {{ deleting ? '삭제 중…' : '삭제' }}
          </button>
          <button class="btn btn--primary" type="button" @click="submit" :disabled="saving || !canSubmit">
            {{ saving ? '저장 중…' : '저장' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed, onUnmounted } from 'vue'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

/**
 * 사용법:
 * <MeetingEditModal
 *   :open="editOpen"
 *   :detail="selectedMeeting"     // { roomNo, teamNo?, title, scheduledTime, scheduledEndTime, participants: [{userNo,name}, ...] }
 *   :team-no="teamNo"             // (선택) detail.teamNo 없을 때 사용
 *   @update:open="v => editOpen = v"
 *   @updated="handleUpdated"
 *   @deleted="handleDeleted"
 * />
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  detail: { type: Object, required: true },
  teamNo: { type: Number, default: null }  // optional
})
const emit = defineEmits(['update:open', 'updated', 'deleted'])

const userStore = useUserStore()
const myUserNo = computed(() => Number(userStore.user?.userNo ?? 0))

const saving = ref(false)
const deleting = ref(false)
const err = ref('')
const tried = ref(false)

const members = ref([])      // [{ userNo, name }]
const mLoading = ref(false)
const mError = ref('')

const form = ref({
  title: '',
  startLocal: '',
  endLocal: '',
  participantUserNos: []
})

const roomNo = computed(() => Number(props.detail?.roomNo ?? 0))
const effTeamNo = computed(() => Number(props.teamNo ?? props.detail?.teamNo ?? 0))

// 오늘(분단위) 이상의 값만 선택
const minStartLocal = computed(() => toLocalInput(floorToMinute(new Date())))

/* 오픈 시 초기화 + 팀원 로딩 */
watch(() => props.open, async (v) => {
  if (v) {
    document.body.classList.add('modal-open')
    initFromDetail()
    await loadMembersByTeam()
  } else {
    document.body.classList.remove('modal-open')
  }
})
onUnmounted(() => document.body.classList.remove('modal-open'))

// 시작 변경 시 종료 최소 보정(+30분)
watch(() => form.value.startLocal, (v) => {
  if (!v) return
  if (!form.value.endLocal || new Date(form.value.endLocal).getTime() < new Date(v).getTime()) {
    const base = new Date(v)
    const end = new Date(base.getTime() + 30 * 60 * 1000)
    form.value.endLocal = toLocalInput(end)
  }
})

function initFromDetail() {
  err.value = ''
  tried.value = false

  const start = props.detail?.scheduledTime
  const end = props.detail?.scheduledEndTime

  // 기본값: 기존 값이 없으면 지금~30분 후
  const now = floorToMinute(new Date())
  const defEnd = new Date(now.getTime() + 30 * 60 * 1000)

  // 참여자: 상세의 participants에서 userNo 추출
  const initial = Array.from(new Set(
    (props.detail?.participants ?? [])
      .map(p => Number(p?.userNo))
      .filter(n => Number.isFinite(n))
  ))
  if (myUserNo.value) {
    // 본인 강제 포함
    if (!initial.includes(myUserNo.value)) initial.push(myUserNo.value)
  }

  form.value = {
    title: props.detail?.title ?? '',
    startLocal: start ? toLocalInput(new Date(start)) : toLocalInput(now),
    endLocal: end ? toLocalInput(new Date(end)) : toLocalInput(defEnd),
    participantUserNos: initial
  }
}

async function loadMembersByTeam() {
  mLoading.value = true
  mError.value = ''
  members.value = []
  try {
    if (!effTeamNo.value) {
      throw new Error('팀 번호를 확인할 수 없습니다.')
    }
    const { data } = await instance.get(`/teams/${effTeamNo.value}/members`)
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
function pad(n) { return String(n).padStart(2, '0') }
function toLocalInput(d) { // Date -> 'YYYY-MM-DDTHH:mm'
  const dt = (d instanceof Date) ? d : new Date(d)
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}
function toLocalMinute(localStr) { // 'YYYY-MM-DDTHH:mm...' -> 'YYYY-MM-DDTHH:mm'
  if (!localStr) return null
  return localStr.replace(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}).*$/, '$1')
}

// 유효성
const timeInvalid = computed(() => {
  if (!form.value.startLocal || !form.value.endLocal) return false
  const s = new Date(form.value.startLocal).getTime()
  const e = new Date(form.value.endLocal).getTime()
  return Number.isFinite(s) && Number.isFinite(e) && e < s
})
const canSubmit = computed(() => {
  if (!form.value.title?.trim()) return false
  if (!form.value.startLocal || !form.value.endLocal) return false
  if (timeInvalid.value) return false
  if (!roomNo.value) return false
  return true
})

function close() { emit('update:open', false) }

async function submit() {
  tried.value = true
  err.value = ''
  if (!canSubmit.value) return
  if (!roomNo.value) {
    err.value = 'roomNo를 확인할 수 없습니다.'
    return
  }

  // 숫자화 + 중복제거 + 본인 강제 포함
  const set = new Set(
    (form.value.participantUserNos || [])
      .map(n => Number(n))
      .filter(n => Number.isFinite(n))
  )
  if (myUserNo.value) set.add(myUserNo.value)
  const participantUserNos = Array.from(set)

  const payload = {
    title: form.value.title.trim(),
    scheduledStartTime: toLocalMinute(form.value.startLocal),
    scheduledEndTime: toLocalMinute(form.value.endLocal),
    participantUserNos
  }

  saving.value = true
  try {
    const { data } = await instance.patch(`/meeting-rooms/${roomNo.value}`, payload, {
      params: { userNo: myUserNo.value }
    })
    emit('updated', data ?? payload)
    close()
  } catch (e) {
    err.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  err.value = ''
  if (!roomNo.value) {
    err.value = 'roomNo를 확인할 수 없습니다.'
    return
  }
  const ok = window.confirm('회의를 삭제할까요?\n(달력 일정도 함께 정리됩니다)')
  if (!ok) return

  deleting.value = true
  try {
    await instance.delete(`/meeting-rooms/${roomNo.value}`, {
      params: { userNo: myUserNo.value }
    })
    emit('deleted', { roomNo: roomNo.value })
    close()
  } catch (e) {
    err.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    deleting.value = false
  }
}
</script>
