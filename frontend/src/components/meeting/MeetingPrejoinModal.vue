<!-- src/components/meeting/MeetingPrejoinModal.vue -->
<template>
  <div v-if="open" class="overlay" @click.self="close">
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
        <div class="meta">방 제목: {{ info ? info.title : '' }} / 내 역할: {{ info ? info.role : '' }}</div>

        <!-- 장치 선택 -->
        <div class="row wrap">
          <label>카메라
            <select v-model="selectedVideoId" @change="restartPreview">
              <option v-for="d in videoInputs" :key="d.deviceId" :value="d.deviceId">
                {{ d.label || 'Camera' }}
              </option>
            </select>
          </label>

          <label>마이크
            <select v-model="selectedAudioId" @change="restartPreview">
              <option v-for="d in audioInputs" :key="d.deviceId" :value="d.deviceId">
                {{ d.label || 'Mic' }}
              </option>
            </select>
          </label>

          <label v-if="supportsSetSinkId" class="speaker">
            스피커
            <select v-model="selectedSpeakerId" @change="savePrefs">
              <option value="">기본 출력</option>
              <option v-for="d in audioOutputs" :key="d.deviceId" :value="d.deviceId">
                {{ d.label || 'Speaker' }}
              </option>
            </select>
          </label>

          <label class="mirror">
            <input type="checkbox" v-model="mirror" @change="savePrefs"> 미러
          </label>
        </div>

        <div class="grid">
          <div class="preview">
            <!-- 카메라 프리뷰 (좌우 반전 인라인 스타일) -->
            <video ref="videoEl" autoplay playsinline muted
              :style="{ transform: mirror ? 'scaleX(-1)' : 'none' }"></video>

            <!-- 마이크 레벨 미터 -->
            <div class="vu-wrap">
              <div class="vu-bar" :style="{ width: vuLevel + '%' }"></div>
              <span class="vu-label">{{ vuLevel }}%</span>
            </div>

            <div class="row">
              <button @click="toggleCam">{{ camOn ? '카메라 끄기' : '카메라 켜기' }}</button>
              <button @click="toggleMic">{{ micOn ? '마이크 끄기' : '마이크 켜기' }}</button>
              <button @click="retryPreview">재시도</button>
              <button @click="testSpeaker">스피커 테스트</button>
              <audio ref="speakerEl"></audio>
            </div>
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
          <div v-if="!supportsSetSinkId" class="note">※ 일부 브라우저는 출력 장치 선택을 지원하지 않습니다.</div>
        </div>
      </div>
    </div>

    <button class="x" @click="close">×</button>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()

const props = defineProps({
  open: { type: Boolean, default: false },
  roomCode: { type: String, required: true },
  teamNo: { type: Number, required: false },
})
const emit = defineEmits(['update:open'])
const userNo = computed(() => Number(userStore.user?.userNo ?? 0))

const loading = ref(true)
const entering = ref(false)
const error = ref('')
const info = ref(null) // { title, role, authorized }
const authorized = computed(() => !!info.value?.authorized)

const videoEl = ref(null)
const speakerEl = ref(null)
let stream = null
const camOn = ref(true)
const micOn = ref(true)

/* 장치 상태 */
const videoInputs = ref([])     // videoinput
const audioInputs = ref([])     // audioinput
const audioOutputs = ref([])    // audiooutput
const selectedVideoId = ref(localStorage.getItem('pref.videoId') || '')
const selectedAudioId = ref(localStorage.getItem('pref.audioId') || '')
const selectedSpeakerId = ref(localStorage.getItem('pref.speakerId') || '')
const mirror = ref(localStorage.getItem('pref.mirror') === 'true')
const supportsSetSinkId = 'setSinkId' in HTMLMediaElement.prototype

/* 마이크 레벨미터 */
let audioCtx = null
let analyser = null
let sourceNode = null
let vuRaf = 0
const vuLevel = ref(0) // 0~100 %

onMounted(async () => { if (props.open) await init() })
watch(() => props.open, async (v) => { v ? await init() : cleanup() })

onBeforeUnmount(() => { cleanup() })
 
async function init () {
  try {
    loading.value = true
    await fetchAuthz()
    loading.value = false
    if (authorized.value) {
      await nextTick()
      await ensurePermission()
      await listDevices()
      setDefaultSelectionsIfEmpty()
      await startPreview()
      navigator.mediaDevices?.addEventListener?.('devicechange', onDeviceChange)
    } else {
      stopPreview()
      teardownAnalyser()
    }
  } catch (e) {
    loading.value = false
    error.value = e?.response?.data?.message || e.message || String(e)
  }
}

function cleanup () {
  stopPreview()
  teardownAnalyser()
  navigator.mediaDevices?.removeEventListener?.('devicechange', onDeviceChange)
}


function savePrefs() {
  localStorage.setItem('pref.videoId', selectedVideoId.value)
  localStorage.setItem('pref.audioId', selectedAudioId.value)
  localStorage.setItem('pref.speakerId', selectedSpeakerId.value)
  localStorage.setItem('pref.mirror', String(mirror.value))
  // 테스트 오디오 엘리먼트에도 즉시 반영 (지원 브라우저 한정)
  if (speakerEl.value && typeof speakerEl.value.setSinkId === 'function' && selectedSpeakerId.value) {
    speakerEl.value.setSinkId(selectedSpeakerId.value).catch(() => { })
  }
}

async function ensurePermission() {
  try { await navigator.mediaDevices.getUserMedia({ video: true, audio: true }) } catch { }
}

async function listDevices() {
  const devices = await navigator.mediaDevices.enumerateDevices()
  videoInputs.value = devices.filter(d => d.kind === 'videoinput')
  audioInputs.value = devices.filter(d => d.kind === 'audioinput')
  audioOutputs.value = devices.filter(d => d.kind === 'audiooutput') // 크롬/엣지 등
}

function setDefaultSelectionsIfEmpty() {
  if (!selectedVideoId.value && videoInputs.value[0]) {
    selectedVideoId.value = videoInputs.value[0].deviceId
  }
  if (!selectedAudioId.value && audioInputs.value[0]) {
    selectedAudioId.value = audioInputs.value[0].deviceId
  }
  // 스피커는 기본값 ''(시스템 기본 출력) 그대로도 OK
  savePrefs()
}

async function fetchAuthz() {
  if (!userNo.value) throw new Error('로그인 정보가 없습니다.')
  const { data } = await instance.get(`/meeting-rooms/${props.roomCode}/authz`, {
    params: { userNo: userNo.value }
  })
  info.value = data
}

/*  프리뷰  */
async function startPreview() {
  stopPreview()
  teardownAnalyser()

  const constraints = {
    video: selectedVideoId.value
      ? { deviceId: { exact: selectedVideoId.value }, width: { ideal: 1280 }, height: { ideal: 720 } }
      : { width: { ideal: 1280 }, height: { ideal: 720 } },
    audio: selectedAudioId.value
      ? { deviceId: { exact: selectedAudioId.value } }
      : true
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia(constraints)

    if (videoEl.value) {
      const v = videoEl.value
      v.srcObject = stream
      v.muted = true
      await new Promise(resolve => {
        if (v.readyState >= 1) return resolve()
        const handler = () => { v.removeEventListener('loadedmetadata', handler); resolve() }
        v.addEventListener('loadedmetadata', handler)
      })
      try { await v.play() } catch { }
    }

    // 마이크 레벨 분석기 세팅
    setupAnalyser(stream)

    error.value = ''     // 성공
    savePrefs()          // 성공한 선택만 저장
  } catch (e) {
    console.error('getUserMedia failed:', e)
    throw e
  }
}

function stopPreview() {
  try { stream?.getTracks()?.forEach(t => t.stop()) } catch { }
  stream = null
}

async function restartPreview() {
  try {
    await startPreview()
  } catch (e) {
    await fallbackToDefaultDevices(e)
  }
}

async function fallbackToDefaultDevices(err) {
  const recoverable = ['NotReadableError', 'OverconstrainedError', 'NotFoundError', 'TrackStartError'].includes(err?.name)
  if (!recoverable) {
    error.value = prettyGumError(err)
    return
  }
  selectedVideoId.value = ''
  selectedAudioId.value = ''
  try {
    await startPreview()      // 기본 제약으로 시도
    await listDevices()
    setDefaultSelectionsIfEmpty()
    error.value = '선택한 장치를 사용할 수 없어 기본 장치로 전환했습니다.'
  } catch (e2) {
    error.value = prettyGumError(e2)
  }
}

/*  마이크 레벨미터(Web Audio)  */
function setupAnalyser(mediaStream) {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)()
    sourceNode = audioCtx.createMediaStreamSource(mediaStream)
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 256
    analyser.smoothingTimeConstant = 0.8
    sourceNode.connect(analyser)
    startVuLoop()
  } catch (e) {
    console.debug('analyser setup failed:', e)
  }
}

function teardownAnalyser() {
  if (vuRaf) cancelAnimationFrame(vuRaf)
  vuRaf = 0
  try { sourceNode?.disconnect(); } catch { }
  try { analyser?.disconnect(); } catch { }
  sourceNode = null
  analyser = null
  // audioCtx는 재사용
}

function startVuLoop() {
  const data = new Uint8Array(analyser.frequencyBinCount)
  const loop = () => {
    if (!analyser) return
    analyser.getByteTimeDomainData(data)
    // RMS 근사 0~100%
    let sum = 0
    for (let i = 0; i < data.length; i++) {
      const val = (data[i] - 128) / 128 // -1 ~ 1
      sum += val * val
    }
    const rms = Math.sqrt(sum / data.length)      // 0~1
    const pct = Math.min(100, Math.max(0, Math.round(rms * 140))) // 감도 약간 보정
    vuLevel.value = pct
    vuRaf = requestAnimationFrame(loop)
  }
  vuRaf = requestAnimationFrame(loop)
}

/* 스피커 테스트 */
async function testSpeaker() {
  try {
    // 테스트 톤을 WebAudio로 생성 - MediaStreamDestination - audio로 재생
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)()
    if (audioCtx.state === 'suspended') await audioCtx.resume()

    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    const dest = audioCtx.createMediaStreamDestination()

    osc.type = 'sine'
    osc.frequency.value = 440
    gain.gain.value = 0.001   // 시작은 아주 작게

    osc.connect(gain)
    gain.connect(dest)

    // fade-in/out
    const now = audioCtx.currentTime
    gain.gain.setTargetAtTime(0.2, now, 0.02)
    gain.gain.setTargetAtTime(0.0001, now + 0.45, 0.05)

    // audio로 출력(장치 선택 지원 시 setSinkId 적용)
    const el = speakerEl.value
    el.srcObject = dest.stream
    el.volume = 1.0
    if (supportsSetSinkId && selectedSpeakerId.value) {
      try {
        // setSinkId는 사용자 제스처 컨텍스트 내 호출 필요 → 버튼 클릭로 만족
        await el.setSinkId(selectedSpeakerId.value)
      } catch (e) {
        console.debug('setSinkId failed:', e)
        // 실패해도 기본 출력으로 진행
      }
    }

    osc.start()
    await el.play()
    osc.stop(audioCtx.currentTime + 0.6) // 0.6초 톤
  } catch (e) {
    error.value = '스피커 테스트 실패: ' + (e?.message || String(e))
  }
}

/* 기타 이벤트 */
async function onDeviceChange() {
  await listDevices()
  const vids = videoInputs.value.map(d => d.deviceId)
  const aids = audioInputs.value.map(d => d.deviceId)
  if (selectedVideoId.value && !vids.includes(selectedVideoId.value)) selectedVideoId.value = ''
  if (selectedAudioId.value && !aids.includes(selectedAudioId.value)) selectedAudioId.value = ''
  setDefaultSelectionsIfEmpty()
  await restartPreview()
}

function toggleCam() {
  camOn.value = !camOn.value
  stream?.getVideoTracks()?.forEach(t => (t.enabled = camOn.value))
}
function toggleMic() {
  micOn.value = !micOn.value
  stream?.getAudioTracks()?.forEach(t => (t.enabled = micOn.value))
}

async function retryPreview() {
  error.value = ''
  try {
    await restartPreview()
  } catch (e) {
    await fallbackToDefaultDevices(e)
  }
}

function prettyGumError(e) {
  const name = e?.name || ''
  const msg = e?.message || String(e)
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

function close() { emit('update:open', false) }

async function enter() {
  if (!authorized.value) return
  entering.value = true
  try {
    await router.push({ name: 'MeetingRoom', params: { roomCode: props.roomCode } })
    emit('update:open', false)
  } finally {
    entering.value = false
  }
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .4);
  display: grid;
  place-items: center;
  z-index: 40;
}

.modal {
  position: relative;
  background: #fff;
  width: min(920px, 96vw);
  padding: 16px;
  border-radius: 12px;
}

.x {
  position: absolute;
  top: 8px;
  right: 8px;
  border: 0;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
}

.err {
  color: #d33;
  white-space: pre-line;
}

.warn {
  color: #d33;
  margin-top: 8px;
}

.note {
  color: #666;
  margin-top: 6px;
  font-size: 12px;
}

.grid {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr 320px;
}

.preview video {
  width: 100%;
  aspect-ratio: 16/9;
  background: #000;
  border-radius: 8px;
}

.vu-wrap {
  position: relative;
  height: 12px;
  background: #eee;
  border-radius: 6px;
  margin: 8px 0;
  overflow: hidden;
}

.vu-bar {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 0%;
  background: #22c55e;
  transition: width .05s linear;
}

.vu-label {
  position: absolute;
  right: 6px;
  top: -20px;
  font-size: 12px;
  color: #444;
}

.row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.row.wrap {
  flex-wrap: wrap;
  gap: 12px;
}

.meta {
  margin-bottom: 8px;
}

.mirror {
  user-select: none;
}

@media (max-width: 880px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
