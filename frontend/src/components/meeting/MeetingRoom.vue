<!-- src/components/meeting/MeetingRoom.vue -->
<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore'

// Components
import VideoGrid from './components/VideoGrid.vue'
import ControlsBar from './components/ControlsBar.vue'

// Composables
import { useRecording } from './composables/useRecording'
import { useMediaControls } from './composables/useMediaControls'
import { useSfu } from './composables/useSfu'

/* ===== 기본 설정 ===== */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roomCode = computed(() => String(route.params.roomCode || ''))
const myName = computed(() => userStore.user?.name || '나')
const myUserNo = computed(() => Number(userStore.user?.userNo ?? 0))

/* ===== 상태 관리 ===== */
const loading = ref(true)
const error = ref('')
const info = ref(null) // 회의방 정보 및 권한
const authorized = computed(() => !!info.value?.authorized)
const roomTitle = computed(() => info.value?.title || '회의')
const isHost = computed(() => String(info.value?.role || '').trim().toUpperCase() === 'C001')

/* ===== 로컬 미디어 ===== */
const localVideoRef = ref(null)
const localStream = ref(null)

function setLocalVideoRef(el) {
  localVideoRef.value = el
}

/* ===== Composables 초기화 ===== */
// 미디어 장치 제어 (카메라/마이크/스피커)
const { 
  mirror, 
  speakerOn, 
  micOn, 
  camOn, 
  toggleMic, 
  toggleCam, 
  toggleSpeaker, 
  getLocalStream,
  stopLocalStream 
} = useMediaControls()

// SFU 서버 연결 및 피어 관리
const {
  sfu,
  peers,
  consumed,
  connectSfu,
  updateSpeakerState,
  disconnect: disconnectSfu
} = useSfu(roomCode, myName, myUserNo, localStream, speakerOn)

// 녹음 기능 (호스트 전용)
const {
  recordingState,
  recordingDuration,
  isRecording,
  isPaused,
  isProcessing,
  isIdle,
  startRecording,
  pauseRecording,
  resumeRecording,
  stopRecording,
  formatDuration,
  cleanup: cleanupRecording
} = useRecording(roomCode, info, peers, localStream)

/* ===== 참가자 목록 계산 ===== */
const allParticipants = computed(() => {
  const participants = []

  // 나 자신 추가
  participants.push({
    id: 'self',
    name: myName.value,
    isSelf: true,
    hasVideo: camOn.value,
    hasAudio: micOn.value,
    videoStream: localStream.value
  })

  // 원격 참가자들 추가
  for (const [socketId, peer] of peers) {
    let hasVideo = false

    // 비디오 트랙 상태 확인
    if (peer.videoStream && peer.consumer) {
      const videoTracks = peer.videoStream.getVideoTracks()
      if (videoTracks.length > 0) {
        const track = videoTracks[0]
        const consumerPaused = peer.consumer.paused
        hasVideo = !consumerPaused && track.enabled && track.readyState === 'live'
      }
    } else if (peer.videoStream) {
      const videoTracks = peer.videoStream.getVideoTracks()
      if (videoTracks.length > 0) {
        const track = videoTracks[0]
        hasVideo = track.enabled && track.readyState === 'live'
      }
    }
    
    participants.push({
      id: socketId,
      name: peer.displayName,
      isSelf: false,
      hasVideo: hasVideo,
      hasAudio: !!peer.audioTrack,
      videoStream: peer.videoStream
    })
  }

  return participants
})

const participantCount = computed(() => allParticipants.value.length)

/* ===== 권한 확인 ===== */
async function fetchAuthz() {
  const { data } = await instance.get(`/meeting-rooms/${roomCode.value}/authz`, {
    params: { userNo: myUserNo.value }
  })
  info.value = data
  if (!data?.authorized) throw new Error('이 회의에 참여 권한이 없습니다.')
}

/* ===== 비디오 연결 ===== */
function connectVideoStream() {
  if (!localVideoRef.value) {
    setTimeout(connectVideoStream, 100)
    return
  }

  if (!localStream.value) return

  localVideoRef.value.srcObject = localStream.value
  localVideoRef.value.play().catch((playError) => {
    console.warn('Video play failed:', playError)
  })
}

/* ===== 미디어 제어 이벤트 핸들러 ===== */
function handleToggleMic() {
  toggleMic(localStream.value)
}

function handleToggleSpeaker() {
  toggleSpeaker(peers)
}

function handleToggleCam() {
  toggleCam(sfu.value, localStream.value)
  
  // 카메라 토글 후 비디오 요소 업데이트
  setTimeout(() => {
    if (camOn.value) {
      connectVideoStream()
    } else if (localVideoRef.value) {
      localVideoRef.value.srcObject = null
    }
  }, 100)
}

/* ===== 녹음 제어 (에러 처리 추가) ===== */
async function handleStartRecording() {
  const result = await startRecording()
  
  if (!result.success) {
    // 에러 메시지별 처리
    if (result.data?.has_existing_data) {
      // 이미 회의록이 생성된 경우
      alert(
        '⚠️ 이미 회의록이 생성된 방입니다.\n\n' +
        '이 회의는 이미 녹음 및 회의록이 생성되었습니다.\n' +
        '새로운 녹음을 시작할 수 없습니다.'
      )
    } else if (result.message.includes('처리 중')) {
      // 이전 녹음이 처리 중인 경우
      alert(
        '⏳ 이전 녹음 처리 중\n\n' +
        '이전 녹음이 아직 처리되고 있습니다.\n' +
        '잠시 후 다시 시도해주세요.'
      )
    } else if (result.message.includes('진행 중')) {
      // 이미 녹음 중인 경우
      alert(
        '🔴 녹음이 이미 진행 중입니다.\n\n' +
        '현재 녹음을 먼저 종료한 후 다시 시작해주세요.'
      )
    } else {
      // 기타 에러
      alert(`녹음 시작 실패\n\n${result.message}`)
    }
  }
}

/* ===== 회의 종료 ===== */
function leaveRoom() {
  cleanupRecording() // 녹음 중이면 먼저 정리
  
  if (!confirm('회의를 종료하고 나가시겠습니까?')) {
    return
  }

  disconnectSfu()
  stopLocalStream(localStream.value)

  // 새창으로 열린 경우 창 닫기, 아니면 홈으로 이동
  if (window.opener) {
    window.close()
  } else {
    router.replace({ name: 'Home' })
  }
}

/* ===== 라이프사이클 ===== */
onMounted(async () => {
  try {
    if (!roomCode.value) throw new Error('잘못된 접근입니다.')
    
    await fetchAuthz() // 권한 확인
    loading.value = false
    await nextTick()

    localStream.value = await getLocalStream() // 로컬 미디어 스트림 획득
    connectVideoStream() // 비디오 요소에 연결
    await connectSfu() // SFU 서버 연결
    
    error.value = ''
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || String(e)
    loading.value = false
  }
})

onBeforeUnmount(() => {
  leaveRoom()
})
</script>

<template>
  <div class="meeting-room">
    <!-- 상단 헤더 -->
    <header class="header">
      <div class="header-left">
        <h1 class="meeting-title">{{ roomTitle }}</h1>
      </div>

      <div class="header-right">
        <!-- 녹음 상태 표시 (호스트만) -->
        <div v-if="isHost && !isIdle" class="recording-status">
          <div class="recording-indicator" :class="{ 
            recording: isRecording, 
            paused: isPaused,
            processing: isProcessing
          }">
            <span class="recording-dot"></span>
            <span v-if="isRecording">녹음 중</span>
            <span v-else-if="isPaused">일시정지</span>
            <span v-else-if="isProcessing">처리 중</span>
          </div>
          <div v-if="!isProcessing" class="recording-time">
            {{ formatDuration(recordingDuration) }}
          </div>
        </div>

        <div class="participant-count">
          <span class="participant-icon">👥</span>
          <span>{{ participantCount }}</span>
        </div>
      </div>
    </header>

    <!-- 메인 비디오 영역 -->
    <main class="video-area">
      <!-- 로딩 상태 -->
      <div v-if="loading" class="status-overlay">
        <div class="loading-spinner"></div>
        <p class="status-text">회의에 참여하는 중...</p>
      </div>

      <!-- 에러 상태 -->
      <div v-else-if="error" class="status-overlay error-state">
        <div class="error-icon">⚠️</div>
        <p class="status-text">{{ error }}</p>
        <button class="retry-btn" @click="$router.go(0)">다시 시도</button>
      </div>

      <!-- 비디오 그리드 -->
      <VideoGrid 
        v-else
        :participants="allParticipants"
        :mirror="mirror"
        @setLocalVideoRef="setLocalVideoRef"
      />
    </main>

    <!-- 하단 컨트롤바 -->
    <ControlsBar
      :mic-on="micOn"
      :cam-on="camOn"
      :speaker-on="speakerOn"
      :is-host="isHost"
      :is-idle="isIdle"
      :is-recording="isRecording"
      :is-paused="isPaused"
      :is-processing="isProcessing"
      @toggle-mic="handleToggleMic"
      @toggle-cam="handleToggleCam"
      @toggle-speaker="handleToggleSpeaker"
      @start-recording="handleStartRecording"
      @pause-recording="pauseRecording"
      @resume-recording="resumeRecording"
      @stop-recording="stopRecording"
      @leave-room="leaveRoom"
    />
  </div>
</template>

<style scoped>
/* ========== 테마 변수 ========== */
.meeting-room {
  --bg-primary: #1a1a1a;
  --bg-secondary: #2a2a2a;
  --bg-tertiary: #0f0f0f;
  --text-primary: #ffffff;
  --text-secondary: #b3b3b3;
  --accent-color: #00a8ff;
  --danger-color: #ff4757;
  --success-color: #2ed573;
  --warning-color: #ffa502;
  --border-color: #3a3a3a;
  --shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  --radius: 12px;
  --transition: all 0.2s ease;
}

/* ========== 메인 레이아웃 ========== */
.meeting-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
  overflow: hidden;
}

/* ========== 헤더 ========== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  min-height: 60px;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.meeting-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ========== 녹음 상태 ========== */
.recording-status {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-tertiary);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
}

.recording-indicator.recording {
  color: var(--danger-color);
}

.recording-indicator.paused {
  color: var(--warning-color);
}

.recording-indicator.processing {
  color: var(--accent-color);
}

.recording-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s ease-in-out infinite;
}

.recording-indicator.paused .recording-dot {
  animation: none;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.recording-time {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  min-width: 40px;
}

.participant-count {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-tertiary);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* ========== 메인 비디오 영역 ========== */
.video-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--bg-tertiary);
  position: relative;
  overflow: hidden;
}

/* ========== 상태 오버레이 ========== */
.status-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top: 3px solid var(--accent-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.status-text {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

.error-state .error-icon {
  font-size: 48px;
}

.retry-btn {
  padding: 10px 20px;
  background: var(--accent-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  font-weight: 500;
}

/* ========== 반응형 ========== */
@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
    min-height: 50px;
  }

  .meeting-title {
    font-size: 16px;
  }

  .video-area {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .participant-count {
    display: none;
  }

  .recording-status {
    font-size: 11px;
    padding: 6px 8px;
  }
}
</style>