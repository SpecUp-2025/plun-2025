<!-- src/components/meeting/components/RecordingControls.vue -->
<template>
  <div v-if="isHost" class="recording-controls">
    <!-- 녹음 시작 버튼 -->
    <button v-if="isIdle" 
            class="control-btn recording-btn" 
            @click="$emit('start-recording')" 
            title="녹음 시작">
      <div class="icon-container">
        <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="12" r="8" />
        </svg>
      </div>
    </button>

    <!-- 녹음 일시정지 버튼 -->
    <button v-if="isRecording" 
            class="control-btn recording-btn active" 
            @click="$emit('pause-recording')" 
            title="녹음 일시정지">
      <div class="icon-container">
        <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="4" width="4" height="16" />
          <rect x="14" y="4" width="4" height="16" />
        </svg>
      </div>
    </button>

    <!-- 녹음 재개 버튼 -->
    <button v-if="isPaused" 
            class="control-btn recording-btn paused" 
            @click="$emit('resume-recording')" 
            title="녹음 재개">
      <div class="icon-container">
        <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
          <polygon points="8,5 19,12 8,19" />
        </svg>
      </div>
    </button>

    <!-- 녹음 종료 버튼 -->
    <button v-if="!isIdle && !isProcessing" 
            class="control-btn recording-stop-btn" 
            @click="$emit('stop-recording')" 
            title="녹음 종료">
      <div class="icon-container">
        <svg class="control-icon" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" />
        </svg>
      </div>
    </button>

    <!-- 처리 중 상태 -->
    <button v-if="isProcessing" 
            class="control-btn" 
            disabled 
            title="처리 중">
      <div class="icon-container">
        <div class="loading-spinner small"></div>
      </div>
    </button>
  </div>
</template>

<script setup>
defineProps({
  isHost: { type: Boolean, required: true, default: false },
  isIdle: { type: Boolean, required: true, default: true },
  isRecording: { type: Boolean, required: true, default: false },
  isPaused: { type: Boolean, required: true, default: false },
  isProcessing: { type: Boolean, required: true, default: false }
})

defineEmits(['start-recording', 'pause-recording', 'resume-recording', 'stop-recording'])
</script>

<style scoped>
.recording-controls {
  display: flex;
  gap: 12px;
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

.recording-btn {
  background: #2ed573;
  border-color: #2ed573;
  color: white;
}

.recording-btn.active {
  background: #ff4757;
  border-color: #ff4757;
}

.recording-btn.paused {
  background: #ffa502;
  border-color: #ffa502;
}

.recording-stop-btn {
  background: #ff4757;
  border-color: #ff4757;
  color: white;
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

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #3a3a3a;
  border-top: 2px solid #00a8ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ========== 반응형 ========== */
@media (max-width: 768px) {
  .recording-controls {
    gap: 8px;
  }

  .control-btn {
    width: 50px;
    height: 50px;
  }

  .control-icon {
    width: 20px;
    height: 20px;
  }
}

@media (max-width: 480px) {
  .recording-controls {
    gap: 6px;
  }

  .control-btn {
    width: 44px;
    height: 44px;
  }

  .control-icon {
    width: 18px;
    height: 18px;
  }
}
</style>