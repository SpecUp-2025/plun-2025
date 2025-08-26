<script setup>
/**
URL의 :roomCode와 로그인 유저정보를 읽음
백엔드 /meeting-rooms/{roomCode}/authz로 제목/권한 체크 → 제목 표시
카메라/마이크 권한 받아 로컬 스트림 만들고 <video muted>로 미리보기
sfuClient로 신호 연결 → join() → 기존 참가자들의 producer 목록으로 즉시 consume
내 오디오/비디오 Track을 produce 해서 방에 업로드
새로운 원격 producer가 생기면 onNewProducer 이벤트로 consume
UX 버튼으로 mic/cam track.enabled 토글, 스피커 mute 토글
탭을 다시 볼 때 restartIceAll()로 ICE 재시작(네트워크 바뀐 경우 대비)
나가거나 언마운트되면 로그 기록, 스트림/소켓 정리

onMounted()
fetchRoomMeta()로 방 제목 가져옴
getLocalStreamWithPrefs()로 로컬 미디어 열기 -- <video muted>에 연결
sfu = createSfuClient(...) 생성
SFU 이벤트 핸들러 설치 (onNewProducer, onPeerLeft, sfu:producer-closed)
document.visibilitychange에 ICE 재시작 훅
sfu.join() → existingProducers 순회하며 consume
내 오디오/비디오 produce()
토글 상태 반영

나가기 버튼, onBeforeUnmount
이벤트 해제
로컬 트랙 정지 / 원격 오디오 정리
sfu.disconnect()
logLeave() 전송
 */
import { ref, computed, onMounted, onBeforeUnmount, defineComponent, h, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/store/userStore";
import instance from "@/util/interceptors";
import { createSfuClient } from "@/util/sfuClient";

/* (렌더 함수)
RemoteVideo가 하는 일
역할: 원격 참가자 한 명의 비디오 타일 렌더.
입력(props):stream(필수, MediaStream) + label(옵션, 표시 텍스트)
동작:<video>에 대한 ref(vref)를 잡음.
watch(props.stream) → 값이 바뀌면 nextTick() 후 vref.value.srcObject = stream 대입.
이미 같은 스트림이면 다시 대입하지 않음(깜빡임 방지).
렌더 함수로 <div class="tile"> 안에 <video autoplay playsinline> + 라벨을 그림.
srcObject는 바인딩 불가: <video :src="...">로는 MediaStream 못 꽂음 → JS로 직접 video.srcObject = stream 필요 → 그래서 ref + watch.
nextTick() 필요: DOM이 실제로 만들어진 뒤에 srcObject를 대입해야 안전.
렌더 함수 사용 이유: 아주 작은 컴포넌트라 템플릿 없이도 간단하게 수명주기/로직 캡슐화.
*/
const RemoteVideo = defineComponent({
  name: "RemoteVideo",
  props: { stream: { type: Object, required: true }, label: { type: String, default: "" } },
  setup(props) {
    const vref = ref(null);
    watch(
      () => props.stream,
      async (ms) => {
        await nextTick();
        if (vref.value && vref.value.srcObject !== ms) vref.value.srcObject = ms;
      },
      { immediate: true }
    );
    return () =>
      h("div", { class: "tile" }, [
        h("video", { ref: vref, autoplay: true, playsinline: true, style: "width:100%;height:100%;background:#000;" }),
        h("div", { class: "label" }, props.label)
      ]);
  }
});

/*  라우팅/사용자 컨텍스트  */
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const roomCode = computed(() => String(route.params.roomCode || ""));
const displayName = computed(() => userStore.user?.name ?? "");
const userNo = computed(() => Number(userStore.user?.userNo ?? 0));

/* 회의 메타/로그  */
const roomTitle = ref("");
const joinedAt = ref(null);
const leftAt = ref(null);
let enterLogged = false;

async function fetchRoomMeta() {
  try {
    const { data } = await instance.get(
      `/meeting-rooms/${encodeURIComponent(roomCode.value)}/authz`,
      { params: { userNo: userNo.value } }
    );
    roomTitle.value = data?.title || roomCode.value;
  } catch {
    roomTitle.value = roomCode.value;
  }
}

async function logEnter() {
  if (!userNo.value) return;
  try {
    await instance.post(`/meeting-rooms/${encodeURIComponent(roomCode.value)}/enter`, {
      userNo: userNo.value,
      joinedAt: joinedAt.value?.toISOString()
    });
    enterLogged = true;
  } catch {}
}

async function logLeave() {
  if (!userNo.value) return;
  try {
    await instance.post(`/meeting-rooms/${encodeURIComponent(roomCode.value)}/leave`, {
      userNo: userNo.value,
      joinedAt: joinedAt.value?.toISOString(),
      leftAt: leftAt.value?.toISOString()
    });
  } catch {}
}

/*  상태  */
const localVideoRef = ref(null);
const mirror = ref(localStorage.getItem("pref.mirror") === "true");

const remoteVideoTracks = ref(new Map());  // Map<key, { stream, socketId, kind, displayName }>
const remoteAudioEls = new Map();          // Map<socketId, HTMLAudioElement>

let localStream;
let sfu;

// UX 토글
const audioGate = ref(true);
const micOn = ref(true);
const camOn = ref(true);
const speakersOn = ref(true);

// 프리조인에서 저장된 선택값
const prefAudioId = localStorage.getItem("pref.audioId") || "";
const prefVideoId = localStorage.getItem("pref.videoId") || "";
const prefSpeakerId = localStorage.getItem("pref.speakerId") || "";

/*  원격 미디어 관리  */
function setRemoteVideo(socketId, kind, track, label) {
  const key = `${socketId}:${kind}`;
  const stream = new MediaStream([track]);
  remoteVideoTracks.value.set(key, { stream, socketId, kind, displayName: label || socketId });
  remoteVideoTracks.value = new Map(remoteVideoTracks.value);
}

function setRemoteAudio(socketId, track) {
  // 자기 오디오는 절대 재생하지 않음(서버도 본인에게 broadcast 안하지만 안전망)
  if (sfu?.socket?.id && socketId === sfu.socket.id) return;
  const ms = new MediaStream([track]);
  let audio = remoteAudioEls.get(socketId);
  if (!audio) {
    audio = new Audio();
    audio.autoplay = true;
    audio.playsInline = true;
    remoteAudioEls.set(socketId, audio);
  }
  audio.srcObject = ms;
  audio.muted = !speakersOn.value;
  if (prefSpeakerId && typeof audio.setSinkId === "function") {
    audio.setSinkId(prefSpeakerId).catch(() => {});
  }
  audio.play().catch(() => { /* “소리 켜기” 버튼으로 재시도 */ });
}

function removePeerMedia(socketId) {
  let changed = false;
  for (const key of [...remoteVideoTracks.value.keys()]) {
    if (!key.startsWith(socketId + ":")) continue;
    remoteVideoTracks.value.delete(key);
    changed = true;
  }
  if (changed) remoteVideoTracks.value = new Map(remoteVideoTracks.value);

  const audio = remoteAudioEls.get(socketId);
  if (audio) {
    try { audio.pause(); } catch {}
    try { audio.srcObject = null; } catch {}
    remoteAudioEls.delete(socketId);
  }
}

// 개별 producer 종료(상대가 카메라/마이크 끄는 경우 UI 반영)
function handleProducerClosed({ socketId, kind }) {
  if (kind === "video") {
    const key = `${socketId}:video`;
    if (remoteVideoTracks.value.has(key)) {
      remoteVideoTracks.value.delete(key);
      remoteVideoTracks.value = new Map(remoteVideoTracks.value);
    }
  } else if (kind === "audio") {
    const audio = remoteAudioEls.get(socketId);
    if (audio) {
      try { audio.pause(); } catch {}
      try { audio.srcObject = null; } catch {}
      remoteAudioEls.delete(socketId);
    }
  }
}

function enableRemoteAudio() {
  for (const [, audio] of remoteAudioEls) audio.play().catch(() => {});
  audioGate.value = false;
}
function toggleMic() {
  micOn.value = !micOn.value;
  const t = localStream?.getAudioTracks()?.[0];
  if (t) t.enabled = micOn.value;
}
function toggleCam() {
  camOn.value = !camOn.value;
  const t = localStream?.getVideoTracks()?.[0];
  if (t) t.enabled = camOn.value;
}
function toggleSpeakers() {
  speakersOn.value = !speakersOn.value;
  for (const [, audio] of remoteAudioEls) {
    audio.muted = !speakersOn.value;
    if (speakersOn.value) audio.play().catch(() => {});
  }
}

/* 로컬 스트림: 프리조인 선택값 + 기본 폴백 */
async function getLocalStreamWithPrefs() {
  const audio =
    prefAudioId
      ? { deviceId: { exact: prefAudioId }, echoCancellation: true, noiseSuppression: true, autoGainControl: true }
      : { echoCancellation: true, noiseSuppression: true, autoGainControl: true };
  const video = prefVideoId ? { deviceId: { exact: prefVideoId } } : true;
  try {
    return await navigator.mediaDevices.getUserMedia({ audio, video });
  } catch {
    return await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      video: true
    });
  }
}

/* 라이프사이클 */
function onVisChange() {
  // 탭 복귀 시 네트워크 경로 바뀌었을 수 있으니 ICE 재시작(있으면)
  if (document.visibilityState === "visible") {
    sfu?.restartIceAll?.().catch(() => {});
  }
}

onMounted(async () => {
  await fetchRoomMeta();

  // 1) 로컬 미디어
  localStream = await getLocalStreamWithPrefs();
  if (localVideoRef.value) localVideoRef.value.srcObject = localStream;

  // 2) SFU 연결
  sfu = createSfuClient({ roomCode: roomCode.value, displayName: displayName.value });

  // 상대 트랙 생성/종료 처리
  sfu.onNewProducer = async ({ producerId, kind, socketId, displayName }) => {
    try {
      const { consumer, track } = await sfu.consume(producerId);
      sfu.registerRemoteConsumer(socketId, kind, consumer, track);
      if (kind === "video") setRemoteVideo(socketId, kind, track, displayName);
      else if (kind === "audio") setRemoteAudio(socketId, track);
    } catch (e) {
      console.warn("[SFU] consume(new) failed:", e);
    }
  };
  sfu.onPeerLeft = ({ socketId }) => removePeerMedia(socketId);
  sfu.onRejoined = async () => { /* 프리조인 선택값 유지 */ };

  // 서버가 알려주는 개별 producer 종료 이벤트 → UI에서 타일/오디오 제거
  sfu.socket.on("sfu:producer-closed", handleProducerClosed);

  // 탭 복귀 시 ICE 재시작
  document.addEventListener("visibilitychange", onVisChange);

  // 3) 조인 + 기존 producer
  const { existingProducers } = await sfu.join();
  joinedAt.value = new Date();
  if (!enterLogged) { logEnter().catch(() => {}); }

  for (const p of existingProducers) {
    try {
      const { consumer, track } = await sfu.consume(p.producerId);
      sfu.registerRemoteConsumer(p.socketId, p.kind, consumer, track);
      if (p.kind === "video") setRemoteVideo(p.socketId, p.kind, track, p.displayName);
      else if (p.kind === "audio") setRemoteAudio(p.socketId, track);
    } catch (e) {
      console.warn("[SFU] consume(existing) failed:", e);
    }
  }

  // 4) 업로드(로컬)
  const [audioTrack] = localStream.getAudioTracks();
  const [videoTrack] = localStream.getVideoTracks();
  if (audioTrack) await sfu.produce("audio", audioTrack);
  if (videoTrack) await sfu.produce("video", videoTrack);

  if (!micOn.value && audioTrack) audioTrack.enabled = false;
  if (!camOn.value && videoTrack) videoTrack.enabled = false;
});

onBeforeUnmount(async () => {
  // 이벤트 해제
  document.removeEventListener("visibilitychange", onVisChange);
  try { sfu?.socket?.off?.("sfu:producer-closed", handleProducerClosed); } catch {}

  try { localStream?.getTracks().forEach(t => t.stop()); } catch {}
  try { sfu?.disconnect(); } catch {}
  for (const [, audio] of remoteAudioEls) {
    try { audio.pause(); } catch {}
    try { audio.srcObject = null; } catch {}
  }
  if (!leftAt.value) leftAt.value = new Date();
  if (enterLogged) await logLeave().catch(() => {});
});

// “나가기”
async function leaveMeeting() {
  leftAt.value = new Date();
  try { await logLeave(); } catch {}
  try { sfu?.disconnect(); } catch {}
  try { localStream?.getTracks().forEach(t => t.stop()); } catch {}
  router.back();
}
</script>

<template>
  <div class="room">
    <div class="header">
      <div class="title">
        <strong>{{ roomTitle || roomCode }}</strong>
        <span class="code">#{{ roomCode }}</span>
      </div>
      <div class="right">
        <button class="btn danger" @click="leaveMeeting">나가기</button>
      </div>
    </div>

    <div class="controls">
      <button v-if="audioGate" @click="enableRemoteAudio" class="btn">🔈 소리 켜기</button>
      <button @click="toggleMic" class="btn">{{ micOn ? '🔇 마이크 끄기' : '🎙 마이크 켜기' }}</button>
      <button @click="toggleCam" class="btn">{{ camOn ? '📷 카메라 끄기' : '📷 카메라 켜기' }}</button>
      <button @click="toggleSpeakers" class="btn">{{ speakersOn ? '🎧 헤드셋 음소거' : '🎧 헤드셋 해제' }}</button>
    </div>

    <div class="videos">
      <!-- 로컬 -->
      <div class="tile">
        <video
          ref="localVideoRef"
          autoplay
          playsinline
          muted
          :style="{ width: '100%', height: '100%', background: '#000', transform: mirror ? 'scaleX(-1)' : 'none' }"
        ></video>
        <div class="label">{{ displayName }} (local)</div>
      </div>

      <!-- 원격 비디오 타일 -->
      <RemoteVideo
        v-for="[key, item] in remoteVideoTracks"
        :key="key"
        :stream="item.stream"
        :label="`${item.displayName} (${item.kind})`"
      />
    </div>
  </div>
</template>

<style scoped>
.room { padding: 12px; }

.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.title { display: flex; align-items: center; gap: 8px; }
.title .code { color: #888; font-size: 12px; }
.right .btn.danger { background: #7a1f1f; border-color: #a22; }

.controls { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; }
.btn { padding: 6px 10px; border-radius: 8px; background: #222; color: #fff; border: 1px solid #444; cursor: pointer; }
.btn:hover { background: #333; }

.videos { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; align-items: stretch; }
.tile { position: relative; aspect-ratio: 16/9; background: #111; border-radius: 12px; overflow: hidden; }
.label { position: absolute; left: 8px; bottom: 8px; font-size: 12px; padding: 4px 6px; background: rgba(0,0,0,.45); color: #fff; border-radius: 6px; }
</style>
