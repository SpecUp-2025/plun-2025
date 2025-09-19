// SFU(Selective Forwarding Unit) 서버 - 화상회의 미디어 중계 서버
import express from "express"
import http from "http"
import { Server } from "socket.io"
import * as mediasoup from "mediasoup"

// 서버 설정값들
const PORT = Number(process.env.PORT || 4000)
const IO_PATH = process.env.IO_PATH || "/sfu"
const CLIENT_ORIGIN = process.env.CLIENT_ORIGIN || "http://localhost:3000"
const LISTEN_IP = process.env.LISTEN_IP || "127.0.0.1"
const ANNOUNCED_IP = process.env.ANNOUNCED_IP || "127.0.0.1"
const WEBRTC_PORT = process.env.WEBRTC_PORT ? Number(process.env.WEBRTC_PORT) : 0
const RTC_MIN = Number(process.env.RTC_MIN || 40000)
const RTC_MAX = Number(process.env.RTC_MAX || 49999)

// HTTP 서버와 Socket.IO 서버 생성
const app = express()
app.get("/healthz", (_req, res) => res.status(200).send("ok"))

const server = http.createServer(app)
const io = new Server(server, {
  path: IO_PATH,
  cors: { origin: CLIENT_ORIGIN, credentials: true },
})

// 전역 변수들
let worker // WebRTC 미디어 처리 워커
let webRtcServer = null // 단일포트 모드용 서버  
const rooms = new Map() // 모든 회의방 저장소: roomCode -> { router, peers }

// Mediasoup 워커 초기화
const workerReady = (async () => {
  worker = await mediasoup.createWorker({
    rtcMinPort: RTC_MIN,
    rtcMaxPort: RTC_MAX,
    logLevel: "warn",
    logTags: ["info", "ice", "dtls", "rtp", "srtp", "rtcp"],
  })

  // 워커가 죽으면 프로세스 종료
  worker.on("died", () => {
    console.error("[SFU] mediasoup worker died. Exiting…")
    setTimeout(() => process.exit(1), 1500)
  })

  // 단일포트 모드 설정 (선택사항)
  if (WEBRTC_PORT > 0) {
    webRtcServer = await worker.createWebRtcServer({
      listenInfos: [
        { protocol: "udp", ip: LISTEN_IP, announcedIp: ANNOUNCED_IP, port: WEBRTC_PORT },
        { protocol: "tcp", ip: LISTEN_IP, announcedIp: ANNOUNCED_IP, port: WEBRTC_PORT },
      ],
    })
  }

  console.log(`[SFU] worker ready (${webRtcServer ? `single-port:${WEBRTC_PORT}` : `port-range:${RTC_MIN}-${RTC_MAX}`})`)
})()

// 회의방 생성 또는 가져오기
async function getOrCreateRoom(roomCode) {
  await workerReady
  let room = rooms.get(roomCode)

  if (!room) {
    const router = await worker.createRouter({
      mediaCodecs: [
        { kind: "audio", mimeType: "audio/opus", clockRate: 48000, channels: 2 },
        { kind: "video", mimeType: "video/VP8", clockRate: 90000 },
        {
          kind: "video",
          mimeType: "video/H264",
          clockRate: 90000,
          parameters: { "packetization-mode": 1, "profile-level-id": "42e01f", "level-asymmetry-allowed": 1 },
        },
      ],
    })

    room = {
      router,
      peers: new Map() // 참가자들 정보
    }
    rooms.set(roomCode, room)
  }

  return room
}

// WebRTC 전송 채널 생성 및 설정
async function createWebRtcTransport(router) {
  const transportOptions = {
    enableUdp: true,
    enableTcp: true,
    preferUdp: true,
    initialAvailableOutgoingBitrate: 600_000,
  }

  let transport
  if (webRtcServer) {
    transport = await router.createWebRtcTransport({ webRtcServer, ...transportOptions })
  } else {
    transport = await router.createWebRtcTransport({
      listenIps: [{ ip: LISTEN_IP, announcedIp: ANNOUNCED_IP }],
      ...transportOptions
    })
  }

  setupTransportEvents(transport)
  return transport
}

// 전송 채널 이벤트 설정
function setupTransportEvents(transport) {
  transport.on("dtlsstatechange", (state) => {
    if (state === "closed" || state === "failed") {
      try { transport.close() } catch { }
    }
  })
}

// Socket.IO 연결 처리
io.on("connection", (socket) => {
  console.log("[SFU] New client connected:", socket.id)

  // 회의방 입장
  socket.on("sfu:join", async ({ roomCode, displayName }, callback) => {
    try {
      const room = await getOrCreateRoom(roomCode)

      // 중복 참가 방지
      if (room.peers.has(socket.id)) {
        return callback?.({ ok: false, error: "already joined" })
      }

      socket.join(roomCode)

      // 참가자 상태 초기화
      const peerState = {
        transports: new Map(),
        producers: new Map(),
        consumers: new Map(),
        rtpCapabilities: null,
        displayName: displayName || `user-${socket.id.slice(0, 5)}`,
      }
      room.peers.set(socket.id, peerState)

      // 기존 참가자 목록
      const existingPeers = [...room.peers.entries()]
        .filter(([id]) => id !== socket.id)
        .map(([id, peer]) => ({ socketId: id, displayName: peer.displayName }))

      // 기존 미디어 스트림 목록
      const existingProducers = []
      for (const [id, peer] of room.peers) {
        if (id === socket.id) continue
        for (const [, producer] of peer.producers) {
          existingProducers.push({
            producerId: producer.id,
            kind: producer.kind,
            socketId: id,
            displayName: peer.displayName,
          })
        }
      }

      // 다른 참가자들에게 새 참가자 알림
      socket.to(roomCode).emit("sfu:peer-joined", {
        socketId: socket.id,
        displayName: peerState.displayName
      })

      // 클라이언트에게 초기 데이터 전송
      callback?.({
        ok: true,
        peers: existingPeers,
        rtpCapabilities: room.router.rtpCapabilities,
        existingProducers
      })

    } catch (err) {
      console.error("sfu:join error:", err)
      callback?.({ ok: false, error: String(err) })
    }
  })

  // 미디어 전송 설정
  setupMediaHandlers(socket)

  // 연결 종료 처리
  setupDisconnectHandlers(socket)
})

// 미디어 관련 이벤트 핸들러들
function setupMediaHandlers(socket) {
  // RTP 능력치 설정
  socket.on("sfu:set-rtp-capabilities", ({ roomCode, rtpCapabilities }) => {
    const room = rooms.get(roomCode)
    if (!room) return
    const peer = room.peers.get(socket.id)
    if (peer) peer.rtpCapabilities = rtpCapabilities
  })

  // 전송 채널 생성
  socket.on("sfu:create-transport", async ({ roomCode, direction }, callback) => {
    try {
      const room = rooms.get(roomCode)
      if (!room) throw new Error("room not found")

      const transport = await createWebRtcTransport(room.router)
      const peer = room.peers.get(socket.id)
      peer?.transports.set(transport.id, transport)

      transport.observer.on("close", () => {
        peer?.transports.delete(transport.id)
      })

      callback?.({
        ok: true,
        direction,
        transportOptions: {
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters,
        }
      })
    } catch (err) {
      callback?.({ ok: false, error: String(err) })
    }
  })

  // 전송 채널 연결
  socket.on("sfu:connect-transport", async ({ roomCode, transportId, dtlsParameters }, callback) => {
    try {
      const room = rooms.get(roomCode)
      const peer = room?.peers.get(socket.id)
      const transport = peer?.transports.get(transportId)

      if (!transport) throw new Error("transport not found")

      await transport.connect({ dtlsParameters })
      callback?.({ ok: true })
    } catch (err) {
      callback?.({ ok: false, error: String(err) })
    }
  })

  // 미디어 송출 및 수신
  setupProducerConsumerHandlers(socket)

  // 미디어 컨트롤 (일시정지/재개)
  setupMediaControlHandlers(socket)
}

// Producer/Consumer 핸들러들
function setupProducerConsumerHandlers(socket) {
  // 미디어 송출 시작
  socket.on("sfu:produce", async ({ roomCode, transportId, kind, rtpParameters }, callback) => {
    try {
      const room = rooms.get(roomCode)
      const peer = room?.peers.get(socket.id)
      const transport = peer?.transports.get(transportId)

      if (!transport) throw new Error("transport not found")

      const producer = await transport.produce({ kind, rtpParameters })
      peer?.producers.set(producer.id, producer)

      socket.to(roomCode).emit("sfu:new-producer", {
        producerId: producer.id,
        kind,
        socketId: socket.id,
        displayName: peer?.displayName,
      })

      producer.observer.on("close", () => {
        peer?.producers.delete(producer.id)
        socket.to(roomCode).emit("sfu:producer-closed", {
          producerId: producer.id,
          socketId: socket.id,
          kind,
        })
      })

      callback?.({ ok: true, producerId: producer.id })
    } catch (err) {
      callback?.({ ok: false, error: String(err) })
    }
  })

  // 미디어 수신 시작
  socket.on("sfu:consume", async ({ roomCode, producerId, rtpCapabilities, transportId }, callback) => {
    try {
      const room = rooms.get(roomCode)
      const peer = room?.peers.get(socket.id)
      const transport = peer?.transports.get(transportId)

      if (!transport) throw new Error("transport not found")
      if (!room.router.canConsume({ producerId, rtpCapabilities })) {
        throw new Error("incompatible rtpCapabilities")
      }

      const consumer = await transport.consume({
        producerId,
        rtpCapabilities,
        paused: false,
      })
      peer?.consumers.set(consumer.id, consumer)

      consumer.observer.on("close", () => {
        peer?.consumers.delete(consumer.id)
      })

      callback?.({
        ok: true,
        consumerId: consumer.id,
        kind: consumer.kind,
        rtpParameters: consumer.rtpParameters,
        producerId,
      })
    } catch (err) {
      callback?.({ ok: false, error: String(err) })
    }
  })
}

// 미디어 컨트롤 핸들러들 (카메라/마이크 on/off)
function setupMediaControlHandlers(socket) {
  socket.on("sfu:pause-producer", async ({ roomCode, producerId }, callback) => {
    try {
      const { peer, producer } = await findProducer(roomCode, socket.id, producerId)
      await producer.pause()

      socket.to(roomCode).emit("sfu:producer-paused", {
        producerId,
        socketId: socket.id,
        kind: producer.kind
      })

      callback?.({ ok: true })
    } catch (err) {
      callback?.({ ok: false, error: String(err) })
    }
  })

  socket.on("sfu:resume-producer", async ({ roomCode, producerId }, callback) => {
    try {
      const { peer, producer } = await findProducer(roomCode, socket.id, producerId)
      await producer.resume()

      socket.to(roomCode).emit("sfu:producer-resumed", {
        producerId,
        socketId: socket.id,
        kind: producer.kind
      })

      callback?.({ ok: true })
    } catch (err) {
      callback?.({ ok: false, error: String(err) })
    }
  })
}

// Producer 찾기 헬퍼 함수
async function findProducer(roomCode, socketId, producerId) {
  const room = rooms.get(roomCode)
  if (!room) throw new Error("room not found")

  const peer = room.peers.get(socketId)
  if (!peer) throw new Error("peer not found")

  const producer = peer.producers.get(producerId)
  if (!producer) throw new Error("producer not found")

  return { peer, producer }
}

// 연결 종료 관련 핸들러들
function setupDisconnectHandlers(socket) {
  // 연결 끊기 직전: 다른 참가자들에게 알림
  socket.on("disconnecting", () => {
    for (const roomCode of socket.rooms) {
      if (rooms.has(roomCode)) {
        socket.to(roomCode).emit("sfu:peer-left", { socketId: socket.id })
      }
    }
  })

  // 실제 연결 종료: 리소스 정리
  socket.on("disconnect", () => {
    cleanupPeerResources(socket.id)
  })
}

// 참가자의 모든 리소스 정리
function cleanupPeerResources(socketId) {
  for (const [roomCode, room] of rooms) {
    const peer = room.peers.get(socketId)
    if (!peer) continue

    // 모든 미디어 리소스 종료
    closeAllResources(peer)
    room.peers.delete(socketId)

    // 빈 방 정리
    if (room.peers.size === 0) {
      try { room.router.close() } catch { }
      rooms.delete(roomCode)
    }
  }
}

// 리소스 종료 헬퍼 함수
function closeAllResources(peer) {
  for (const [, consumer] of peer.consumers) {
    try { consumer.close() } catch { }
  }
  for (const [, producer] of peer.producers) {
    try { producer.close() } catch { }
  }
  for (const [, transport] of peer.transports) {
    try { transport.close() } catch { }
  }
}

// 서버 시작
workerReady
  .then(() => {
    server.listen(PORT, () => {
      console.log(`[SFU] Server running on port ${PORT}`)
      console.log(`[SFU] Socket.IO path: ${IO_PATH}`)
      console.log(`[SFU] Allowed origin: ${CLIENT_ORIGIN}`)
    })
  })
  .catch((error) => {
    console.error("[SFU] Failed to start server:", error)
    process.exit(1)
  })