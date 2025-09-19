// src/components/meeting/composables/useSfu.js
import { ref, reactive } from 'vue'
import { createSfuClient } from '@/util/sfuClient'

export function useSfu(roomCode, displayName, userNo, localStream, speakerOn) {
  const sfu = ref(null) // SFU 클라이언트 인스턴스
  const peers = reactive(new Map()) // 원격 참가자들의 미디어 상태
  const consumed = reactive(new Set()) // 이미 소비한 producer ID 목록

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
   * 참가자 정보 생성 또는 업데이트
   */
  function ensurePeer(socketId, displayName) {
    if (!peers.has(socketId)) {
      peers.set(socketId, {
        displayName: displayName || socketId,
        videoStream: null,
        audioEl: null,
        audioTrack: null
      })
    } else if (displayName) {
      peers.get(socketId).displayName = displayName
    }
    return peers.get(socketId)
  }

  /**
   * 원격 오디오 트랙을 Audio 요소에 연결
   * 실제 스피커로 출력되는 부분
   */
  async function attachRemoteAudio(socketId, producerId, track, displayName) {
    if (socketId === sfu.value?.socket?.id) return // 자기 자신 제외

    const peer = ensurePeer(socketId, displayName)
    peer.audioTrack = track

    let audioEl = peer.audioEl
    if (!audioEl) {
      // DOM에 숨겨진 Audio 요소 생성
      audioEl = new Audio()
      audioEl.autoplay = true
      audioEl.playsInline = true
      audioEl.style.display = 'none'
      document.body.appendChild(audioEl)
      peer.audioEl = audioEl
    }

    audioEl.srcObject = new MediaStream([track])
    audioEl.muted = !speakerOn.value
    if (speakerOn.value) {
      audioEl.play?.().catch(() => retryPlayOnGestureOnce(audioEl))
    }

    peers.set(socketId, peer)
    consumed.add(producerId)
  }

  /**
   * 원격 비디오 트랙을 MediaStream으로 저장
   * 실제 video 요소 연결은 VideoGrid에서 처리
   */
  async function attachRemoteVideo(socketId, producerId, track, displayName, consumer = null) {
    if (socketId === sfu.value?.socket?.id) return

    const peer = ensurePeer(socketId, displayName)
    peer.videoStream = new MediaStream([track])
    if (consumer) {
      peer.consumer = consumer // mediasoup consumer 참조 저장
    }
    peers.set(socketId, peer)
    consumed.add(producerId)
  }

  /**
   * 참가자 퇴장 시 리소스 정리
   */
  function removePeer(socketId) {
    const peer = peers.get(socketId)
    if (!peer) return

    // 오디오 요소 정리
    if (peer.audioEl) {
      try { peer.audioEl.pause() } catch { }
      peer.audioEl.srcObject = null
      peer.audioEl.remove?.()
    }
    peer.videoStream = null
    peers.delete(socketId)
  }

  /**
   * SFU 서버 연결 및 미디어 교환 시작
   */
  async function connectSfu() {
    sfu.value = createSfuClient({
      roomCode: roomCode.value,
      displayName: displayName.value,
      userNo: userNo.value
    })

    // 회의방 참가 및 기존 참가자 정보 수신
    const { existingProducers } = await sfu.value.join()

    // 로컬 미디어 스트림을 SFU로 전송
    const audioTrack = localStream.value?.getAudioTracks?.()?.[0]
    const videoTrack = localStream.value?.getVideoTracks?.()?.[0]
    if (audioTrack) await sfu.value.produce('audio', audioTrack)
    if (videoTrack) await sfu.value.produce('video', videoTrack)

    // 기존 참가자들의 미디어 스트림 소비
    for (const producer of (existingProducers || [])) {
      if (consumed.has(producer.producerId) || producer.socketId === sfu.value.socket.id) continue

      try {
        const { consumer, track, kind } = await sfu.value.consume(producer.producerId)
        try { await consumer.resume() } catch { }

        if (kind === 'audio') {
          await attachRemoteAudio(producer.socketId, producer.producerId, track, producer.displayName)
        } else {
          await attachRemoteVideo(producer.socketId, producer.producerId, track, producer.displayName, consumer)
        }
      } catch (e) {
        console.warn('기존 참가자 consume 실패:', e)
      }
    }

    setupEventHandlers()
  }

  /**
   * SFU 이벤트 핸들러 설정
   */
  function setupEventHandlers() {
    // 새 참가자의 미디어 스트림 수신
    sfu.value.onNewProducer = async ({ producerId, kind, socketId, displayName }) => {
      if (consumed.has(producerId) || socketId === sfu.value.socket.id) return

      try {
        const { consumer, track } = await sfu.value.consume(producerId)
        try { await consumer.resume() } catch { }

        if (kind === 'audio') {
          await attachRemoteAudio(socketId, producerId, track, displayName)
        } else {
          await attachRemoteVideo(socketId, producerId, track, displayName, consumer)
        }
      } catch (e) {
        console.warn('새 참가자 consume 실패:', e)
      }
    }

    // 참가자 퇴장 처리
    sfu.value.onPeerLeft = ({ socketId }) => removePeer(socketId)

    // Producer 종료 (참가자가 카메라/마이크 완전 해제)
    sfu.value.socket.on('sfu:producer-closed', ({ socketId, kind }) => {
      const peer = peers.get(socketId)
      if (!peer) return

      if (kind === 'audio') {
        if (peer.audioEl) {
          try { peer.audioEl.pause() } catch { }
          peer.audioEl.srcObject = null
          peer.audioEl.remove?.()
          peer.audioEl = null
        }
        peer.audioTrack = null
      } else if (kind === 'video') {
        peer.videoStream = null
      }
      peers.set(socketId, peer)
    })

    // Producer 일시정지 (참가자가 카메라 OFF)
    sfu.value.socket.on('sfu:producer-paused', ({ socketId, producerId, kind }) => {
      if (kind === 'video') {
        const peer = peers.get(socketId)
        if (peer) {
          if (peer.consumer) {
            peer.consumer.pause() // mediasoup consumer 일시정지
          }
          if (peer.videoStream) {
            const tracks = peer.videoStream.getVideoTracks()
            tracks.forEach(track => {
              track.enabled = false // 로컬 트랙도 비활성화
            })
          }
          peers.set(socketId, { ...peer })
        }
      }
    })

    // Producer 재개 (참가자가 카메라 ON)
    sfu.value.socket.on('sfu:producer-resumed', ({ socketId, producerId, kind }) => {
      if (kind === 'video') {
        const peer = peers.get(socketId)
        if (peer) {
          if (peer.consumer) {
            peer.consumer.resume() // mediasoup consumer 재개
          }
          if (peer.videoStream) {
            const tracks = peer.videoStream.getVideoTracks()
            tracks.forEach(track => {
              track.enabled = true // 로컬 트랙도 활성화
            })
          }
          peers.set(socketId, { ...peer })
        }
      }
    })
  }

  /**
   * 모든 참가자의 오디오 출력 상태 업데이트
   */
  function updateSpeakerState() {
    for (const [socketId, peer] of peers) {
      if (peer.audioEl) {
        peer.audioEl.muted = !speakerOn.value
        if (speakerOn.value) {
          peer.audioEl.play?.().catch(() => retryPlayOnGestureOnce(peer.audioEl))
        }
      }
    }
  }

  /**
   * SFU 연결 종료 및 리소스 정리
   */
  function disconnect() {
    try {
      sfu.value?.disconnect()
    } catch (e) {
      console.warn('SFU 연결 해제 오류:', e)
    }

    // 모든 참가자 리소스 정리
    for (const [socketId, peer] of peers) {
      try { peer.audioEl?.pause?.() } catch { }
      if (peer.audioEl) peer.audioEl.srcObject = null
      peer.audioEl?.remove?.()
      peer.videoStream = null
    }
    peers.clear()
    consumed.clear()
  }

  return {
    sfu,
    peers,
    consumed,
    connectSfu,
    updateSpeakerState,
    disconnect
  }
}