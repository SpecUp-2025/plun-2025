<!-- src/components/meeting/MeetingRoom.vue -->
<template>
  <div class="wrap">
    <main class="grid">
      <video ref="localVideo" autoplay playsinline muted
             :style="{ transform: mirror ? 'scaleX(-1)' : 'none' }"></video>
      <video ref="remoteVideo" autoplay playsinline></video>
    </main>

    <footer class="controls">
      <button @click="toggleMic" :disabled="!localReady">{{ micOn ? '마이크 끄기' : '마이크 켜기' }}</button>
      <button @click="toggleCam" :disabled="!localReady">{{ camOn ? '카메라 끄기' : '카메라 켜기' }}</button>
      <button class="bye" @click="hangup">나가기</button>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/* 라우터/유저 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const roomCode = route.params.roomCode
const userNo = computed(() => Number(userStore.user?.userNo ?? 0))
const myId = computed(() => String(userNo.value))   // 신호 식별자

/* 장치/상태 */
const localVideo = ref(null)
const remoteVideo = ref(null)
let localStream = null
let pc = null
let stomp = null
let otherId = null
const localReady = ref(false)

const micOn = ref(true)
const camOn = ref(true)
const mirror = ref(localStorage.getItem('pref.mirror') === 'true')
const selectedVideoId = localStorage.getItem('pref.videoId') || ''
const selectedAudioId = localStorage.getItem('pref.audioId') || ''

/* 엔드포인트/토픽 (서버 설정과 일치) */
const SOCKJS_ENDPOINT = '/ws-chat'
const SUB_DEST = (rid) => `/topic/webrtc/${rid}`
const PUB_DEST = '/app/webrtc.signal'

/* ICE 서버 */
const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  // { urls: 'turn:YOUR_TURN_IP:3478', username: 'xxx', credential: 'yyy' },
]

/* 시그널 전송 */
function sendSignal(obj) {
  if (!stomp || !stomp.connected) return
  stomp.publish({
    destination: PUB_DEST,
    body: JSON.stringify({ ...obj, roomId: roomCode, from: myId.value })
  })
}

/* 로컬 미디어 */
async function getLocalMedia() {
  const constraints = {
    video: selectedVideoId
      ? { deviceId: { exact: selectedVideoId }, width: { ideal: 1280 }, height: { ideal: 720 } }
      : { width: { ideal: 1280 }, height: { ideal: 720 } },
    audio: selectedAudioId ? { deviceId: { exact: selectedAudioId } } : true
  }
  localStream = await navigator.mediaDevices.getUserMedia(constraints)
  if (localVideo.value) localVideo.value.srcObject = localStream
  micOn.value = localStream.getAudioTracks()[0]?.enabled ?? true
  camOn.value = localStream.getVideoTracks()[0]?.enabled ?? true
  localReady.value = true
}

/* PeerConnection */
function createPC() {
  pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
  if (localStream) {
    localStream.getTracks().forEach(track => pc.addTrack(track, localStream))
  }
  pc.ontrack = (e) => {
    const remoteStream = e.streams && e.streams[0]
    if (remoteVideo.value && remoteStream) remoteVideo.value.srcObject = remoteStream
  }
  pc.onicecandidate = (e) => {
    if (e.candidate) sendSignal({ type: 'candidate', payload: e.candidate })
  }
}

async function makeOfferIfCaller() {
  if (!otherId) return
  if (Number(myId.value) >= Number(otherId)) return
  if (!pc) createPC()
  const offer = await pc.createOffer()
  await pc.setLocalDescription(offer)
  sendSignal({ type: 'offer', payload: offer })
}

async function handleOffer(offer) {
  if (!pc) createPC()
  await pc.setRemoteDescription(new RTCSessionDescription(offer))
  const answer = await pc.createAnswer()
  await pc.setLocalDescription(answer)
  sendSignal({ type: 'answer', payload: answer })
}

async function handleAnswer(answer) {
  if (!pc) return
  await pc.setRemoteDescription(new RTCSessionDescription(answer))
}

function handleCandidate(cand) {
  if (!pc) return
  pc.addIceCandidate(new RTCIceCandidate(cand)).catch(() => {})
}

/* STOMP */
function connectStomp() {
  stomp = new Client({
    webSocketFactory: () => new SockJS(SOCKJS_ENDPOINT),
    reconnectDelay: 1000
  })
  stomp.onConnect = () => {
    stomp.subscribe(SUB_DEST(roomCode), (frame) => {
      const msg = JSON.parse(frame.body || '{}')
      if (msg.from === myId.value) return
      if (!otherId) otherId = String(msg.from)
      handleSignal(msg)
    })
    sendSignal({ type: 'join', payload: null })
  }
  stomp.activate()
}

/* 메시지 처리 */
function handleSignal(msg) {
  switch (msg.type) {
    case 'join':      makeOfferIfCaller(); break
    case 'offer':     handleOffer(msg.payload); break
    case 'answer':    handleAnswer(msg.payload); break
    case 'candidate': handleCandidate(msg.payload); break
    case 'leave':     cleanupPeer(); break
  }
}

/* 토글/정리 */
function toggleMic() {
  const t = localStream?.getAudioTracks()[0]; if (!t) return
  t.enabled = !t.enabled; micOn.value = t.enabled
}
function toggleCam() {
  const t = localStream?.getVideoTracks()[0]; if (!t) return
  t.enabled = !t.enabled; camOn.value = t.enabled
}

function cleanupPeer() {
  try { pc?.getSenders()?.forEach(s => s.track && s.track.stop()) } catch {}
  try { pc && pc.close() } catch {}
  pc = null
  if (remoteVideo.value && remoteVideo.value.srcObject) {
    const rs = remoteVideo.value.srcObject
    if (rs && rs.getTracks) rs.getTracks().forEach(t => t.stop())
    remoteVideo.value.srcObject = null
  }
  otherId = null
}

function hangup() {
  sendSignal({ type: 'leave', payload: null })
  cleanupPeer()
  try { localStream?.getTracks()?.forEach(t => t.stop()) } catch {}
  localStream = null
  if (localVideo.value) localVideo.value.srcObject = null
  if (stomp) { stomp.deactivate(); stomp = null }
  router.push('/meeting-main')
}

/* 라이프사이클 */
onMounted(async () => {
  if (!roomCode || !userNo.value) { router.push('/meeting-main'); return }
  try {
    await nextTick()
    await getLocalMedia()
    createPC()
    connectStomp()
  } catch (e) {
    router.push('/meeting-main')
  }
})
onBeforeUnmount(() => { hangup() })
</script>

<style scoped>
.wrap { display:flex; flex-direction:column; height:100vh; background:#0b0b0b; }
.grid { display:grid; grid-template-columns:repeat(auto-fit, minmax(280px,1fr)); gap:8px; padding:8px; flex:1; }
video { width:100%; height:100%; background:#000; border-radius:10px; object-fit:cover; }
.controls { display:flex; gap:8px; justify-content:center; padding:10px; border-top:1px solid #222; }
.bye { background:#ef4444; color:#fff; border:none; padding:8px 12px; border-radius:6px; }
button { background:#1f2937; color:#fff; border:none; padding:8px 12px; border-radius:6px; }
button:disabled { opacity:.5; }
</style>
