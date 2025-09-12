<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, h, defineComponent, watch, nextTick } from "vue";
import { createSfuClient } from "@/util/sfuClient";

const props = defineProps({
  roomCode: { type: String, required: true },
  myPublisherId: { type: String, default: "" }
});

const RemoteVideo = defineComponent({
  name: "RemoteVideo",
  props: { stream: { type: Object, required: true } },
  setup(p) {
    const vref = ref(null);
    watch(() => p.stream, async (ms) => {
      await nextTick();
      if (vref.value && vref.value.srcObject !== ms) vref.value.srcObject = ms;
    }, { immediate: true });
    return () => h("video", { ref: vref, autoplay: true, playsinline: true, style: "width:100%;height:100%;background:#000;" });
  }
});

const participants = reactive(new Map()); // socketId -> { displayName, videoStream?, hasAudio }
const audioEls = new Map();               // socketId -> HTMLAudioElement
const consumed = new Set();               // producerId 중복 방지
let sfu = null;

// 🔇 기본 음소거로 시작
const speakersOn = ref(false);

function toggleSpeakers() {
  speakersOn.value = !speakersOn.value;
  for (const [, el] of audioEls) {
    el.muted = !speakersOn.value;
    if (speakersOn.value) el.play().catch(() => {});
  }
}

function isSelf(socketId) {
  if (sfu?.socket?.id && socketId === sfu.socket.id) return true;        // viewer 자신
  if (props.myPublisherId && socketId === props.myPublisherId) return true; // 내 publisher
  return false;
}

function ensureParticipant(socketId, displayName = "") {
  if (!participants.has(socketId)) {
    participants.set(socketId, { displayName: displayName || socketId, videoStream: null, hasAudio: false });
  } else if (displayName) {
    const p = participants.get(socketId);
    if (p.displayName !== displayName) p.displayName = displayName;
  }
}

function setVideo(socketId, track, displayName) {
  if (isSelf(socketId)) return;
  ensureParticipant(socketId, displayName);
  const p = participants.get(socketId);
  p.videoStream = track ? new MediaStream([track]) : null;
  participants.set(socketId, p);
}

// ✅ 기존 setAudio를 이 버전으로 “교체”하세요 (새로 추가 금지)
function setAudio(socketId, track, displayName) {
  if (isSelf(socketId)) return;
  ensureParticipant(socketId, displayName);
  const p = participants.get(socketId);
  p.hasAudio = !!track;
  participants.set(socketId, p);

  const prev = audioEls.get(socketId);
  if (!track) {
    if (prev) { try { prev.pause(); } catch {} prev.srcObject = null; audioEls.delete(socketId); }
    return;
  }
  let el = prev;
  if (!el) {
    el = new Audio();
    el.autoplay = true;
    el.playsInline = true;
    audioEls.set(socketId, el);
  }
  el.srcObject = new MediaStream([track]);
  el.muted = !speakersOn.value;            // 🔇 기본 음소거
  if (speakersOn.value) el.play().catch(() => {});
}

function removePeer(socketId) {
  participants.delete(socketId);
  const el = audioEls.get(socketId);
  if (el) { try { el.pause(); } catch {} el.srcObject = null; audioEls.delete(socketId); }
}

function handleProducerClosed({ socketId, kind }) {
  if (!participants.has(socketId)) return;
  const p = participants.get(socketId);
  if (kind === "video") p.videoStream = null;
  else if (kind === "audio") {
    p.hasAudio = false;
    const el = audioEls.get(socketId);
    if (el) { try { el.pause(); } catch {} el.srcObject = null; audioEls.delete(socketId); }
  }
  participants.set(socketId, p);
}

onMounted(async () => {
  sfu = createSfuClient({ roomCode: props.roomCode, displayName: "viewer" });

  sfu.onNewProducer = async ({ producerId, kind, socketId, displayName }) => {
    console.info("[viewer] onNewProducer", { producerId, kind, socketId, displayName, myPublisherId: props.myPublisherId, viewerId: sfu?.socket?.id });
    if (consumed.has(producerId)) return;
    if (isSelf(socketId)) return;

    try {
      const { consumer, track } = await sfu.consume(producerId);
      sfu.registerRemoteConsumer(socketId, kind, consumer, track);
      consumed.add(producerId);
      if (kind === "video") setVideo(socketId, track, displayName);
      else if (kind === "audio") setAudio(socketId, track, displayName);
    } catch (e) {
      console.warn("[viewer] consume(new) failed:", e);
    }
  };

  sfu.onPeerLeft = ({ socketId }) => {
    console.info("[viewer] peerLeft", socketId);
    removePeer(socketId);
  };
  sfu.socket.on("sfu:producer-closed", (p) => {
    console.info("[viewer] producerClosed", p);
    handleProducerClosed(p);
  });
  sfu.onRejoined = () => { console.info("[viewer] rejoined — clear consumed"); consumed.clear(); };

  const { existingProducers } = await sfu.join();
  console.info("[viewer] existingProducers", existingProducers);
  for (const p of existingProducers) {
    if (consumed.has(p.producerId)) continue;
    if (isSelf(p.socketId)) continue;
    try {
      const { consumer, track } = await sfu.consume(p.producerId);
      sfu.registerRemoteConsumer(p.socketId, p.kind, consumer, track);
      consumed.add(p.producerId);
      if (p.kind === "video") setVideo(p.socketId, track, p.displayName);
      else if (p.kind === "audio") setAudio(p.socketId, track, p.displayName);
    } catch (e) { console.warn("[viewer] consume(existing) failed:", e); }
  }
});

onBeforeUnmount(() => {
  try { sfu?.socket?.off?.("sfu:producer-closed", handleProducerClosed); } catch {}
  try { sfu?.disconnect(); } catch {}
  for (const [, el] of audioEls) { try { el.pause(); } catch {} el.srcObject = null; }
  audioEls.clear();
  participants.clear();
});
</script>

<template>
  <div class="panel">
    <h3>원격</h3>

    <div class="controls">
      <button class="btn" @click="toggleSpeakers">
        {{ speakersOn ? '🔊 소리 끄기' : '🔈 소리 켜기' }}
      </button>
    </div>

    <div class="grid">
      <div v-for="[socketId, p] in participants" :key="socketId" class="tile">
        <RemoteVideo v-if="p.videoStream" :stream="p.videoStream" />
        <div v-else class="avatar"><div class="initials">{{ p.displayName?.[0]?.toUpperCase() || "?" }}</div></div>
        <div class="label">{{ p.displayName }} <span v-if="p.hasAudio">🔊</span><span v-else>🔇</span></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel { padding:8px; border:1px solid #333; border-radius:12px; }
.controls{display:flex;gap:8px;margin-bottom:8px}
.btn{padding:6px 10px;border-radius:8px;background:#222;color:#fff;border:1px solid #444;cursor:pointer}
.grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(220px,1fr)); gap:12px; align-items:stretch; }
.tile { position:relative; aspect-ratio:16/9; background:#111; border-radius:12px; overflow:hidden; }
.label { position:absolute; left:8px; bottom:8px; font-size:12px; padding:4px 6px; background:rgba(0,0,0,.45); color:#fff; border-radius:6px; }
.avatar { width:100%; height:100%; display:flex; align-items:center; justify-content:center; background:#0f1216; }
.initials { width:64px; height:64px; border-radius:50%; display:flex; align-items:center; justify-content:center; background:#1f2630; color:#dfe6ef; font-weight:700; font-size:22px; }
</style>
