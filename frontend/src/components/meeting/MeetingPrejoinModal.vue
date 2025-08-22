<!-- src/components/meeting/MeetingPrejoinModal.vue -->
<template>
  <div class="overlay" @click.self="close">
    <div class="modal">
      <h3>프리조인</h3>

      <div v-if="loading">권한 확인 중…</div>
      <div v-else-if="error" class="err">
        {{ error }}
        <div class="row" style="margin-top:8px">
          <button @click="retryPreview">재시도</button>
          <button @click="close">닫기</button>
        </div>
      </div>

      <div v-else>
        <div class="meta">방 제목: {{ info?.title }} / 내 역할: {{ info?.role }}</div>

        <div class="grid">
          <div class="preview">
            <!-- 🔑 자동재생 정책 회피: muted 반드시 명시 -->
            <video ref="videoEl" autoplay playsinline muted></video>

            <div class="row">
              <button @click="toggleCam">{{ camOn ? '카메라 끄기' : '카메라 켜기' }}</button>
              <button @click="toggleMic">{{ micOn ? '마이크 끄기' : '마이크 켜기' }}</button>
              <button @click="retryPreview">재시도</button>
            </div>
          </div>

          <div class="actions">
            <div>카메라/마이크 확인 후 입장하세요.</div>
            <div class="row">
              <button @click="enter" :disabled="entering || !authorized">
                {{ entering ? '입장 중…' : '입장' }}
              </button>
              <button @click="close" :disabled="entering">취소</button>
            </div>
            <div v-if="!authorized" class="warn">이 회의에 참여 권한이 없습니다.</div>
          </div>
        </div>
      </div>

      <button class="x" @click="close">×</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roomCode = route.params.roomCode
const userNo = computed(() => Number(userStore.user?.userNo ?? 0))

const loading = ref(true)
const entering = ref(false)
const error = ref('')
const info = ref(null) // { title, role, authorized }
const authorized = computed(() => !!info?.value?.authorized)

const videoEl = ref(null)
let stream = null
const camOn = ref(true)
const micOn = ref(true)

onMounted(async () => {
  try {
    await fetchAuthz()
    if (authorized.value) await startPreview()
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || String(e)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => stopPreview())

async function fetchAuthz () {
  if (!userNo.value) throw new Error('로그인 정보가 없습니다.')
  const { data } = await instance.get(`/meeting-rooms/${roomCode}/authz`, {
    params: { userNo: userNo.value }
  })
  info.value = data
}

async function startPreview () {
  stopPreview() // 이전 스트림 정리

  // 필요하면 해상도 힌트만 (과한 제약은 실패 유발 가능)
  const constraints = {
    video: { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: 'user' },
    audio: true
  }

  try {
    stream = await navigator.mediaDevices.getUserMedia(constraints)

    if (videoEl.value) {
      const v = videoEl.value
      v.srcObject = stream
      v.muted = true // 프리뷰는 항상 muted가 안전

      // 메타데이터 로드 후 play() (자동재생 정책/타이밍 이슈 방지)
      await new Promise(resolve => {
        if (v.readyState >= 1) return resolve()
        const handler = () => { v.removeEventListener('loadedmetadata', handler); resolve() }
        v.addEventListener('loadedmetadata', handler)
      })

      try {
        await v.play()
      } catch (e) {
        // 일부 환경에서 play()가 거부될 수 있음 → 재시도 버튼으로 복구
        console.debug('video.play() blocked:', e)
      }
    }
    error.value = '' // 성공했으면 에러 클리어
  } catch (e) {
    console.error('getUserMedia failed:', e)
    error.value = prettyGumError(e)
  }
}

function stopPreview () {
  try { stream?.getTracks()?.forEach(t => t.stop()) } catch {}
  stream = null
}

function toggleCam () {
  camOn.value = !camOn.value
  stream?.getVideoTracks()?.forEach(t => (t.enabled = camOn.value))
}
function toggleMic () {
  micOn.value = !micOn.value
  stream?.getAudioTracks()?.forEach(t => (t.enabled = micOn.value))
}

async function retryPreview () {
  error.value = ''
  await startPreview()
}

function prettyGumError (e) {
  const name = e?.name || ''
  const msg  = e?.message || String(e)
  if (name === 'NotAllowedError' || name === 'SecurityError') {
    return '카메라/마이크 권한이 거부되었습니다.\n브라우저 주소창의 카메라 아이콘에서 “허용”으로 변경 후 다시 시도하세요.'
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return '연결된 카메라/마이크를 찾을 수 없습니다.\n장치를 연결하거나 다른 장치를 선택해 주세요.'
  }
  if (name === 'NotReadableError' || name === 'TrackStartError') {
    return '다른 프로그램(Zoom/Teams/OBS 등)이 장치를 사용 중입니다.\n해당 프로그램을 종료하고 다시 시도하세요.'
  }
  if (name === 'OverconstrainedError') {
    return '요청한 장치 제약 조건을 만족할 수 없습니다.\n해상도를 낮추거나 기본 설정으로 재시도하세요.'
  }
  return `미디어 장치 초기화에 실패했습니다.\n(${name || 'Error'}: ${msg})`
}

function close () { router.back() } // 뒤로가기 = 모달 닫기

async function enter () {
  if (!authorized.value) return
  entering.value = true
  try {
    await router.push({ name: 'MeetingRoom', params: { roomCode } })
  } finally {
    entering.value = false
  }
}
</script>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: grid; place-items: center; z-index: 40; }
.modal { position: relative; background: #fff; width: min(920px, 96vw); padding: 16px; border-radius: 12px; }
.x { position: absolute; top: 8px; right: 8px; border: 0; background: transparent; font-size: 20px; cursor: pointer; }
.err { color: #d33; white-space: pre-line; }
.warn { color: #d33; margin-top: 8px; }
.grid { display: grid; gap: 12px; grid-template-columns: 1fr 320px; }
.preview video { width: 100%; aspect-ratio: 16/9; background: #000; border-radius: 8px; }
.row { display: flex; gap: 8px; margin-top: 8px; }
.meta { margin-bottom: 8px; }
@media (max-width: 880px) {
  .grid { grid-template-columns: 1fr; }
}
</style>
