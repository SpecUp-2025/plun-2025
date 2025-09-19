<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="close">
      <div class="modal modal--solid prejoin-modal">
        <!-- 헤더 -->
        <div class="modal__header">
          <h3 class="modal__title">화상 회의 설정</h3>
          <button type="button" class="btn btn--secondary" @click="close" aria-label="닫기">✕</button>
        </div>

        <!-- 바디 -->
        <div class="modal__body">
          <!-- 로딩 상태 -->
          <div v-if="loading" class="prejoin__loading">권한 확인 중…</div>

          <!-- 에러 상태 -->
          <div v-else-if="error" class="prejoin__error">
            <div class="prejoin__error-text">{{ error }}</div>
            <div class="prejoin__row">
              <button class="btn" @click="retryPreview">재시도</button>
              <button class="btn" @click="close">닫기</button>
            </div>
          </div>

          <!-- 메인 설정 화면 -->
          <div v-else class="prejoin__grid">
            <!-- 좌측: 비디오 프리뷰 영역 -->
            <div class="prejoin__left">
              <div class="prejoin__meta">회의방 제목: {{ info?.title || '' }}</div>

              <!-- 비디오 프리뷰 -->
              <div class="prejoin__video">
                <video ref="videoEl" autoplay playsinline muted
                  :style="{ transform: mirror ? 'scaleX(-1)' : 'none' }"></video>

                <!-- 마이크 레벨 표시기 -->
                <div class="prejoin__vu">
                  <div class="prejoin__vu-bar" :style="{ width: vuLevel + '%' }"></div>
                  <span class="prejoin__vu-label">{{ vuLevel }}%</span>
                </div>
              </div>

              <!-- 미디어 컨트롤 버튼 -->
              <div class="prejoin__controls">
                <button class="btn" @click="toggleCam">{{ camOn ? '카메라 끄기' : '카메라 켜기' }}</button>
                <button class="btn" @click="toggleMic">{{ micOn ? '마이크 끄기' : '마이크 켜기' }}</button>
                <button class="btn" @click="retryPreview">재시도</button>
                <button class="btn" @click="testSpeaker">스피커 테스트</button>
                <audio ref="speakerEl"></audio>
              </div>
            </div>

            <!-- 우측: 장치 설정 패널 -->
            <div class="prejoin__right">
              <!-- 카메라 선택 -->
              <section class="prejoin__section">
                <label class="field__label">카메라</label>
                <div class="field__control">
                  <select v-model="selectedVideoId" @change="restartPreview">
                    <option v-for="d in videoInputs" :key="d.deviceId" :value="d.deviceId">
                      {{ d.label || 'Camera' }}
                    </option>
                  </select>
                </div>
              </section>

              <!-- 마이크 선택 -->
              <section class="prejoin__section">
                <label class="field__label">마이크</label>
                <div class="field__control">
                  <select v-model="selectedAudioId" @change="restartPreview">
                    <option v-for="d in audioInputs" :key="d.deviceId" :value="d.deviceId">
                      {{ d.label || 'Mic' }}
                    </option>
                  </select>
                </div>
              </section>

              <!-- 스피커 선택 (지원하는 브라우저만) -->
              <section class="prejoin__section" v-if="supportsSetSinkId">
                <label class="field__label">스피커</label>
                <div class="field__control">
                  <select v-model="selectedSpeakerId" @change="savePrefs">
                    <option value="">기본 출력</option>
                    <option v-for="d in audioOutputs" :key="d.deviceId" :value="d.deviceId">
                      {{ d.label || 'Speaker' }}
                    </option>
                  </select>
                </div>
              </section>

              <!-- 미러 모드 설정 -->
              <section class="prejoin__section prejoin__row">
                <label class="prejoin__check">
                  <input type="checkbox" v-model="mirror" @change="savePrefs" />
                  미러 모드
                </label>
              </section>

              <p v-if="!supportsSetSinkId" class="prejoin__note">
                ※ 일부 브라우저는 출력 장치 선택을 지원하지 않습니다.
              </p>
            </div>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="modal__footer prejoin__footer">
          <div class="prejoin__footer-left">
            <span v-if="!authorized" class="prejoin__warn">이 회의에 참여 권한이 없습니다.</span>
          </div>
          <div class="prejoin__footer-right">
            <button class="btn" @click="close" :disabled="entering">취소</button>
            <button class="btn btn--primary" @click="enter" :disabled="entering || !authorized">
              {{ entering ? '입장 중…' : '입장' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()

// Props & Emits
const props = defineProps({
  open: { type: Boolean, default: false },
  roomCode: { type: String, required: true },
  teamNo: { type: Number, required: false },
})
const emit = defineEmits(['update:open'])
const userNo = computed(() => Number(userStore.user?.userNo ?? 0))

// 모달 상태 관리
const loading = ref(true)
const entering = ref(false)
const error = ref('')
const info = ref(null) // 회의방 정보: { title, role, authorized }
const authorized = computed(() => !!info.value?.authorized)

// 미디어 스트림 및 DOM 요소 참조
const videoEl = ref(null)
const speakerEl = ref(null)
let stream = null
const camOn = ref(true)
const micOn = ref(true)

// 장치 목록 및 선택된 장치
const videoInputs = ref([])
const audioInputs = ref([])
const audioOutputs = ref([])
const selectedVideoId = ref(localStorage.getItem('pref.videoId') || '')
const selectedAudioId = ref(localStorage.getItem('pref.audioId') || '')
const selectedSpeakerId = ref(localStorage.getItem('pref.speakerId') || '')
const mirror = ref(localStorage.getItem('pref.mirror') === 'true')
const supportsSetSinkId = 'setSinkId' in HTMLMediaElement.prototype

// 마이크 레벨 측정을 위한 Web Audio API 관련 변수
let audioCtx = null
let analyser = null
let sourceNode = null
let vuRaf = 0
const vuLevel = ref(0) // 0~100% 마이크 레벨

// 라이프사이클 관리
onMounted(async () => { if (props.open) await init() })
watch(() => props.open, async (v) => { v ? await init() : cleanup() })
onBeforeUnmount(() => { cleanup() })

/**
 * 모달 초기화 - 권한 확인부터 프리뷰 시작까지
 */
async function init() {
  try {
    loading.value = true
    await fetchAuthz() // 회의방 참여 권한 확인
    loading.value = false
    
    if (authorized.value) {
      await nextTick()
      await ensurePermission() // 브라우저 미디어 권한 확인
      await listDevices() // 사용 가능한 미디어 장치 목록 조회
      setDefaultSelectionsIfEmpty() // 기본 장치 선택
      await startPreview() // 비디오/오디오 프리뷰 시작
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

/**
 * 모달 종료 시 리소스 정리
 */
function cleanup() {
  stopPreview()
  teardownAnalyser()
  navigator.mediaDevices?.removeEventListener?.('devicechange', onDeviceChange)
}

/**
 * 사용자 설정을 로컬스토리지에 저장
 */
function savePrefs() {
  localStorage.setItem('pref.videoId', selectedVideoId.value)
  localStorage.setItem('pref.audioId', selectedAudioId.value)
  localStorage.setItem('pref.speakerId', selectedSpeakerId.value)
  localStorage.setItem('pref.mirror', String(mirror.value))
  
  // 스피커 설정 즉시 반영
  if (speakerEl.value && typeof speakerEl.value.setSinkId === 'function' && selectedSpeakerId.value) {
    speakerEl.value.setSinkId(selectedSpeakerId.value).catch(() => {})
  }
}

/**
 * 브라우저 미디어 권한 사전 확인
 */
async function ensurePermission() {
  try { 
    await navigator.mediaDevices.getUserMedia({ video: true, audio: true }) 
  } catch {}
}

/**
 * 사용 가능한 미디어 장치 목록 조회
 */
async function listDevices() {
  const devices = await navigator.mediaDevices.enumerateDevices()
  videoInputs.value = devices.filter(d => d.kind === 'videoinput')
  audioInputs.value = devices.filter(d => d.kind === 'audioinput')
  audioOutputs.value = devices.filter(d => d.kind === 'audiooutput')
}

/**
 * 기본 장치가 선택되지 않은 경우 첫 번째 장치로 설정
 */
function setDefaultSelectionsIfEmpty() {
  if (!selectedVideoId.value && videoInputs.value[0]) {
    selectedVideoId.value = videoInputs.value[0].deviceId
  }
  if (!selectedAudioId.value && audioInputs.value[0]) {
    selectedAudioId.value = audioInputs.value[0].deviceId
  }
  savePrefs()
}

/**
 * 서버에서 회의방 참여 권한 확인
 */
async function fetchAuthz() {
  if (!userNo.value) throw new Error('로그인 정보가 없습니다.')
  const { data } = await instance.get(`/meeting-rooms/${props.roomCode}/authz`, {
    params: { userNo: userNo.value }
  })
  info.value = data
}

/**
 * 미디어 스트림 시작 및 비디오 프리뷰 표시
 */
async function startPreview() {
  stopPreview()
  teardownAnalyser()

  // 선택된 장치에 맞는 제약 조건 설정
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

    // 비디오 요소에 스트림 연결
    if (videoEl.value) {
      const v = videoEl.value
      v.srcObject = stream
      v.muted = true
      
      // 비디오 메타데이터 로드 대기
      await new Promise(resolve => {
        if (v.readyState >= 1) return resolve()
        const handler = () => { v.removeEventListener('loadedmetadata', handler); resolve() }
        v.addEventListener('loadedmetadata', handler)
      })
      
      try { await v.play() } catch {}
    }

    // 마이크 레벨 분석기 설정
    setupAnalyser(stream)
    error.value = ''
    savePrefs()
  } catch (e) {
    throw e
  }
}

/**
 * 현재 미디어 스트림 중지
 */
function stopPreview() {
  try { 
    stream?.getTracks()?.forEach(t => t.stop()) 
  } catch {}
  stream = null
}

/**
 * 프리뷰 재시작 (장치 변경 시 사용)
 */
async function restartPreview() {
  try {
    await startPreview()
  } catch (e) {
    await fallbackToDefaultDevices(e)
  }
}

/**
 * 장치 오류 시 기본 장치로 폴백
 */
async function fallbackToDefaultDevices(err) {
  const recoverable = ['NotReadableError', 'OverconstrainedError', 'NotFoundError', 'TrackStartError'].includes(err?.name)
  if (!recoverable) {
    error.value = prettyGumError(err)
    return
  }
  
  selectedVideoId.value = ''
  selectedAudioId.value = ''
  
  try {
    await startPreview()
    await listDevices()
    setDefaultSelectionsIfEmpty()
    error.value = '선택한 장치를 사용할 수 없어 기본 장치로 전환했습니다.'
  } catch (e2) {
    error.value = prettyGumError(e2)
  }
}

/**
 * 마이크 레벨 측정을 위한 Web Audio API 분석기 설정
 */
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
    // 분석기 설정 실패는 치명적이지 않으므로 무시
  }
}

/**
 * 오디오 분석기 리소스 정리
 */
function teardownAnalyser() {
  if (vuRaf) cancelAnimationFrame(vuRaf)
  vuRaf = 0
  try { sourceNode?.disconnect() } catch {}
  try { analyser?.disconnect() } catch {}
  sourceNode = null
  analyser = null
}

/**
 * 마이크 레벨 측정 및 표시 루프
 */
function startVuLoop() {
  const data = new Uint8Array(analyser.frequencyBinCount)
  const loop = () => {
    if (!analyser) return
    analyser.getByteTimeDomainData(data)
    
    // RMS(Root Mean Square) 계산으로 음성 레벨 측정
    let sum = 0
    for (let i = 0; i < data.length; i++) {
      const val = (data[i] - 128) / 128 // -1 ~ 1 정규화
      sum += val * val
    }
    const rms = Math.sqrt(sum / data.length)
    const pct = Math.min(100, Math.max(0, Math.round(rms * 140))) // 0~100% 변환
    vuLevel.value = pct
    vuRaf = requestAnimationFrame(loop)
  }
  vuRaf = requestAnimationFrame(loop)
}

/**
 * 스피커 테스트 - 440Hz 사인파 0.6초 재생
 */
async function testSpeaker() {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)()
    if (audioCtx.state === 'suspended') await audioCtx.resume()

    // Web Audio로 테스트 톤 생성
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    const dest = audioCtx.createMediaStreamDestination()

    osc.type = 'sine'
    osc.frequency.value = 440 // A4 음
    gain.gain.value = 0.001

    osc.connect(gain)
    gain.connect(dest)

    // 페이드 인/아웃 효과
    const now = audioCtx.currentTime
    gain.gain.setTargetAtTime(0.2, now, 0.02)
    gain.gain.setTargetAtTime(0.0001, now + 0.45, 0.05)

    // 선택된 스피커로 재생
    const el = speakerEl.value
    el.srcObject = dest.stream
    el.volume = 1.0
    
    if (supportsSetSinkId && selectedSpeakerId.value) {
      try {
        await el.setSinkId(selectedSpeakerId.value)
      } catch (e) {
        // setSinkId 실패해도 기본 출력으로 진행
      }
    }

    osc.start()
    await el.play()
    osc.stop(audioCtx.currentTime + 0.6)
  } catch (e) {
    error.value = '스피커 테스트 실패: ' + (e?.message || String(e))
  }
}

/**
 * 장치 변경 감지 시 처리
 */
async function onDeviceChange() {
  await listDevices()
  
  // 현재 선택된 장치가 사라진 경우 초기화
  const vids = videoInputs.value.map(d => d.deviceId)
  const aids = audioInputs.value.map(d => d.deviceId)
  if (selectedVideoId.value && !vids.includes(selectedVideoId.value)) selectedVideoId.value = ''
  if (selectedAudioId.value && !aids.includes(selectedAudioId.value)) selectedAudioId.value = ''
  
  setDefaultSelectionsIfEmpty()
  await restartPreview()
}

/**
 * 카메라 온/오프 토글
 */
function toggleCam() {
  camOn.value = !camOn.value
  stream?.getVideoTracks()?.forEach(t => (t.enabled = camOn.value))
}

/**
 * 마이크 온/오프 토글
 */
function toggleMic() {
  micOn.value = !micOn.value
  stream?.getAudioTracks()?.forEach(t => (t.enabled = micOn.value))
}

/**
 * 프리뷰 재시도
 */
async function retryPreview() {
  error.value = ''
  try {
    await restartPreview()
  } catch (e) {
    await fallbackToDefaultDevices(e)
  }
}

/**
 * getUserMedia 오류를 사용자 친화적 메시지로 변환
 */
function prettyGumError(e) {
  const name = e?.name || ''
  const msg = e?.message || String(e)
  
  if (name === 'NotAllowedError' || name === 'SecurityError') {
    return '카메라/마이크 권한이 거부되었습니다.\n브라우저 주소창의 카메라 아이콘에서 "허용"으로 변경 후 다시 시도하세요.'
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

/**
 * 모달 닫기
 */
function close() { 
  emit('update:open', false) 
}

/**
 * 회의방 입장 - 새 창에서 회의방 열기
 */
async function enter() {
  if (!authorized.value) return
  entering.value = true
  
  try {
    const meetingUrl = window.location.origin + router.resolve({
      name: 'MeetingRoom',
      params: { roomCode: props.roomCode }
    }).href
    
    const newWindow = window.open(
      meetingUrl,
      'meeting_' + props.roomCode,
      'width=1400,height=900,resizable=yes,scrollbars=yes'
    )
    
    if (newWindow) {
      emit('update:open', false)
    } else {
      alert('팝업이 차단되었습니다. 브라우저 설정에서 팝업을 허용해주세요.')
      // 팝업 차단 시 같은 탭에서 열기
      await router.push({
        name: 'MeetingRoom',
        params: { roomCode: props.roomCode }
      })
      emit('update:open', false)
    }
  } catch (e) {
    console.error('Failed to open meeting room:', e)
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