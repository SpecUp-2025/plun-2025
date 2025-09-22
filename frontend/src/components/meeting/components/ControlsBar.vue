<!-- src/components/meeting/components/ControlsBar.vue -->
<template>
  <footer class="controls-bar">
    <div class="controls-group">
      <!-- 마이크 토글 -->
      <button class="control-btn" :class="{ active: micOn, muted: !micOn }" @click="$emit('toggle-mic')" title="마이크">
        <div class="icon-container">
          <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M12 2c1.1 0 2 .9 2 2v6c0 1.1-.9 2-2 2s-2-.9-2-2V4c0-1.1.9-2 2-2zm-1 13c-1.1 0-2-.9-2-2h-2c0 1.7.8 3.2 2 4.1V19h6v-1.9c1.2-.9 2-2.4 2-4.1h-2c0 1.1-.9 2-2 2h-1z" />
          </svg>
          <div v-if="!micOn" class="slash-line"></div>
        </div>
      </button>

      <!-- 카메라 토글 -->
      <button class="control-btn" :class="{ active: camOn, muted: !camOn }" @click="$emit('toggle-cam')" title="카메라">
        <div class="icon-container">
          <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z" />
          </svg>
          <div v-if="!camOn" class="slash-line"></div>
        </div>
      </button>

      <!-- 스피커 토글 -->
      <button class="control-btn" :class="{ active: speakerOn, muted: !speakerOn }" @click="$emit('toggle-speaker')"
        title="스피커">
        <div class="icon-container">
          <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
          </svg>
          <div v-if="!speakerOn" class="slash-line"></div>
        </div>
      </button>

      <!-- 녹음 컨트롤 (호스트만) -->
      <RecordingControls :is-host="isHost" :is-idle="isIdle" :is-recording="isRecording" :is-paused="isPaused"
        :is-processing="isProcessing" @start-recording="$emit('start-recording')"
        @pause-recording="$emit('pause-recording')" @resume-recording="$emit('resume-recording')"
        @stop-recording="$emit('stop-recording')" />

      <!-- 회의 나가기 -->
      <button class="control-btn leave-control" @click="$emit('leave-room')" title="나가기">
        <div class="icon-container">
          <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
          </svg>
        </div>
      </button>
    </div>
  </footer>
</template>

<script setup>
import RecordingControls from './RecordingControls.vue'

defineProps({
  micOn: { type: Boolean, required: true },
  camOn: { type: Boolean, required: true },
  speakerOn: { type: Boolean, required: true },
  isHost: { type: Boolean, required: true, default: false },
  isIdle: { type: Boolean, required: true },
  isRecording: { type: Boolean, required: true },
  isPaused: { type: Boolean, required: true },
  isProcessing: { type: Boolean, required: true }
})

defineEmits([
  'toggle-mic',
  'toggle-cam',
  'toggle-speaker',
  'start-recording',
  'pause-recording',
  'resume-recording',
  'stop-recording',
  'leave-room'
])
</script>

<style scoped>
/* ========== 컨트롤 바 ========== */
.controls-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: #2a2a2a;
  border-top: 1px solid #3a3a3a;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
}

.controls-group {
  display: flex;
  gap: 20px;
  align-items: center;
}

.control-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: #0f0f0f;
  border: 2px solid #3a3a3a;
  border-radius: 50%;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.control-btn:hover:not(:disabled) {
  background: #1a1a1a;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.control-btn.active {
  background: #00a8ff;
  border-color: #00a8ff;
  color: white;
}

.control-btn.muted {
  background: #0f0f0f;
  border-color: #ff4757;
  color: #ffffff;
}

.leave-control {
  background: #ff4757;
  border-color: #ff4757;
  color: white;
}

.leave-control:hover {
  background: #e03e4c;
  border-color: #e03e4c;
}

.icon-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-icon {
  width: 24px;
  height: 24px;
}

.slash-line {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 32px;
  height: 2px;
  background: #ff4757;
  transform: translate(-50%, -50%) rotate(45deg);
  border-radius: 1px;
  z-index: 2;
}

/* ========== 반응형 ========== */
@media (max-width: 768px) {
  .controls-bar {
    padding: 16px 12px;
  }

  .controls-group {
    gap: 16px;
  }

  .control-btn {
    width: 50px;
    height: 50px;
  }

  .control-icon {
    width: 20px;
    height: 20px;
  }

  .slash-line {
    width: 28px;
  }
}

@media (max-width: 480px) {
  .controls-group {
    gap: 12px;
  }

  .control-btn {
    width: 44px;
    height: 44px;
  }

  .control-icon {
    width: 18px;
    height: 18px;
  }

  .slash-line {
    width: 24px;
  }
}
</style>