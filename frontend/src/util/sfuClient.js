// SFU 클라이언트 SDK - 브라우저에서 SFU 서버와 통신하는 라이브러리
import { io } from "socket.io-client"
import { Device } from "mediasoup-client"

const DEFAULT_URL = undefined // 현재 웹사이트와 같은 주소 사용
const DEFAULT_PATH = "/sfu"

export class SfuClient {
  constructor({ roomCode, displayName = "", url = DEFAULT_URL, path = DEFAULT_PATH, token = null, autoConsume = false } = {}) {
    if (!roomCode) throw new Error("roomCode is required")

    // 기본 설정
    this.roomCode = roomCode
    this.displayName = displayName || `user-${Math.random().toString(36).slice(2, 7)}`

    // 연결 설정
    this._url = url
    this._path = path
    this._token = token
    this._autoConsume = !!autoConsume

    // 연결 객체들
    this.socket = null
    this.device = null
    this.sendTransport = null
    this.recvTransport = null

    // 상태 관리
    this.remoteConsumers = new Map() // 다른 사람들의 미디어
    this._joinedOnce = false
    this._needRejoin = false
    this._localTracks = { audio: null, video: null }
    this._producers = { audio: null, video: null }

    // 이벤트 콜백들
    this.onPeerJoined = null
    this.onPeerLeft = null
    this.onNewProducer = null
    this.onRejoined = null
    this.onTrack = null

    this._handlers = {} // 이벤트 핸들러 저장소
  }

  // Socket.IO 서버 연결
  connect() {
    this.socket = io(this._url, {
      path: this._path,
      transports: ["websocket"],
      reconnection: true,
      reconnectionAttempts: Infinity,
      reconnectionDelayMax: 3000,
      auth: this._token ? { token: this._token } : undefined,
      withCredentials: false
    })

    this.setupConnectionEvents()
    this.setupRoomEvents()
    this.setupMediaEvents()
  }

  // 연결 관련 이벤트들
  setupConnectionEvents() {
    this._handlers.onConnect = async () => {
      if (this._joinedOnce && this._needRejoin) {
        try {
          await this._rejoin()
          this._needRejoin = false
          this.onRejoined?.()
        } catch (e) {
          console.warn("[SFU] rejoin failed:", e)
        }
      }
    }
    this.socket.on("connect", this._handlers.onConnect)

    this._handlers.onDisconnect = (reason) => {
      if (this._joinedOnce) this._needRejoin = true
    }
    this.socket.on("disconnect", this._handlers.onDisconnect)

    this._handlers.onConnectError = (err) =>
      console.warn("[SFU] connect_error:", err?.message || err)
    this.socket.on("connect_error", this._handlers.onConnectError)
  }

  // 방 관련 이벤트들
  setupRoomEvents() {
    this._handlers.onPeerJoined = (p) => this.onPeerJoined?.(p)
    this.socket.on("sfu:peer-joined", this._handlers.onPeerJoined)

    this._handlers.onPeerLeft = (p) => {
      this.cleanupPeerConsumers(p.socketId)
      this.onPeerLeft?.(p)
    }
    this.socket.on("sfu:peer-left", this._handlers.onPeerLeft)
  }

  // 미디어 관련 이벤트들
  setupMediaEvents() {
    this._handlers.onNewProducer = async (p) => {
      if (this._autoConsume) {
        try {
          const { consumer, track, kind } = await this.consume(p.producerId)
          this.registerRemoteConsumer(p.socketId, kind, consumer, track)
          this.onTrack?.({ socketId: p.socketId, kind, track, consumer })
        } catch (e) {
          console.warn("[SFU] auto-consume failed:", e)
        }
      } else {
        this.onNewProducer?.(p)
      }
    }
    this.socket.on("sfu:new-producer", this._handlers.onNewProducer)

    this._handlers.onProducerClosed = ({ socketId, kind }) => {
      const key = `${socketId}:${kind}`
      const entry = this.remoteConsumers.get(key)
      if (entry) {
        try { entry.consumer.close() } catch { }
        try { entry.track?.stop?.() } catch { }
        this.remoteConsumers.delete(key)
      }
    }
    this.socket.on("sfu:producer-closed", this._handlers.onProducerClosed)

    // 카메라/마이크 on/off 이벤트들
    this._handlers.onProducerPaused = (data) => {
      // 상대방이 카메라/마이크를 껐을 때 처리
    }
    this.socket.on("sfu:producer-paused", this._handlers.onProducerPaused)

    this._handlers.onProducerResumed = (data) => {
      // 상대방이 카메라/마이크를 켰을 때 처리
    }
    this.socket.on("sfu:producer-resumed", this._handlers.onProducerResumed)
  }

  // 회의방 입장
  async join() {
    // 소켓 연결 대기
    if (!this.socket?.connected) {
      await this.waitForConnection()
    }

    // 서버에 입장 요청
    const response = await this.sendJoinRequest()

    // Mediasoup Device 초기화
    await this.initializeDevice(response.rtpCapabilities)

    // 서버에 내 미디어 능력치 전송
    this.sendRtpCapabilities()

    this._joinedOnce = true
    return { peers: response.peers || [], existingProducers: response.existingProducers || [] }
  }

  // 소켓 연결 대기
  async waitForConnection() {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error("socket connect timeout")), 10000)
      this.socket.once("connect", () => {
        clearTimeout(timeout)
        resolve()
      })
    })
  }

  // 서버에 입장 요청
  async sendJoinRequest() {
    return new Promise((resolve) => {
      this.socket.emit("sfu:join", {
        roomCode: this.roomCode,
        displayName: this.displayName
      }, (response) => {
        if (!response?.ok) throw new Error(response?.error || "sfu:join failed")
        resolve(response)
      })
    })
  }

  // Mediasoup Device 초기화
  async initializeDevice(rtpCapabilities) {
    if (!this.device) this.device = new Device()
    if (!this.device.loaded) {
      await this.device.load({ routerRtpCapabilities: rtpCapabilities })
    }
  }

  // RTP 능력치 전송
  sendRtpCapabilities() {
    this.socket.emit("sfu:set-rtp-capabilities", {
      roomCode: this.roomCode,
      rtpCapabilities: this.device.rtpCapabilities
    })
  }

  // 재연결 시 복구 처리
  async _rejoin() {
    // 기존 연결들 정리
    this.cleanupTransports()
    this.cleanupConsumers()

    // 다시 입장
    const { existingProducers } = await this.join()

    // 내 미디어 다시 송출
    await this.restoreLocalProducers()

    // 다른 사람들 미디어 다시 수신
    await this.restoreRemoteConsumers(existingProducers)
  }

  // Transport 정리
  cleanupTransports() {
    try { this.sendTransport?.close() } catch { }
    try { this.recvTransport?.close() } catch { }
    this.sendTransport = null
    this.recvTransport = null
  }

  // Consumer 정리
  cleanupConsumers() {
    for (const { consumer, track } of this.remoteConsumers.values()) {
      try { consumer.close() } catch { }
      try { track?.stop?.() } catch { }
    }
    this.remoteConsumers.clear()
  }

  // 내 미디어 복구
  async restoreLocalProducers() {
    for (const kind of ["audio", "video"]) {
      const track = this._localTracks[kind]
      if (track && track.readyState === "live") {
        try {
          await this.produce(kind, track)
        } catch (e) {
          console.warn("[SFU] reproduce fail", kind, e)
        }
      }
    }
  }

  // 다른 사람들 미디어 복구
  async restoreRemoteConsumers(existingProducers) {
    for (const producer of existingProducers) {
      try {
        const { consumer, track } = await this.consume(producer.producerId)
        this.registerRemoteConsumer(producer.socketId, producer.kind, consumer, track)
        if (this._autoConsume) {
          this.onTrack?.({ socketId: producer.socketId, kind: producer.kind, track, consumer })
        }
      } catch (e) {
        console.warn("[SFU] consume(existing) on rejoin failed:", e)
      }
    }
  }

  // WebRTC Transport 생성
  async createTransport(direction) {
    const response = await new Promise((resolve) => {
      this.socket.emit("sfu:create-transport", {
        roomCode: this.roomCode,
        direction
      }, resolve)
    })

    if (!response?.ok) throw new Error(response?.error || "sfu:create-transport failed")

    const transport = direction === "send"
      ? this.device.createSendTransport(response.transportOptions)
      : this.device.createRecvTransport(response.transportOptions)

    this.setupTransportEvents(transport, direction)
    return transport
  }

  // Transport 이벤트 설정
  setupTransportEvents(transport, direction) {
    transport.on("connect", ({ dtlsParameters }, callback, errCallback) => {
      this.socket.emit("sfu:connect-transport", {
        roomCode: this.roomCode,
        transportId: transport.id,
        dtlsParameters
      }, (res) => res?.ok ? callback() : errCallback(new Error(res?.error || "connect-transport failed")))
    })

    if (direction === "send") {
      transport.on("produce", ({ kind, rtpParameters }, callback, errCallback) => {
        this.socket.emit("sfu:produce", {
          roomCode: this.roomCode,
          transportId: transport.id,
          kind,
          rtpParameters
        }, (res) => res?.ok ? callback({ id: res.producerId }) : errCallback(new Error(res?.error || "produce failed")))
      })
    }
  }

  // 송신 Transport 확보
  async ensureSendTransport() {
    if (!this.sendTransport) {
      this.sendTransport = await this.createTransport("send")
    }
    return this.sendTransport
  }

  // 수신 Transport 확보
  async ensureRecvTransport() {
    if (!this.recvTransport) {
      this.recvTransport = await this.createTransport("recv")
    }
    return this.recvTransport
  }

  // 미디어 송출 시작
  async produce(kind, track) {
    if (!track) throw new Error("track is required")
    if (!this.device?.canProduce(kind)) throw new Error(`device cannot produce ${kind}`)

    const sendTransport = await this.ensureSendTransport()
    const producer = await sendTransport.produce({ track })

    this._localTracks[kind] = track
    this._producers[kind] = producer

    // Producer 종료 시 정리
    producer.on("transportclose", () => { this._producers[kind] = null })
    producer.on("close", () => { this._producers[kind] = null })

    return producer
  }

  // 미디어 일시정지
  async pause(kind) {
    const producer = this._producers[kind]
    if (!producer) return

    producer.pause()

    const response = await new Promise((resolve) => {
      this.socket.emit("sfu:pause-producer", {
        roomCode: this.roomCode,
        producerId: producer.id
      }, resolve)
    })

    if (!response?.ok) {
      console.warn(`[SFU] pause-producer failed: ${response?.error}`)
    }
  }

  // 미디어 재개
  async resume(kind) {
    const producer = this._producers[kind]
    if (!producer) return

    producer.resume()

    const response = await new Promise((resolve) => {
      this.socket.emit("sfu:resume-producer", {
        roomCode: this.roomCode,
        producerId: producer.id
      }, resolve)
    })

    if (!response?.ok) {
      console.warn(`[SFU] resume-producer failed: ${response?.error}`)
    }
  }

  // 미디어 수신 시작
  async consume(producerId) {
    const recvTransport = await this.ensureRecvTransport()

    const response = await new Promise((resolve) => {
      this.socket.emit("sfu:consume", {
        roomCode: this.roomCode,
        producerId,
        rtpCapabilities: this.device.rtpCapabilities,
        transportId: recvTransport.id
      }, resolve)
    })

    if (!response?.ok) throw new Error(response?.error || "sfu:consume failed")

    const consumer = await recvTransport.consume({
      id: response.consumerId,
      producerId: response.producerId,
      kind: response.kind,
      rtpParameters: response.rtpParameters
    })

    return { consumer, track: consumer.track, kind: consumer.kind }
  }

  // 원격 Consumer 등록
  registerRemoteConsumer(socketId, kind, consumer, track) {
    const key = `${socketId}:${kind}`
    const previous = this.remoteConsumers.get(key)

    if (previous) {
      try { previous.consumer.close() } catch { }
      try { previous.track?.stop?.() } catch { }
    }

    this.remoteConsumers.set(key, { consumer, track })
  }

  // 특정 참가자 Consumer 정리
  cleanupPeerConsumers(socketId) {
    for (const key of [...this.remoteConsumers.keys()]) {
      if (!key.startsWith(socketId + ":")) continue

      const entry = this.remoteConsumers.get(key)
      try { entry?.consumer?.close() } catch { }
      try { entry?.track?.stop?.() } catch { }
      this.remoteConsumers.delete(key)
    }
  }

  // 연결 종료 및 정리
  disconnect() {
    this.cleanupTransports()
    this.cleanupConsumers()
    this.cleanupProducers()
    this.removeEventListeners()

    try { this.socket?.disconnect() } catch { }
  }

  // Producer 정리
  cleanupProducers() {
    try { this._producers.audio?.close?.() } catch { }
    try { this._producers.video?.close?.() } catch { }
    this._producers = { audio: null, video: null }
  }

  // 이벤트 리스너 제거
  removeEventListeners() {
    if (this.socket) {
      this.socket.off("connect", this._handlers.onConnect)
      this.socket.off("disconnect", this._handlers.onDisconnect)
      this.socket.off("connect_error", this._handlers.onConnectError)
      this.socket.off("sfu:peer-joined", this._handlers.onPeerJoined)
      this.socket.off("sfu:peer-left", this._handlers.onPeerLeft)
      this.socket.off("sfu:new-producer", this._handlers.onNewProducer)
      this.socket.off("sfu:producer-closed", this._handlers.onProducerClosed)
      this.socket.off("sfu:producer-paused", this._handlers.onProducerPaused)
      this.socket.off("sfu:producer-resumed", this._handlers.onProducerResumed)
    }
  }
}

// 팩토리 함수
export function createSfuClient(options) {
  const sfu = new SfuClient(options)
  sfu.connect()
  return sfu
}