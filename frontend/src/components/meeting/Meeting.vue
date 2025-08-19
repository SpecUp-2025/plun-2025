<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/** ---------- 반응형 상태/레퍼런스 ---------- */
const roomId   = ref('room-123')
const nickname = ref('user-' + Math.floor(Math.random() * 1000))
const started  = ref(false)
const logs     = ref([])
const log = m => logs.value.push(`[${new Date().toLocaleTimeString()}] ${m}`)

const localVideo  = ref(null)   // 내 <video> DOM 참조
const remoteVideo = ref(null)   // 상대 <video> DOM 참조

/** ---------- 전역 핸들 ---------- */
let stomp = null                // STOMP 클라이언트
let pc = null                   // RTCPeerConnection
let localStream = null          // 내 카메라/마이크 MediaStream
let otherUser = null            // 1:1에서 상대 닉네임 기억

/** ---------- 서버 경로 상수 (Spring 설정과 일치) ---------- */
const SOCKJS_ENDPOINT = '/ws-chat'                  // registerStompEndpoints("/ws-chat")
const SUB_DEST = rid => `/topic/webrtc/${rid}`      // enableSimpleBroker("/topic") + webrtc 네임스페이스
const PUB_DEST = '/app/webrtc.signal'               // setApplicationDestinationPrefixes("/app")

/** ---------- ICE 서버(초기엔 STUN만) ---------- */
const ICE_SERVERS = [{ urls: 'stun:stun.l.google.com:19302' }]

/** ---------- 언마운트시 정리 ---------- */
onBeforeUnmount(() => endMeeting())

/** ---------- 1) 로컬 미디어 얻기 ---------- */
async function getLocalMedia() {
  try {
    localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    localVideo.value.srcObject = localStream
    log('got local media')
  } catch (err) {
    log('getUserMedia error: ' + err)
    alert('카메라/마이크 권한을 허용해주세요.')
  }
}

const micOn = ref(true)
const camOn = ref(true)

/** ---------- 신호(시그널) 발행 유틸 ---------- */
function sendSignal(obj) {
  if (!stomp) return
  stomp.publish({
    destination: PUB_DEST,
    body: JSON.stringify({ ...obj, roomId: roomId.value, from: nickname.value })
  })
  log('SEND: ' + obj.type)
}

/** ---------- 2) STOMP 연결/구독 ---------- */
function connectStomp() {
  stomp = new Client({
    webSocketFactory: () => new SockJS(SOCKJS_ENDPOINT),
    reconnectDelay: 5000,
    onConnect: () => {
      log('STOMP connected')
      // 방 토픽 구독
      stomp.subscribe(SUB_DEST(roomId.value), (frame) => {
        const msg = JSON.parse(frame.body)
        if (msg.from === nickname.value) return        // 내가 보낸 건 무시
        if (!otherUser) otherUser = msg.from           // 1:1 상대 닉네임 저장
        handleSignal(msg)
      })
      // JOIN 알림
      sendSignal({ type: 'join', payload: null })
    },
    onStompError: (f) => log('STOMP error: ' + f.headers['message']),
    onWebSocketError: (e) => log('WS error: ' + (e?.message || e))
  })
  stomp.activate()
}

/** ---------- 3) PeerConnection 생성 ---------- */
function createPC() {
  pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })

  // 내 오디오/비디오 트랙을 연결
  localStream.getTracks().forEach(track => pc.addTrack(track, localStream))

  // 상대 트랙 수신 → 원격 <video>에 바인딩
  pc.ontrack = (e) => {
    log('ontrack')
    remoteVideo.value.srcObject = e.streams[0]
  }

  // ICE 후보가 생길 때마다 시그널링으로 전송
  pc.onicecandidate = (e) => {
    if (e.candidate) sendSignal({ type: 'candidate', payload: e.candidate })
  }

  pc.onconnectionstatechange = () => log('pc state: ' + pc.connectionState)
}

/** ---------- 4) Offer/Answer 교환 (glare 회피용 규칙 포함) ---------- */
// 간단한 호출자 규칙: 문자열 비교(1:1 전제)
async function makeOfferIfCaller() {
  const amCaller = otherUser && nickname.value < otherUser
  if (!amCaller) return
  if (!pc) createPC()
  const offer = await pc.createOffer()
  await pc.setLocalDescription(offer)
  sendSignal({ type: 'offer', payload: offer })
  log('offer sent')
}

async function handleOffer(offer) {
  if (!pc) createPC()
  await pc.setRemoteDescription(new RTCSessionDescription(offer))
  const answer = await pc.createAnswer()
  await pc.setLocalDescription(answer)
  sendSignal({ type: 'answer', payload: answer })
  log('answer sent')
}

async function handleAnswer(answer) {
  if (!pc) return
  await pc.setRemoteDescription(new RTCSessionDescription(answer))
  log('answer applied')
}

function handleCandidate(cand) {
  if (!pc) return
  pc.addIceCandidate(new RTCIceCandidate(cand))
    .catch(err => log('candidate error: ' + err))
}

/** ---------- 5) 방에서 온 메시지 처리 ---------- */
function handleSignal(msg) {
  switch (msg.type) {
    case 'join':
      log('peer joined: ' + msg.from)
      // 상대가 들어왔으면, 내가 caller라면 offer 생성
      makeOfferIfCaller()
      break
    case 'offer':
      log('recv offer')
      handleOffer(msg.payload)
      break
    case 'answer':
      log('recv answer')
      handleAnswer(msg.payload)
      break
    case 'candidate':
      handleCandidate(msg.payload)
      break
    case 'leave':
      log('peer left')
      teardownPC()
      break
  }
}

/** ---------- 6) 시작/종료/토글 ---------- */
async function startMeeting() {
  if (!roomId.value || !nickname.value) {
    alert('roomId / nickname 입력')
    return
  }
  started.value = true                 // ← JOIN 가드 문제 피하려고 먼저 true
  await getLocalMedia()
  connectStomp()
}

function teardownPC() {
  if (pc) { try { pc.close() } catch {}
    pc = null
  }
  if (remoteVideo.value) remoteVideo.value.srcObject = null
  otherUser = null
}

function endMeeting() {
  sendSignal({ type: 'leave', payload: null })
  teardownPC()
  if (localStream) {
    localStream.getTracks().forEach(t => t.stop())
    localStream = null
  }
  if (localVideo.value) localVideo.value.srcObject = null
  if (stomp) { stomp.deactivate(); stomp = null }
  started.value = false
  log('meeting ended')
}

function toggleMic() {
  if (!localStream) return
  const t = localStream.getAudioTracks()[0]; if (!t) return
  t.enabled = !t.enabled
  micOn.value = t.enabled
}

function toggleCam() {
  if (!localStream) return
  const t = localStream.getVideoTracks()[0]; if (!t) return
  t.enabled = !t.enabled
  camOn.value = t.enabled
}
</script>

<template>
  <div class="wrap">
    <div class="toolbar">
      <input v-model="roomId" placeholder="roomId" />
      <input v-model="nickname" placeholder="nickname" />
      <button @click="startMeeting" :disabled="started">회의 시작</button>
      <button @click="endMeeting"   :disabled="!started">회의 종료</button>
      <button @click="toggleMic"    :disabled="!localStream">마이크 {{ micOn ? '끄기' : '켜기' }}</button>
      <button @click="toggleCam"    :disabled="!localStream">카메라 {{ camOn ? '끄기' : '켜기' }}</button>
    </div>

    <div class="videos">
      <div><h4>나</h4><video ref="localVideo" autoplay playsinline muted></video></div>
      <div><h4>상대</h4><video ref="remoteVideo" autoplay playsinline></video></div>
    </div>

    <pre class="log">{{ logs.join('\n') }}</pre>
  </div>
</template>

<style scoped>
.wrap{max-width:960px;margin:24px auto;font-family:system-ui}
.toolbar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}
.videos{display:grid;grid-template-columns:1fr 1fr;gap:12px}
video{width:100%;background:#000;border-radius:12px}
.log{margin-top:12px;background:#0b1020;color:#bcd4ff;padding:10px;border-radius:8px;height:160px;overflow:auto}
</style>
