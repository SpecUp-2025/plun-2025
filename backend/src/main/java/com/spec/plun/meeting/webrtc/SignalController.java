package com.spec.plun.meeting.webrtc;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/*
 * @MessageMapping: STOMP 메시지를 받는 핸들러 메서드에 붙이는 어노테이션.
   @Payload: 메시지 본문(JSON 등)을 자바 객체로 바인딩할 때 사용.
   SimpMessagingTemplate: 서버에서 특정 주제(topic)로 메시지를 보낼 때 쓰는 스프링 제공 도구.

 * WebRTC 시그널링 전용 STOMP 컨트롤러
 *
 * - 클라이언트 → /app/webrtc.signal 로 전송
 * - 서버     → /topic/webrtc/{roomId} 로 브로드캐스트
 *
 * 기존 WebSocketConfig 설정(/ws-chat, /app, /topic)을 변경하지 않고
 * webrtc 전용 네임스페이스만 분리해 충돌을 피합니다.
 */

@Controller
@RequiredArgsConstructor
public class SignalController {
	
	private final SimpMessagingTemplate template;
	
	@MessageMapping("/webrtc.signal")
	public void relay(@Payload Signal msg) {
		if (msg == null || msg.getRoomId() == null || msg.getRoomId().isBlank()) {
			return; // 필수값 없으면 무시
		}
		String destination = "/topic/webrtc/" + msg.getRoomId();
		template.convertAndSend(destination, msg);
	}
	
	// DTO 클래스
	@Data
	@NoArgsConstructor
	public static class Signal {
		private String type;    // "offer" | "answer" | "candidate" | "join" | "leave"
        private String roomId;  // 방 식별자
        private String from;    // 보낸 사람
        private String to;      // 특정 대상만 보낼 때
        private Object payload; // SDP/ICE 등 자유형식
	}
}
