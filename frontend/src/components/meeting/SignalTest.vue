<template>
  <div class="p-4" style="max-width:720px;margin:0 auto;font-family:system-ui;">
    <h2>STOMP Signal Test</h2>
    <div style="display:flex;gap:8px;flex-wrap:wrap;margin:8px 0;">
      <input v-model="roomId" placeholder="roomId" />
      <input v-model="nickname" placeholder="nickname" />
      <button @click="connect" :disabled="connected">연결</button>
      <button @click="disconnect" :disabled="!connected">해제</button>
      <button @click="sendJoin" :disabled="!connected">JOIN 전송</button>
      <button @click="sendDummyOffer" :disabled="!connected">OFFER 전송</button>
    </div>

    <pre style="background:#0b1020;color:#b9d1ff;padding:12px;border-radius:8px;height:220px;overflow:auto;">
{{ logs.join('\n') }}
    </pre>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const roomId = ref('room-123')
const nickname = ref('user-' + Math.floor(Math.random()*1000))
const logs = ref([])
const log = (m)=> logs.value.push(`[${new Date().toLocaleTimeString()}] ${m}`)

let stomp = null
const connected = ref(false)

// 서버 설정: 백엔드에서 열어둔 엔드포인트와 프리픽스 사용
const SOCKJS_ENDPOINT = '/ws-chat'                 // Spring: registerStompEndpoints("/ws-chat")
const SUB_DEST = (rid)=> `/topic/webrtc/${rid}`    // Spring: enableSimpleBroker("/topic")
const PUB_DEST = '/app/webrtc.signal'              // Spring: setApplicationDestinationPrefixes("/app")

function connect() {
  if (connected.value) return
  stomp = new Client({
    webSocketFactory: () => new SockJS(SOCKJS_ENDPOINT),
    reconnectDelay: 3000,
    onConnect: () => {
      connected.value = true
      log('STOMP connected')
      stomp.subscribe(SUB_DEST(roomId.value), (frame) => {
        log('RECV: ' + frame.body)
      })
      log(`SUBSCRIBED: ${SUB_DEST(roomId.value)}`)
    },
    onStompError: (frame) => {
      log('STOMP error: ' + frame.headers['message'])
    },
    onWebSocketError: (e) => log('WS error: ' + (e?.message || e))
  })
  stomp.activate()
}

function disconnect() {
  if (stomp) {
    stomp.deactivate()
    stomp = null
  }
  connected.value = false
  log('STOMP disconnected')
}

function send(obj) {
  if (!stomp || !connected.value) return
  stomp.publish({
    destination: PUB_DEST,
    body: JSON.stringify(obj)
  })
  log('SEND: ' + JSON.stringify(obj))
}

function sendJoin() {
  send({ type:'join', roomId: roomId.value, from: nickname.value, payload: null })
}

function sendDummyOffer() {
  // 아직 진짜 WebRTC SDP는 없으니 더미 페이로드로 흐름만 테스트
  send({
    type:'offer',
    roomId: roomId.value,
    from: nickname.value,
    payload: { type:'offer', sdp:'v=0 ... (dummy)' }
  })
}
</script>
