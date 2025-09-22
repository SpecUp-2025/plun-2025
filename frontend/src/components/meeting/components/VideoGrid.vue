
<!-- src/components/meeting/components/VideoGrid.vue -->
<template>
  <div class="video-grid" :style="{
    '--grid-cols': gridConfig.cols,
    '--grid-rows': gridConfig.rows
  }">
    <div v-for="participant in participants" :key="participant.id" 
         class="video-tile" :class="{ 'self-tile': participant.isSelf }">
      
      <div class="video-container">
        <!-- 로컬 비디오 (본인, 카메라 켜진 경우) -->
        <video v-if="participant.isSelf && participant.hasVideo" 
               :ref="setLocalVideoRef" 
               class="video"
               :class="{ mirror: mirror }" 
               autoplay 
               playsinline 
               muted />

        <!-- 원격 비디오 (다른 참가자, 비디오 스트림 있는 경우) -->
        <video v-else-if="!participant.isSelf && participant.hasVideo && participant.videoStream" 
               :ref="el => setRemoteVideo(el, participant)" 
               class="video" 
               autoplay 
               playsinline 
               muted />

        <!-- 아바타 (카메라 오프 상태) -->
        <div v-else class="avatar">
          <div class="avatar-circle">
            {{ (participant.name || '?').charAt(0).toUpperCase() }}
          </div>
          <div class="camera-off-icon">📹</div>
        </div>
      </div>

      <!-- 참가자 정보 -->
      <div class="name-badge">
        <span class="name">{{ participant.name }}</span>
        <div class="status-icons">
          <span v-if="!participant.hasAudio" class="mic-off">🎤</span>
          <span v-if="participant.isSelf" class="self-indicator">나</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  participants: { type: Array, required: true },
  mirror: { type: Boolean, default: false }
})

const emit = defineEmits(['setLocalVideoRef'])

/**
 * 참가자 수에 따른 동적 그리드 레이아웃 계산
 * CSS Grid의 columns/rows 값을 반환
 */
const gridConfig = computed(() => {
  const count = props.participants.length
  if (count === 1) return { cols: 1, rows: 1 }
  if (count === 2) return { cols: 2, rows: 1 }
  if (count <= 4) return { cols: 2, rows: 2 }
  if (count <= 6) return { cols: 3, rows: 2 }
  if (count <= 9) return { cols: 3, rows: 3 }
  return { cols: 4, rows: Math.ceil(count / 4) }
})

/**
 * 로컬 비디오 요소 참조를 부모로 전달
 */
function setLocalVideoRef(el) {
  emit('setLocalVideoRef', el)
}

/**
 * 원격 비디오 요소에 MediaStream 연결
 */
function setRemoteVideo(el, participant) {
  if (el && participant.videoStream) {
    el.srcObject = participant.videoStream
    el.play().catch(() => {}) // autoplay 실패 무시
  }
}
</script>

<style scoped>
/* ========== 비디오 그리드 ========== */
.video-grid {
  display: grid;
  grid-template-columns: repeat(var(--grid-cols), 1fr);
  grid-template-rows: repeat(var(--grid-rows), 1fr);
  gap: 12px;
  width: 100%;
  height: 100%;
  max-width: 1400px;
  max-height: 900px;
}

/* ========== 비디오 타일 ========== */
.video-tile {
  position: relative;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  transition: all 0.2s ease;
  border: 2px solid transparent;
  min-height: 200px;
}

.self-tile {
  border-color: #00a8ff;
}

.video-tile:hover {
  transform: scale(1.02);
}

.video-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
}

.video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.mirror {
  transform: scaleX(-1);
}

/* ========== 아바타 ========== */
.avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2c3e50, #34495e);
  position: relative;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00a8ff, #0097e6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border: 3px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 12px;
}

.camera-off-icon {
  font-size: 20px;
  opacity: 0.6;
}

/* ========== 이름표 ========== */
.name-badge {
  position: absolute;
  bottom: 12px;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 10;
}

.name {
  font-size: 13px;
  font-weight: 500;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.8);
}

.status-icons {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.mic-off {
  opacity: 0.6;
  filter: grayscale(1);
}

.self-indicator {
  background: #00a8ff;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

/* ========== 반응형 ========== */
@media (max-width: 768px) {
  .video-grid {
    gap: 8px;
  }

  .avatar-circle {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }

  .name-badge {
    bottom: 8px;
    left: 8px;
    right: 8px;
    padding: 6px 10px;
  }

  .name {
    font-size: 13px;
    max-width: 100px;
  }
}

@media (max-width: 480px) {
  .video-grid {
    gap: 6px;
  }

  .avatar-circle {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }
}
</style>