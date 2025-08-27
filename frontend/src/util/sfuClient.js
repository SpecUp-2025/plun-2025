// src/util/sfuClient.js
//클라이언트(브라우저) 가 SFU 서버(우리 server.js)와 통신해서 WebRTC 트랜스포트 생성 → 송출(produce) → 수신(consume) 을 수행하도록 도와주는 작은 SDK 클래스
import { io } from "socket.io-client";
import { Device } from "mediasoup-client";

const DEFAULT_URL = undefined; // 동일 오리진 사용
const DEFAULT_PATH = "/sfu";
//DEFAULT_PATH는 서버의 Socket.IO path 와 반드시 동일해야 함. (server.js의 IO_PATH 기본 /sfu)
/**DEFAULT_URL = undefined → io(undefined) 이면 현재 페이지 오리진으로 연결해.
프론트 dev 서버에서 프록시(/sfu → 4000) 걸어놨다면 그대로 잘 붙음.
만약 서버가 다른 도메인/포트라면, 생성자에 url 넘기면 됨. (ex. https://sfu.example.com) */


/**roomCode는 필수. 없으면 즉시 throw.
_localTracks: 내 마이크/카메라의 Track 을 기억 → 재조인 때 다시 produce 가능.
_producers: 실제 서버에 올려둔 Producer 핸들 (pause/resume/replaceTrack 등 제어할 때 사용).
콜백 : UI에 이벤트를 구독시키는 방식 */
export class SfuClient {
  constructor({ roomCode, displayName = "", url = DEFAULT_URL, path = DEFAULT_PATH, token = null } = {}) {
    if (!roomCode) throw new Error("roomCode is required");
    this.roomCode = roomCode;
    this.displayName = displayName || `user-${Math.random().toString(36).slice(2, 7)}`;

    this._url = url;
    this._path = path;
    this._token = token;

    this.socket = null;
    this.device = null;

    this.sendTransport = null;
    this.recvTransport = null;

    // 원격 consumer: key = `${socketId}:${kind}` -> { consumer, track }
    this.remoteConsumers = new Map();

    // 재조인 상태
    this._joinedOnce = false;
    this._needRejoin = false;

    // 로컬 트랙 & 프로듀서(장치교체/복원용)
    this._localTracks = { audio: null, video: null };
    this._producers = { audio: null, video: null };

    // 콜백
    this.onPeerJoined = null;
    this.onPeerLeft = null;
    this.onNewProducer = null;
    this.onRejoined = null;
  }

  /**재연결 전략:연결이 끊기면 _needRejoin = true
연결이 다시 열릴 때(connect) 이미 한 번 join 했던 세션이면 _rejoin() 호출
sfu:peer-left 시 해당 피어의 원격 Consumer 정리(오디오 멈추기/track 해제) */
  connect() {
    this.socket = io(this._url /* undefined면 현재 오리진 */, {
      path: this._path,
      transports: ["websocket"],
      reconnection: true,
      reconnectionAttempts: Infinity,
      reconnectionDelayMax: 3000,
      auth: this._token ? { token: this._token } : undefined,
      withCredentials: false
    });

    this.socket.on("connect", async () => {
      console.log("[SFU] connected:", this.socket.id);
      if (this._joinedOnce && this._needRejoin) {
        try {
          await this._rejoin();
          this._needRejoin = false;
          this.onRejoined?.();
        } catch (e) {
          console.warn("[SFU] rejoin failed:", e);
        }
      }
    });

    this.socket.on("disconnect", (reason) => {
      console.log("[SFU] disconnected:", reason);
      if (this._joinedOnce) this._needRejoin = true;
    });

    this.socket.on("connect_error", (err) => console.warn("[SFU] connect_error:", err?.message || err));

    // 방 이벤트
    this.socket.on("sfu:peer-joined", (p) => this.onPeerJoined?.(p));

    this.socket.on("sfu:peer-left", (p) => {
      this._closePeerConsumers(p.socketId);
      this.onPeerLeft?.(p);
    });

    // 새 producer 알림 → UI가 consume 호출
    this.socket.on("sfu:new-producer", (p) => this.onNewProducer?.(p));

    // 상대가 트랙을 닫은 경우 → 내 consumer 정리
    this.socket.on("sfu:producer-closed", ({ socketId, kind }) => {
      const key = `${socketId}:${kind}`;
      const entry = this.remoteConsumers.get(key);
      if (entry) {
        try { entry.consumer.close(); } catch { }
        try { entry.track?.stop?.(); } catch { }
        this.remoteConsumers.delete(key);
      }
    });
  }

  /**서버가 주는 routerRtpCapabilities 로 Device.load 필수 브라우저-서버 사이 코덱/파라미터 호환 체크가 가능.
서버로 내 rtpCapabilities 를 등록(선택이지만 서버 쪽 호환 체크에 도움)
existingProducers: 내가 들어올 때 이미 방에 있던 다른 참가자들의 producer 목록
ㄴ 이걸 iterate 하면서 즉시 consume 해주면 입장과 동시에 화면/소리가 나옴 */
  async join() {
    if (!this.socket?.connected) {
      await new Promise((res, rej) => {
        const t = setTimeout(() => rej(new Error("socket connect timeout")), 10000);
        this.socket.once("connect", () => { clearTimeout(t); res(); });
      });
    }

    const ack = await new Promise((res) => {
      this.socket.emit("sfu:join", { roomCode: this.roomCode, displayName: this.displayName }, res);
    });
    if (!ack?.ok) throw new Error(ack?.error || "sfu:join failed");

    if (!this.device) this.device = new Device();
    if (!this.device.loaded) await this.device.load({ routerRtpCapabilities: ack.rtpCapabilities });

    // (선택) 서버에 내 rtpCapabilities 등록
    this.socket.emit("sfu:set-rtp-capabilities", {
      roomCode: this.roomCode,
      rtpCapabilities: this.device.rtpCapabilities
    });

    this._joinedOnce = true;
    return { peers: ack.peers || [], existingProducers: ack.existingProducers || [] };
  }

  async _rejoin() {
    // 기존 트랜스포트/리모트 consumer 정리
    try { this.sendTransport?.close(); } catch { }
    try { this.recvTransport?.close(); } catch { }
    this.sendTransport = null;
    this.recvTransport = null;

    for (const { consumer, track } of this.remoteConsumers.values()) {
      try { consumer.close(); } catch { }
      try { track?.stop?.(); } catch { }
    }
    this.remoteConsumers.clear();

    // 다시 join
    const { existingProducers } = await this.join();

    // 로컬 트랙 재-produce
    for (const k of /** @type {const} */ (["audio", "video"])) {
      const t = this._localTracks[k];
      if (t && t.readyState === "live") {
        try { await this.produce(k, t); } catch (e) { console.warn("[SFU] reproduce fail", k, e); }
      }
    }

    // 기존 원격 producer를 즉시 consume (UI와 중복 없게 여기서도 보장)
    for (const p of existingProducers) {
      try {
        const { consumer, track } = await this.consume(p.producerId);
        this.registerRemoteConsumer(p.socketId, p.kind, consumer, track);
      } catch (e) { console.warn("[SFU] consume(existing) on rejoin failed:", e); }
    }
  }

  /*서버에게 “트랜스포트 만들어줘” → ICE/DTLS 파라미터 받음
브라우저에서 createSendTransport/createRecvTransport
트랜스포트가 연결(connect) 이벤트를 발생 → DTLS 파라미터를 서버로 보내 실제 연결 성사
(send만) produce 이벤트에서 서버에 Producer 생성 요청 → ID 되돌려 받음*/
  async _createTransport(direction /* "send" | "recv" */) {
    const ack = await new Promise((res) => {
      this.socket.emit("sfu:create-transport", { roomCode: this.roomCode, direction }, res);
    });
    if (!ack?.ok) throw new Error(ack?.error || "sfu:create-transport failed");

    const { transportOptions } = ack;

    const transport = direction === "send"
      ? this.device.createSendTransport(transportOptions)
      : this.device.createRecvTransport(transportOptions);

    // 상태 로깅(디버깅에 유용)
    transport.on("connectionstatechange", (s) => {
      console.log(`[SFU] ${direction} transport state:`, s);
    });

    transport.on("connect", ({ dtlsParameters }, cb, errb) => {
      this.socket.emit("sfu:connect-transport", {
        roomCode: this.roomCode,
        transportId: transport.id,
        dtlsParameters
      }, (res) => res?.ok ? cb() : errb(new Error(res?.error || "connect-transport failed")));
    });

    if (direction === "send") {
      transport.on("produce", ({ kind, rtpParameters }, cb, errb) => {
        this.socket.emit("sfu:produce", {
          roomCode: this.roomCode,
          transportId: transport.id,
          kind,
          rtpParameters
        }, (res) => res?.ok ? cb({ id: res.producerId }) : errb(new Error(res?.error || "produce failed")));
      });
    }

    return transport;
  }

  async _ensureSendTransport() {
    if (!this.sendTransport) this.sendTransport = await this._createTransport("send");
    return this.sendTransport;
  }
  async _ensureRecvTransport() {
    if (!this.recvTransport) this.recvTransport = await this._createTransport("recv");
    return this.recvTransport;
  }

  /**produce: 실제 송출 시작. Producer 핸들로 일시정지/재개, 트랙 교체(replaceTrack) 가능
replaceTrack: 장치 바꿀 때 유용 카메라 전환 등
sender.replaceTrack 를 직접 찾는 방식보다 안전 */
  async produce(kind /* "audio"|"video" */, track) {
    if (!track) throw new Error("track is required");
    if (!this.device?.canProduce(kind)) throw new Error(`device cannot produce ${kind}`);
    const sendTransport = await this._ensureSendTransport();
    const producer = await sendTransport.produce({ track });
    this._localTracks[kind] = track;
    this._producers[kind] = producer;

    // 프로듀서가 자체적으로 닫히면 참조 정리
    producer.on("transportclose", () => { this._producers[kind] = null; });
    producer.on("close", () => { this._producers[kind] = null; });

    return producer;
  }

  // 장치 교체: Producer 핸들로 안전하게 교체
  async replaceTrack(kind /* "audio"|"video" */, newTrack) {
    const p = this._producers[kind];
    if (p) {
      await p.replaceTrack({ track: newTrack });
      this._localTracks[kind] = newTrack;
      return p;
    }
    // 아직 produce 안 했으면 produce
    return this.produce(kind, newTrack);
  }

  pause(kind) {
    this._producers[kind]?.pause?.();
  }
  resume(kind) {
    this._producers[kind]?.resume?.();
  }

  /**서버가 consumerId/rtpParameters 를 넘겨줌 → 브라우저에서 consume() 호출
반환된 track 을 <audio>/<video>에 붙이면 재생됨. */
  async consume(producerId) {
    const recvTransport = await this._ensureRecvTransport();

    const ack = await new Promise((res) => {
      this.socket.emit("sfu:consume", {
        roomCode: this.roomCode,
        producerId,
        rtpCapabilities: this.device.rtpCapabilities,
        transportId: recvTransport.id
      }, res);
    });
    if (!ack?.ok) throw new Error(ack?.error || "sfu:consume failed");

    const consumer = await recvTransport.consume({
      id: ack.consumerId,
      producerId: ack.producerId,
      kind: ack.kind,
      rtpParameters: ack.rtpParameters
    });

    return { consumer, track: consumer.track, kind: consumer.kind };
  }

  // wifi → 5G 전환 같은 상황에서 ICE 다시 뿌리기 용 지금은 수동 트리거, 필요 시 자동화
  async restartIceAll() {
    if (this.sendTransport) {
      const ack = await new Promise((res) => {
        this.socket.emit("sfu:restart-ice", { roomCode: this.roomCode, transportId: this.sendTransport.id }, res);
      });
      if (ack?.ok) await this.sendTransport.restartIce({ iceParameters: ack.iceParameters });
    }
    if (this.recvTransport) {
      const ack2 = await new Promise((res) => {
        this.socket.emit("sfu:restart-ice", { roomCode: this.roomCode, transportId: this.recvTransport.id }, res);
      });
      if (ack2?.ok) await this.recvTransport.restartIce({ iceParameters: ack2.iceParameters });
    }
  }

  /**키 설계: socketId:kind 당 1개(오디오 1/비디오 1)만 유지 - 중복 소비 방지.
disconnect()는 모든 리소스 깔끔 해제 (트랙 stop, transport/prod/cons close) */
  registerRemoteConsumer(socketId, kind, consumer, track) {
    const key = `${socketId}:${kind}`;
    const prev = this.remoteConsumers.get(key);
    if (prev) {
      try { prev.consumer.close(); } catch { }
      try { prev.track?.stop?.(); } catch { }
    }
    this.remoteConsumers.set(key, { consumer, track });
  }

  _closePeerConsumers(socketId) {
    for (const key of [...this.remoteConsumers.keys()]) {
      if (!key.startsWith(socketId + ":")) continue;
      const entry = this.remoteConsumers.get(key);
      try { entry?.consumer?.close(); } catch { }
      try { entry?.track?.stop?.(); } catch { }
      this.remoteConsumers.delete(key);
    }
  }

  disconnect() {
    try { this.sendTransport?.close(); } catch { }
    try { this.recvTransport?.close(); } catch { }
    try {
      for (const { consumer, track } of this.remoteConsumers.values()) {
        try { consumer.close(); } catch { }
        try { track?.stop?.(); } catch { }
      }
      this.remoteConsumers.clear();
    } catch { }
    try { this._producers.audio?.close?.(); } catch { }
    try { this._producers.video?.close?.(); } catch { }
    this._producers = { audio: null, video: null };
    try { this.socket?.disconnect(); } catch { }
  }
}

export function createSfuClient(opts) {
  const sfu = new SfuClient(opts);
  sfu.connect();
  return sfu;
}
