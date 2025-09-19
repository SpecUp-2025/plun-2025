// src/components/meeting/composables/useMediaControls.js
import { ref } from 'vue'

export function useMediaControls() {
  // 사용자 설정 상태 (로컬스토리지에서 복원)
  const mirror = ref(localStorage.getItem('pref.mirror') === 'true')
  const speakerOn = ref(true)
  const micOn = ref(true)
  const camOn = ref(true)

  /**
   * 마이크 온/오프 토글
   * MediaStreamTrack의 enabled 속성으로 제어
   */
  function toggleMic(localStream) {
    micOn.value = !micOn.value

    if (localStream) {
      localStream.getAudioTracks().forEach(track => {
        track.enabled = micOn.value
      })
    }
  }

  /**
   * 카메라 온/오프 토글
   * SFU producer pause/resume + MediaStreamTrack enabled 조합
   */
  async function toggleCam(sfu, localStream) {
    camOn.value = !camOn.value

    if (sfu && localStream) {
      if (camOn.value) {
        // 카메라 켜기: 트랙 활성화 + producer resume/create
        localStream.getVideoTracks().forEach(track => {
          track.enabled = true
        })

        if (sfu._producers?.video) {
          try {
            await sfu.resume('video')
          } catch (e) {
            console.warn('SFU resume failed:', e)
          }
        } else {
          // Producer가 없으면 새로 생성
          const videoTrack = localStream.getVideoTracks()[0]
          if (videoTrack) {
            try {
              await sfu.produce('video', videoTrack)
            } catch (e) {
              console.warn('Failed to create video producer:', e)
            }
          }
        }
      } else {
        // 카메라 끄기: producer pause + 트랙 비활성화
        if (sfu._producers?.video) {
          try {
            await sfu.pause('video')
          } catch (e) {
            console.warn('SFU pause failed:', e)
          }
        }

        localStream.getVideoTracks().forEach(track => {
          track.enabled = false
        })
      }
    }
  }

  /**
   * 스피커 온/오프 토글
   * 모든 원격 참가자의 오디오 요소 muted 속성 제어
   */
  function toggleSpeaker(peers) {
    speakerOn.value = !speakerOn.value

    if (peers && peers.size > 0) {
      for (const [socketId, peer] of peers) {
        if (peer.audioEl) {
          peer.audioEl.muted = !speakerOn.value
          if (speakerOn.value) {
            peer.audioEl.play?.().catch(() => retryPlayOnGestureOnce(peer.audioEl))
          }
        }
      }
    }
  }

  /**
   * autoplay 실패 시 사용자 제스처 기다려서 재시도
   */
  function retryPlayOnGestureOnce(el) {
    const once = () => {
      el.play?.().catch(() => { })
      cleanup()
    }
    const cleanup = () => {
      document.removeEventListener('click', once)
      document.removeEventListener('keydown', once)
      document.removeEventListener('touchstart', once)
    }
    document.addEventListener('click', once, { once: true })
    document.addEventListener('keydown', once, { once: true })
    document.addEventListener('touchstart', once, { once: true })
  }

  /**
   * 로컬 미디어 스트림 획득
   * 고품질 오디오 설정 + 폴백 처리
   */
  async function getLocalStream() {
    // 프로덕션급 오디오 설정
    const constraints = {
      audio: {
        // 기본 브라우저 처리
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,

        // 고품질 설정
        sampleRate: { ideal: 48000, min: 44100 },
        channelCount: { ideal: 1 }, // 음성통화 최적화

        // Chrome/Edge 고급 설정
        googEchoCancellation: true,
        googEchoCancellation2: true,
        googAutoGainControl: true,
        googAutoGainControl2: true,
        googNoiseSuppression: true,
        googNoiseSuppression2: true,
        googHighpassFilter: true,        // 저주파 노이즈 제거
        googTypingNoiseDetection: true,  // 키보드 소음 감지
        googAudioMirroring: false,
        googAudioProcessing: true,

        // 레이턴시 최적화
        latency: { ideal: 0.02 }, // 20ms 목표
        bitrate: { ideal: 128000, max: 256000 }
      },
      video: {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        frameRate: { ideal: 30, max: 60 }
      }
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia(constraints)

      // 초기 상태 적용
      stream.getAudioTracks().forEach(t => (t.enabled = micOn.value))
      stream.getVideoTracks().forEach(t => (t.enabled = camOn.value))

      return stream

    } catch (error) {
      // 고급 설정 실패 시 기본 설정으로 폴백
      const fallbackConstraints = {
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          sampleRate: 48000,
          channelCount: 1
        },
        video: { width: { ideal: 1280 }, height: { ideal: 720 } }
      }

      try {
        const stream = await navigator.mediaDevices.getUserMedia(fallbackConstraints)

        stream.getAudioTracks().forEach(t => (t.enabled = micOn.value))
        stream.getVideoTracks().forEach(t => (t.enabled = camOn.value))

        return stream
      } catch (fallbackError) {
        console.error('Both enhanced and fallback getUserMedia failed:', fallbackError)
        throw fallbackError
      }
    }
  }

  /**
   * 로컬 스트림 정리
   */
  function stopLocalStream(localStream) {
    try {
      localStream?.getTracks()?.forEach(track => {
        track.stop()
      })
    } catch (e) {
      console.warn('Error stopping tracks:', e)
    }
  }

  return {
    // 상태
    mirror,
    speakerOn,
    micOn,
    camOn,

    // 메서드
    toggleMic,
    toggleCam,
    toggleSpeaker,
    getLocalStream,
    stopLocalStream
  }
}