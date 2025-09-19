// useRecording.js - 회의 녹음 및 실시간 STT 처리
import { ref, computed } from 'vue'
import axios from 'axios'

export function useRecording(roomCode, roomInfo, peers, localStream) {
  const recordingState = ref('idle') // 'idle' | 'recording' | 'paused' | 'processing'
  const recordingDuration = ref(0)

  let recordingTimer = null
  let mediaRecorder = null
  let recordingStream = null
  let chunkCounter = 0

  // 상태 computed 속성들
  const isRecording = computed(() => recordingState.value === 'recording')
  const isPaused = computed(() => recordingState.value === 'paused')
  const isProcessing = computed(() => recordingState.value === 'processing')
  const isIdle = computed(() => recordingState.value === 'idle')

  /**
   * 녹음 시작 - 서버에 세션 생성 후 로컬 녹음 시작
   */
  async function startRecording() {
    try {
      recordingStream = await getRecordingStream()

      const response = await axios.post('/stt/start-recording', {
        roomCode: roomCode.value,
        roomNo: roomInfo.value?.roomNo
      })

      if (response.data.success) {
        recordingState.value = 'recording'
        chunkCounter = 0
        startRecordingTimer()
        startMediaRecorder()
      }
    } catch (error) {
      console.error('Failed to start recording:', error)
      alert('녹음 시작에 실패했습니다.')
    }
  }

  /**
   * 녹음 일시정지 - 서버 알림 후 로컬 녹음 중단
   */
  async function pauseRecording() {
    try {
      recordingState.value = 'paused'
      stopMediaRecorder()
      stopRecordingTimer()

      const response = await axios.post('/stt/pause-recording', {
        roomCode: roomCode.value
      })

      if (!response.data.success) {
        // 서버 요청 실패 시 녹음 상태 복원
        recordingState.value = 'recording'
        startRecordingTimer()
        startMediaRecorder()
      }
    } catch (error) {
      console.error('Failed to pause recording:', error)
      recordingState.value = 'recording'
      startRecordingTimer()
      startMediaRecorder()
    }
  }

  /**
   * 녹음 재개 - 새 스트림 생성 후 녹음 재시작
   */
  async function resumeRecording() {
    try {
      const response = await axios.post('/stt/resume-recording', {
        roomCode: roomCode.value
      })

      if (response.data.success) {
        recordingState.value = 'recording'
        recordingStream = await getRecordingStream()
        startRecordingTimer()
        startMediaRecorder()
      }
    } catch (error) {
      console.error('Failed to resume recording:', error)
    }
  }

  /**
   * 녹음 종료 - 로컬 정리 후 서버에 최종 처리 요청
   */
  async function stopRecording() {
    try {
      recordingState.value = 'processing'
      stopMediaRecorder()
      stopRecordingTimer()

      // 마지막 청크 처리 대기 후 서버 요청
      setTimeout(async () => {
        try {
          const requestData = {
            roomCode: roomCode.value,
            roomNo: roomInfo.value?.roomNo
          }

          const response = await axios.post('/stt/stop-recording', requestData)

          if (response.data.success) {
            recordingState.value = 'idle'
            recordingDuration.value = 0
            alert('녹음이 완료되었습니다. 회의록이 생성 중입니다.')
          }
        } catch (error) {
          console.error('Server stop request failed:', error)
          recordingState.value = 'idle'
        }
      }, 1000)

    } catch (error) {
      console.error('Failed to stop recording:', error)
      recordingState.value = 'idle'
    }
  }

  /**
   * MediaRecorder 시작 - 8초 단위 청크로 연속 녹음
   */
  function startMediaRecorder() {
    if (!recordingStream) return
    createNewRecorder()
  }

  /**
   * 8초 단위 MediaRecorder 생성 및 순환 처리
   * 실시간 STT를 위해 짧은 청크로 분할
   */
  function createNewRecorder() {
    if (recordingState.value !== 'recording') return

    try {
      // 기존 레코더 정리
      if (mediaRecorder) {
        mediaRecorder.stop()
        mediaRecorder = null
      }

      mediaRecorder = new MediaRecorder(recordingStream, {
        mimeType: 'audio/webm;codecs=opus',
        audioBitsPerSecond: 192000 // 고품질 오디오
      })

      let chunkReceived = false

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0 && !chunkReceived) {
          chunkReceived = true
          handleChunk(event)
        }
      }

      mediaRecorder.onstop = () => {
        mediaRecorder = null
        // 녹음 중이면 다음 8초 청크 시작
        if (recordingState.value === 'recording') {
          setTimeout(() => createNewRecorder(), 100)
        }
      }

      mediaRecorder.onerror = (e) => {
        console.error('MediaRecorder error:', e)
        mediaRecorder = null
        if (recordingState.value === 'recording') {
          setTimeout(() => createNewRecorder(), 500)
        }
      }

      // 8초 녹음 시작
      mediaRecorder.start()

      // 8초 후 자동 정지 (다음 청크를 위해)
      setTimeout(() => {
        if (mediaRecorder && mediaRecorder.state === 'recording') {
          mediaRecorder.stop()
        }
      }, 8000)

    } catch (error) {
      console.error('MediaRecorder 생성 실패:', error)
      if (recordingState.value === 'recording') {
        setTimeout(() => createNewRecorder(), 1000)
      }
    }
  }

  /**
   * MediaRecorder 정리
   */
  function stopMediaRecorder() {
    if (mediaRecorder) {
      try {
        if (mediaRecorder.state === 'recording') {
          mediaRecorder.stop()
        }
      } catch (error) {
        console.error('Error stopping MediaRecorder:', error)
      }

      setTimeout(() => {
        mediaRecorder = null
        if (recordingStream) {
          recordingStream.getTracks().forEach(track => track.stop())
          recordingStream = null
        }
      }, 500)
    }
  }

  /**
   * 오디오 청크를 서버에 업로드하여 실시간 STT 처리
   */
  async function handleChunk(event) {
    if (event.data.size < 1024) return // 너무 작은 청크 무시
    if (recordingState.value === 'idle') return // 세션 종료됨

    const currentChunk = chunkCounter++

    try {
      const formData = new FormData()
      formData.append('audio', event.data, `chunk_${currentChunk}.webm`)
      formData.append('roomCode', roomCode.value)

      const response = await axios.post('/stt/upload-chunk', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 15000
      })

      console.log(`Chunk ${currentChunk} uploaded:`, response.data)

    } catch (error) {
      console.error(`Failed to upload chunk ${currentChunk}:`, error)

      if (error.response?.status === 404) {
        // 서버에서 세션 종료됨
        recordingState.value = 'idle'
        stopMediaRecorder()
      }
    }
  }

  /**
   * 회의 오디오 믹싱 - 로컬 + 모든 원격 참가자 오디오 합성
   * Web Audio API로 실시간 믹싱
   */
  async function getRecordingStream() {
    const audioContext = new AudioContext()
    const mixedOutput = audioContext.createMediaStreamDestination()

    // 로컬 오디오 추가 (이미 화상회의에서 처리된 깨끗한 스트림)
    if (localStream?.value) {
      const localAudioTracks = localStream.value.getAudioTracks()
      if (localAudioTracks.length > 0) {
        const localSource = audioContext.createMediaStreamSource(
          new MediaStream([localAudioTracks[0]])
        )
        localSource.connect(mixedOutput)
      }
    }

    // 원격 참가자 오디오 추가
    for (const [socketId, peer] of peers) {
      if (peer.audioTrack) {
        const remoteSource = audioContext.createMediaStreamSource(
          new MediaStream([peer.audioTrack])
        )
        remoteSource.connect(mixedOutput)
      }
    }

    return mixedOutput.stream
  }

  /**
   * 녹음 시간 타이머 시작
   */
  function startRecordingTimer() {
    recordingTimer = setInterval(() => {
      recordingDuration.value++
    }, 1000)
  }

  /**
   * 녹음 시간 타이머 정지
   */
  function stopRecordingTimer() {
    if (recordingTimer) {
      clearInterval(recordingTimer)
      recordingTimer = null
    }
  }

  /**
   * 시간 포맷팅 (초 → MM:SS)
   */
  function formatDuration(seconds) {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  /**
   * 리소스 정리
   */
  function cleanup() {
    stopMediaRecorder()
    stopRecordingTimer()
    recordingState.value = 'idle'
    recordingDuration.value = 0
    chunkCounter = 0
  }

  return {
    recordingState,
    recordingDuration,
    isRecording,
    isPaused,
    isProcessing,
    isIdle,
    startRecording,
    pauseRecording,
    resumeRecording,
    stopRecording,
    formatDuration,
    cleanup
  }
}