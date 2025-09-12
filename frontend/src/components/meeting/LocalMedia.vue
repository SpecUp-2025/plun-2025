<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from "vue"
import { createSfuClient } from "@/util/sfuClient"

const props = defineProps({
  roomCode: { type: String, required: true }
})

const emit = defineEmits(["publisher-id", "status-change"])

const localVideoRef = ref(null)
const status = ref("idle")                      // idle | recording | paused | finishing
const log = ref("")
const seq = ref(0)
const startEpochMs = ref(0)                    // Date.now() 기준 타임스탬프
const roomNo = ref(0)

const micOn = ref(true)
const camOn = ref(true)
const mirror = ref(true)

let localStream = null                         // UI 미리보기/송출용 (audio+video)
let running = false
let pauseRequested = false

let sfu = null
let published = false

async function resolveRoomNo() {
  const m = props.roomCode.match(/\d+/)
  roomNo.value = m ? parseInt(m[0], 10) : 101
}

/* ----- 녹음 유틸 ----- */

// 오디오 전용 MediaStream을 만든다 (비디오 트랙 제거)
function buildAudioOnlyStream() {
  const at = localStream?.getAudioTracks?.()[0]
  if (!at) throw new Error("no audio track")
  // 녹음 안정성을 위해 clone 사용 (UI mute 등의 영향 최소화)
  const recTrack = at.clone()
  // 원하는 정책: 마이크 버튼이 off면 녹음도 무음이 되게 하려면 아래 라인 주석 처리
  recTrack.enabled = true
  return new MediaStream([recTrack])
}

function pickMimeType() {
  const CAND = [
    "audio/webm;codecs=opus",
    "audio/webm;codecs=vorbis",
    "audio/ogg;codecs=opus",
    "audio/ogg",
    "audio/webm",
  ]
  for (const t of CAND) {
    if (window.MediaRecorder && MediaRecorder.isTypeSupported(t)) return t
  }
  return "" // 브라우저가 알아서 기본값 선택
}

function recordBlob(ms = 8000) {
  return new Promise((resolve, reject) => {
    try {
      const recStream = buildAudioOnlyStream()
      const mimeType = pickMimeType()
      const opts = mimeType ? { mimeType, audioBitsPerSecond: 128000 } : { audioBitsPerSecond: 128000 }
      const mr = new MediaRecorder(recStream, opts)

      const parts = []
      mr.ondataavailable = e => { if (e.data && e.data.size) parts.push(e.data) }
      mr.onerror = e => reject(e.error || e)
      mr.onstop = () => {
        try { recStream.getTracks().forEach(t => t.stop()) } catch {}
        resolve(new Blob(parts, { type: mr.mimeType || mimeType || "audio/webm" }))
      }

      // 일부 브라우저에서 start(0) 이 안정적인 경우가 있어 0을 명시
      mr.start(0)
      setTimeout(() => {
        try { mr.stop() } catch (e) { reject(e) }
      }, ms)
    } catch (err) {
      reject(err)
    }
  })
}

async function uploadChunk(blob, s) {
  const form = new FormData()
  form.append("file", blob, `chunk-${s}.webm`)
  form.append("room_no", roomNo.value)
  form.append("user_no", 1) // TODO: 실제 로그인 사용자 번호로 교체
  form.append("seq", s)
  form.append("ts_ms", startEpochMs.value + s * 8000) // 시작 시각 + 청크 간격(8s)
  const res = await fetch("/stt/chunk_raw", { method: "POST", body: form })
  if (!res.ok) throw new Error(await res.text())
}

/* ----- 업로드 루프 ----- */
async function loop() {
  while (running) {
    const s = seq.value++
    try {
      const blob = await recordBlob(8000)
      await uploadChunk(blob, s)
      log.value += `+ 청크 ${s} 업로드\n`
    } catch (e) {
      log.value += `! 업로드 실패: ${String(e)}\n`
    }
    if (pauseRequested) {
      running = false
      pauseRequested = false
      status.value = "paused"
      emit("status-change", status.value)
      log.value += "⏸ 업로드 중지됨(재시작 가능)\n"
      break
    }
    await new Promise(r => setTimeout(r, 10))
  }
}

/* ----- 버튼 ----- */
async function startRecording() {
  if (status.value === "recording") return
  await resolveRoomNo()

  if (!localStream) {
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: { channelCount: 1, sampleRate: 48000, noiseSuppression: true, echoCancellation: true, autoGainControl: true },
      video: true
    })
    await nextTick()
    if (localVideoRef.value) localVideoRef.value.srcObject = localStream
  }

  const at = localStream?.getAudioTracks?.()[0]
  const vt = localStream?.getVideoTracks?.()[0]
  if (at) at.enabled = micOn.value
  if (vt) vt.enabled = camOn.value

  if (status.value === "idle") {
    seq.value = 0
    startEpochMs.value = Date.now()
  }

  running = true
  status.value = "recording"
  emit("status-change", status.value)
  log.value += `${seq.value === 0 ? "🎙 녹음 시작" : "▶️ 녹음 재개"} (8초 단위 업로드)\n`
  loop()
}

function pauseRecording() {
  if (status.value !== "recording") return
  pauseRequested = true
  log.value += "⏸ 중지 요청… 현재 청크 마무리 중\n"
}

async function finishRecording() {
  if (status.value === "finishing") return
  pauseRequested = true
  running = false
  status.value = "finishing"
  emit("status-change", status.value)
  log.value += "⏹ 종료 요청… 전사 시작\n"

  setTimeout(async () => {
    try {
      const res = await fetch(`/stt/finalize_room?room_no=${roomNo.value}`, { method: "POST" })
      const t = await res.text()
      log.value += `— 전사 응답 —\n${t}\n`
    } catch (e) {
      log.value += `! 전사 실패: ${String(e)}\n`
    } finally {
      status.value = "idle"
      emit("status-change", status.value)
      seq.value = 0
      startEpochMs.value = 0
    }
  }, 300)
}

function toggleMic() {
  micOn.value = !micOn.value
  const t = localStream?.getAudioTracks?.()[0]
  if (t) t.enabled = micOn.value
}
function toggleCam() {
  camOn.value = !camOn.value
  const t = localStream?.getVideoTracks?.()[0]
  if (t) t.enabled = camOn.value
}

/* ----- SFU publish (변경 없음) ----- */
async function startSfuPublish() {
  sfu = createSfuClient({ roomCode: props.roomCode, displayName: "publisher" })
  await sfu.join()
  emit("publisher-id", sfu.socket.id)

  if (!published) {
    const at = localStream?.getAudioTracks?.()[0]
    const vt = localStream?.getVideoTracks?.()[0]
    if (at) await sfu.produce("audio", at)
    if (vt) await sfu.produce("video", vt)
    published = true
  }

  sfu.onRejoined = async () => {
    try {
      const at = localStream?.getAudioTracks?.()[0]
      const vt = localStream?.getVideoTracks?.()[0]
      if (at) await sfu.produce("audio", at)
      if (vt) await sfu.produce("video", vt)
    } catch {}
  }
}

onMounted(async () => {
  await resolveRoomNo()
  try {
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true, channelCount: 1, sampleRate: 48000 },
      video: true
    })
    if (localVideoRef.value) localVideoRef.value.srcObject = localStream
  } catch (e) {
    log.value += `마이크/카메라 오류: ${String(e)}\n`
  }

  try {
    await startSfuPublish()
    log.value += "🛰 SFU publish 시작(내 오디오/비디오 업로드)\n"
  } catch (e) {
    log.value += `SFU publish 실패: ${String(e)}\n`
  }
})

onBeforeUnmount(() => {
  try { sfu?.disconnect() } catch {}
  try { localStream?.getTracks().forEach(t => t.stop()) } catch {}
  running = false
  pauseRequested = false
})

// 상위에서 제어하고 싶으면 사용
defineExpose({ startRecording, pauseRecording, finishRecording })
</script>

<template>
  <div class="panel">
    <h3>로컬</h3>
    <div class="tile">
      <video
        ref="localVideoRef"
        autoplay
        playsinline
        muted
        :style="{ width:'100%', height:'100%', background:'#000', transform: mirror ? 'scaleX(-1)' : 'none' }"
      />
      <div class="label">local</div>
    </div>

    <div class="controls">
      <button class="btn" @click="toggleMic">{{ micOn ? '🔇 마이크 끄기' : '🎙 마이크 켜기' }}</button>
      <button class="btn" @click="toggleCam">{{ camOn ? '📷 카메라 끄기' : '📷 카메라 켜기' }}</button>

      <button class="btn primary" :disabled="status==='recording' || status==='finishing'" @click="startRecording">
        {{ status==='paused' ? '▶️ 녹음 재개' : '🎙 녹음 시작' }}
      </button>
      <button class="btn" :disabled="status!=='recording'" @click="pauseRecording">⏸ 중지</button>
      <button class="btn danger" :disabled="status==='finishing' || status==='idle'" @click="finishRecording">🛑 종료(전사)</button>
    </div>

    <h4>로그</h4>
    <pre class="log">{{ log }}</pre>
  </div>
</template>

<style scoped>
.panel { padding: 8px; border:1px solid #333; border-radius:12px; }
.tile { position:relative; aspect-ratio:16/9; background:#111; border-radius:12px; overflow:hidden; }
.label { position:absolute; left:8px; bottom:8px; font-size:12px; padding:4px 6px; background:rgba(0,0,0,.45); color:#fff; border-radius:6px; }
.controls { display:flex; gap:8px; margin:8px 0; flex-wrap: wrap; }
.btn { padding:6px 10px; border-radius:8px; background:#222; color:#fff; border:1px solid #444; cursor:pointer; }
.btn.primary { background:#0b5; border-color:#0a4; }
.btn.danger { background:#7a1f1f; border-color:#a22; }
.log { white-space:pre-wrap; background:#0b0b0b; color:#ddd; padding:8px; border-radius:8px; min-height:120px; }
</style>
