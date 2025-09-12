// @ts-check
import express from "express"; //REST 엔드포인트를 쉽게 만들려고 씀.
import http from "http"; //Node의 기본 HTTP 서버. Express 앱을 얹어 Socket.IO와 같이 쓰려고 직접 생성.
import { Server } from "socket.io"; //웹소켓 서버. 브라우저와 “실시간” 이벤트 송수신.
import * as mediasoup from "mediasoup"; //SFU 엔진. 브라우저의 WebRTC 트랙(오디오/비디오)을 서버에서 받아서 다른 참가자에게 중계

/*
JSDoc 주석(@type, @typedef, @property)
@type {T}
이 표현식(변수/값)의 타입은 TypeScript 타입이다
에디터가 worker를 mediasoup Worker로 인식해서 자동완성과 오타 검사
@typedef {…} MyType
타입 별명을 만드는 것(타입스크립트의 type MyType = …와 같은 역할).
보통 오브젝트 모양을 정의하고 싶을 때 @property와 함께 씀
@property {T} propName
오브젝트 타입 안의 필드들을 정의.
*/

/*
PORT: Socket.IO 신호 서버가 뜨는 포트(HTTP도 동일)       기본은 4000
CLIENT_ORIGIN: 브라우저 오리진을 CORS로 허용         기본은 Vite(5173).
IO_PATH: Socket.IO가 붙는 경로 클라에서도 path: /sfu로 맞춰야 함.
LISTEN_IP: 서버가 실제 NIC에 바인딩할 IP 보통 0.0.0.0.
ANNOUNCED_IP: 브라우저에게 알려줄 IP/도메인(공인 IP/도메인) NAT/프록시 환경에서 중요
WEBRTC_PORT: 단일 포트 모드(TCP/UDP 같은 포트 1개) 사용 시 포트 0이면 안 씀
RTC_MIN/RTC_MAX: 단일 포트를 안 쓸 때, WebRTC 미디어용 UDP 포트 범위

브라우저는 TURN이 아니라 SFU(=이 서버)로 붙음
그래서 SFU가 “어떤 IP/포트로” 자신을 알리는지가 중요 → announcedIp/포트 열기.
 */
const PORT = Number(process.env.PORT || 4000);
const CLIENT_ORIGIN = process.env.CLIENT_ORIGIN || "http://localhost:5173";
const IO_PATH = process.env.IO_PATH || "/sfu";
const LISTEN_IP = process.env.LISTEN_IP || "127.0.0.1";
const ANNOUNCED_IP = process.env.ANNOUNCED_IP || "127.0.0.1";
const WEBRTC_PORT = process.env.WEBRTC_PORT ? Number(process.env.WEBRTC_PORT) : 0;
const RTC_MIN = Number(process.env.RTC_MIN || 40000);
const RTC_MAX = Number(process.env.RTC_MAX || 49999);

/*
Express 앱 생성 + 헬스체크 엔드포인트(/healthz)
const server = http.createServer(app): Express를 HTTP 서버에 탑재.
new Server(server, {...}): Socket.IO 서버를 같은 HTTP 서버 위에 얹음.
path: IO_PATH → 클라에서도 동일하게 /sfu로 접속.
cors: 브라우저 오리진 허용.
 */
const app = express();
app.get("/healthz", (_req, res) => res.status(200).send("ok"));
const server = http.createServer(app);
const io = new Server(server, {
  path: IO_PATH,
  cors: { origin: CLIENT_ORIGIN, credentials: true },
});

/*
worker: mediasoup의 최상위 엔진 프로세스(스레드). 라우터/트랜스포트는 이 위에서 만들어짐.
webRtcServer: 단일 포트 모드를 쓸 때만 생성. 없으면 포트 범위 모드.
*/
/** @type {mediasoup.types.Worker} */
let worker;
/** @type {mediasoup.types.WebRtcServer | null} */
let webRtcServer = null;

/*
// roomCode -> { router, peers: Map<socketId, PeerState> }
router: 코덱/라우팅 정책을 가진 가상 스위치 같은 방의 모든 트랙이 여기를 돈다.
peers: 소켓ID → PeerState
transports: WebRTC 전송객체(send/recv).
producers: 이 피어가 업로드한 트랙(오디오/비디오).
consumers: 이 피어가 다운로드 중인 트랙.
Producer: “내 트랙을 서버로 올림”
Consumer: “남의 트랙을 서버에서 받아옴”
Transport: Producer/Consumer가 실려 다니는 파이프(ICE/DTLS 연결)
*/
const rooms = new Map();
/**
 * @typedef {Object} PeerState
 * @property {Map<string, mediasoup.types.WebRtcTransport>} transports
 * @property {Map<string, mediasoup.types.Producer>} producers
 * @property {Map<string, mediasoup.types.Consumer>} consumers
 * @property {mediasoup.types.RtpCapabilities | null} rtpCapabilities
 * @property {string} displayName
 */

/*
mediasoup의 워커(코어 엔진) 를 띄우는 단계
rtcMinPort/rtcMaxPort는 포트 범위 모드에서 사용할 UDP 포트 범위를 지정
이 범위에서 미디어 전송용 UDP 포트를 열어 사용
방화벽을 열어줄 때 이 범위를 열어줘야 함
*/
// 초기화(레이스 방지)
const workerReady = (async () => {
  worker = await mediasoup.createWorker({
    rtcMinPort: RTC_MIN,
    rtcMaxPort: RTC_MAX,
    logLevel: "warn",
    logTags: ["info", "ice", "dtls", "rtp", "srtp", "rtcp"],
  });

  //mediasoup 워커가 크래시로 죽었을 때 이벤트가 호출돼.
  //보통 이런 경우 프로세스를 종료, pm2나 systemd 같은 프로세스 매니저가 자동 재시작 하도록 안정화
  //워커가 죽은 상태로 서비스를 계속하면 연결이 다 깨져서 복구가 어려움
  // 워커 크래시 감시
  worker.on("died", () => {
    console.error("[SFU] mediasoup worker died. Exiting in 2s…");
    setTimeout(() => process.exit(1), 2000);
  });

  // 단일 포트 모드: WEBRTC_PORT를 지정하고 아래처럼 WebRtcServer를 만들면
  //UDP/TCP 같은 하나의 포트로만 미디어를 받게 됨
  //기업망/까다로운 방화벽 환경에서 열어야 할 포트가 하나라서 편함
  //포트 범위 모드: WebRtcServer를 안 만들면, rtcMinPort~rtcMaxPort 범위를 사용.
  //다양한 포트를 열어야 하지만, 기본/전통적 구성
  //어떤 모드든 announcedIp는 중요
  //브라우저가 ICE 후보를 만들 때 “외부에서 보이는 IP/도메인”이 무엇인가를 알려주는 값
  //로컬 테스트는 127.0.0.1, 실서비스는 공인 IP 또는 도메인
  if (WEBRTC_PORT > 0) {
    webRtcServer = await worker.createWebRtcServer({
      listenInfos: [
        { protocol: "udp", ip: LISTEN_IP, announcedIp: ANNOUNCED_IP, port: WEBRTC_PORT },
        { protocol: "tcp", ip: LISTEN_IP, announcedIp: ANNOUNCED_IP, port: WEBRTC_PORT },
      ],
    });
    console.log(`[SFU] WebRtcServer @ ${LISTEN_IP}:${WEBRTC_PORT} (announced ${ANNOUNCED_IP}:${WEBRTC_PORT})`);
  }

  console.log(
    `[SFU] mediasoup worker ready (mode=${
      webRtcServer ? `single-port:${WEBRTC_PORT}` : `port-range:${RTC_MIN}-${RTC_MAX}`
    })`
  );
})().catch((e) => {
  console.error("[SFU] worker init failed:", e);
  process.exit(1);
});

// ====== 유틸 ======
/*router에는 지원 코덱을 명시.
Opus(오디오), VP8(비디오), H.264(비디오) 추가 → 브라우저 호환성↑*/
async function getOrCreateRoom(roomCode) {
  await workerReady; 
  let room = rooms.get(roomCode);
  if (!room) {
    const router = await worker.createRouter({
      mediaCodecs: [
        { kind: "audio", mimeType: "audio/opus", clockRate: 48000, channels: 2 },
        { kind: "video", mimeType: "video/VP8", clockRate: 90000 },
        { kind: "video", mimeType: "video/H264", clockRate: 90000,
          parameters: { "packetization-mode": 1, "profile-level-id": "42e01f", "level-asymmetry-allowed": 1 }
        },
      ],
    });
    room = { router, peers: new Map() };
    rooms.set(roomCode, room);
    console.log(`[SFU] created router for room ${roomCode}`);
  }
  return room;
}
/*
단일 포트 모드면 webRtcServer 경유 아니면 listenIps로 직접 바인드
initialAvailableOutgoingBitrate: 서버가 초기에 어느 정도 대역으로 보낼지
announcedIp: NAT/프록시 환경에서 중요
ㄴ외부에서 접근할 때 브라우저에게 이 IP/도메인으로 ICE 후보를 써라
*/
async function createWebRtcTransport(router) {
  if (webRtcServer) {
    const t = await router.createWebRtcTransport({
      webRtcServer,
      enableUdp: true,
      enableTcp: true,
      preferUdp: true,
      initialAvailableOutgoingBitrate: 600000,
    });
    attachTransportLogs(t);
    return t;
  }
  const t = await router.createWebRtcTransport({
    listenIps: [{ ip: LISTEN_IP, announcedIp: ANNOUNCED_IP }],
    enableUdp: true,
    enableTcp: true,
    preferUdp: true,
    initialAvailableOutgoingBitrate: 600000,
  });
  attachTransportLogs(t);
  return t;
}

//DTLS/ICE 상태변화 로깅 연결 이슈 추적 시 유용
function attachTransportLogs(transport) {
  transport.on("dtlsstatechange", (s) => {
    if (s === "closed" || s === "failed") {
      console.warn("[SFU] DTLS state:", s, "closing transport", transport.id);
      try { transport.close(); } catch {}
    }
  });
  transport.on("icestatechange", (s) => {
    console.log("[SFU] ICE state:", s, "transport", transport.id);
  });
}

// ====== 소켓 이벤트 ======
io.on("connection", (socket) => {
  console.log("[SFU] socket connected:", socket.id);
  // 클라이언트가 Socket.IO로 연결되면 여기부터 시작.

  socket.on("sfu:join", async ({ roomCode, displayName }, cb) => {
    try {
      const room = await getOrCreateRoom(roomCode);
      socket.join(roomCode);

      /** @type {PeerState} */
      const peerState = {
        transports: new Map(),
        producers: new Map(),
        consumers: new Map(),
        rtpCapabilities: null,
        displayName: displayName || `user-${socket.id.slice(0, 5)}`,
      };
      room.peers.set(socket.id, peerState);
      //socket.join(roomCode): Socket.IO의 룸 기능(브로드캐스트 범위 관리)
      //PeerState 생성 후 등록

      const peers = [...room.peers.entries()]
        .filter(([id]) => id !== socket.id)
        .map(([id, p]) => ({ socketId: id, displayName: p.displayName }));
        //이미 접속해 있는 참가자 목록(자기 자신 제외)
      
      const existingProducers = [];
      for (const [id, p] of room.peers) {
        if (id === socket.id) continue;
        for (const [, producer] of p.producers) {
          existingProducers.push({
            producerId: producer.id,
            kind: producer.kind,
            socketId: id,
            displayName: p.displayName,
          });
        }
      }
      //이미 방에 있던 사람들의 업로드 트랙 목록, 새로 들어온 사람이 consume할 수 있게

      //브라우저는 rtpCapabilities를 받아서 mediasoup-client.Device를 load하는 데 씀
      const rtpCapabilities = room.router.rtpCapabilities;
      socket.to(roomCode).emit("sfu:peer-joined", { socketId: socket.id, displayName: peerState.displayName });
      cb?.({ ok: true, peers, rtpCapabilities, existingProducers });
      console.log(`[SFU] ${socket.id} joined ${roomCode} (${room.peers.size} peers)`);
    } catch (err) {
      console.error("sfu:join error:", err);
      cb?.({ ok: false, error: String(err) });
    }
  });

  // 브라우저가 rtpCapabilities를 받아 mediasoup-client Device를 초기화한 후 알려줌
  socket.on("sfu:set-rtp-capabilities", ({ roomCode, rtpCapabilities }) => {
    const room = rooms.get(roomCode);
    if (!room) return;
    const peer = room.peers.get(socket.id);
    if (peer) peer.rtpCapabilities = rtpCapabilities;
  });

  //브라우저는 이 옵션을 받아 RTCPeerConnection 내부 값을 세팅
  //send용 1개 recv용 1개 보통 총 2개 만듬
  socket.on("sfu:create-transport", async ({ roomCode, direction }, cb) => {
    try {
      const room = rooms.get(roomCode);
      if (!room) throw new Error("room not found");

      const transport = await createWebRtcTransport(room.router);
      const transportOptions = {
        id: transport.id,
        iceParameters: transport.iceParameters,
        iceCandidates: transport.iceCandidates,
        dtlsParameters: transport.dtlsParameters,
      };

      const peer = room.peers.get(socket.id);
      peer?.transports.set(transport.id, transport);

      cb?.({ ok: true, direction, transportOptions });
    } catch (err) {
      console.error("sfu:create-transport error:", err);
      cb?.({ ok: false, error: String(err) });
    }
  });

  //브라우저가 준 DTLS 파라미터로 서버쪽 트랜스포트를 연결.
  socket.on("sfu:connect-transport", async ({ roomCode, transportId, dtlsParameters }, cb) => {
    try {
      const room = rooms.get(roomCode);
      if (!room) throw new Error("room not found");
      const peer = room.peers.get(socket.id);
      const transport = peer?.transports.get(transportId);
      if (!transport) throw new Error("transport not found");

      await transport.connect({ dtlsParameters });
      cb?.({ ok: true });
    } catch (err) {
      console.error("sfu:connect-transport error:", err);
      cb?.({ ok: false, error: String(err) });
    }
  });

  //브라우저가 내 오디오/비디오 트랙을 올리겠다라고 요청
  //다른 사람들한테 새 producer 생김 상대방은 consume 가능
  socket.on("sfu:produce", async ({ roomCode, transportId, kind, rtpParameters }, cb) => {
    try {
      const room = rooms.get(roomCode);
      if (!room) throw new Error("room not found");
      const peer = room.peers.get(socket.id);
      const transport = peer?.transports.get(transportId);
      if (!transport) throw new Error("transport not found");

      const producer = await transport.produce({ kind, rtpParameters });
      peer?.producers.set(producer.id, producer);

      // 새 producer 알림
      socket.to(roomCode).emit("sfu:new-producer", {
        producerId: producer.id, kind, socketId: socket.id, displayName: peer?.displayName,
      });

      // 정리 & close 브로드캐스트(특정 트랙만 내려가는 상황 대응)
      producer.on("transportclose", () => { try { producer.close(); } catch {} });
      producer.on("close", () => {
        socket.to(roomCode).emit("sfu:producer-closed", {
          producerId: producer.id, socketId: socket.id, kind,
        });
      });

      cb?.({ ok: true, producerId: producer.id });
    } catch (err) {
      console.error("sfu:produce error:", err);
      cb?.({ ok: false, error: String(err) });
    }
  });

  //canConsume로 코덱 호환 체크 후 성공하면 consumer 생성
  socket.on("sfu:consume", async ({ roomCode, producerId, rtpCapabilities, transportId }, cb) => {
    try {
      const room = rooms.get(roomCode);
      if (!room) throw new Error("room not found");
      if (!room.router.canConsume({ producerId, rtpCapabilities })) {
        throw new Error("incompatible rtpCapabilities");
      }

      const peer = room.peers.get(socket.id);
      const transport = peer?.transports.get(transportId);
      if (!transport) throw new Error("transport not found");

      const consumer = await transport.consume({
        producerId, rtpCapabilities, paused: false,
      });
      peer?.consumers.set(consumer.id, consumer);
      consumer.on("transportclose", () => { try { consumer.close(); } catch {} });

      cb?.({
        ok: true,
        consumerId: consumer.id,
        kind: consumer.kind,
        rtpParameters: consumer.rtpParameters,
        producerId,
      });
    } catch (err) {
      console.error("sfu:consume error:", err);
      cb?.({ ok: false, error: String(err) });
    }
  });

  //네트워크 변경 시(와이파이→LTE 등) ICE 재시작으로 복원.
  socket.on("sfu:restart-ice", async ({ roomCode, transportId }, cb) => {
    try {
      const room = rooms.get(roomCode);
      if (!room) throw new Error("room not found");
      const peer = room.peers.get(socket.id);
      const transport = peer?.transports.get(transportId);
      if (!transport) throw new Error("transport not found");
      const iceParameters = await transport.restartIce();
      cb?.({ ok: true, iceParameters });
    } catch (err) {
      console.error("sfu:restart-ice error:", err);
      cb?.({ ok: false, error: String(err) });
    }
  });

  //disconnecting: 나 나감 알림
  //disconnect: 모든 자원 정리(Consumer/Producer/Transport). 방이 비면 Router도 닫고 방 Map에서 제거.
  socket.on("disconnecting", () => {
    for (const roomCode of socket.rooms) {
      if (rooms.has(roomCode)) {
        socket.to(roomCode).emit("sfu:peer-left", { socketId: socket.id });
      }
    }
  });

  socket.on("disconnect", () => {
    for (const [roomCode, room] of rooms) {
      const peer = room.peers.get(socket.id);
      if (!peer) continue;

      for (const [, c] of peer.consumers) { try { c.close(); } catch {} }
      for (const [, p] of peer.producers) { try { p.close(); } catch {} }
      for (const [, t] of peer.transports) { try { t.close(); } catch {} }

      room.peers.delete(socket.id);
      console.log(`[SFU] ${socket.id} left ${roomCode} (${room.peers.size} peers remain)`);

      if (room.peers.size === 0) {
        try { room.router.close(); } catch {}
        rooms.delete(roomCode);
        console.log(`[SFU] closed router for empty room ${roomCode}`);
      }
    }
  });
});

// 초기화 완료 후 리슨
workerReady.then(() => {
  server.listen(PORT, () => {
    console.log(`[SFU] listening on :${PORT}, path=${IO_PATH}, origin=${CLIENT_ORIGIN}`);
    if (WEBRTC_PORT > 0) {
      console.log(`[SFU] mode: single-port via WebRtcServer @ ${ANNOUNCED_IP}:${WEBRTC_PORT}`);
    } else {
      console.log(`[SFU] mode: port-range ${RTC_MIN}-${RTC_MAX} (listenIps announced=${ANNOUNCED_IP})`);
    }
  });
});
