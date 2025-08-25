package com.spec.plun.chat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.spec.plun.chat.dto.ChatMessageRequestDTO;
import com.spec.plun.chat.entity.ChatMember;
import com.spec.plun.chat.entity.ChatMessage;
import com.spec.plun.chat.entity.ChatRoom;
import com.spec.plun.chat.service.ChatService;

@RestController
@RequestMapping("/chat")
public class ChatController {
	@Autowired
	private ChatService chatService;
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	// 메시지와 파일(선택사항) 함께 전송
	@PostMapping("/send")
	public ResponseEntity<?> sendMessageWithOptionalAttachment(
	        @RequestPart("message") ChatMessageRequestDTO messageDTO,
	        @RequestPart(value = "file", required = false) MultipartFile file
	) {
	    try {
	    	// 1. 서비스 호출하여 메시지와 첨부파일 저장 처리
	        ChatMessage savedMessage = chatService.sendMessageWithOptionalAttachment(messageDTO, file);
	        
	        // 2. 파일 메시지도 WebSocket으로 브로드캐스트
	        messagingTemplate.convertAndSend(
	            "/topic/chat/room/" + savedMessage.getRoomNo(),
	            savedMessage
	        );
	        // 3. HTTP 200 OK와 함께 저장된 메시지 정보를 클라이언트에 반환
	        return ResponseEntity.ok(savedMessage);
	        
	    } catch (Exception e) {
	    	// 4. 에러 발생 시 스택 트레이스 출력 후 500 서버 에러 반환
	        e.printStackTrace();
	        return ResponseEntity.status(500).body("메시지 전송 실패");
	    }
	}
	// 채팅방 메시지 및 파일 목록을 조회
	@GetMapping("/message")
	public ResponseEntity<List<ChatMessage>> getMessageWithAttachments(@RequestParam int roomNo){
		// 1. 서비스 호출로 roomNo에 해당하는 메시지(첨부파일 포함) 목록을 조회
		List<ChatMessage> messages = chatService.getChatMessagesWithAttachments(roomNo);
		// 2. HTTP 200 OK와 함께 메시지 리스트를 반환
		return ResponseEntity.ok(messages);
	}
	// 채팅방 이름 조회
	@GetMapping("/room/{roomNo}")
	public ResponseEntity<ChatRoom> getChatRoom(@PathVariable("roomNo") int roomNo) {
	    ChatRoom chatRoom = chatService.getChatRoom(roomNo);
	    if (chatRoom != null) {
	        return ResponseEntity.ok(chatRoom);
	    } else {
	        return ResponseEntity.notFound().build();
	    }
	}

	// 채팅방 이름 변경
	@PutMapping("/room/{roomNo}/name")
	public ResponseEntity<?> updateRoomName(@PathVariable("roomNo") int roomNo, @RequestBody Map<String, String> request) {
	    String newName = request.get("roomName");
	    chatService.updateRoomName(roomNo, newName);
	    return ResponseEntity.ok().build();
	}
	// 채팅방 생성
	@PostMapping("/room")
	public ChatRoom createChatRoom(@RequestBody Map<String, String> roomName) {
		
		String name = roomName.get("roomName");
		return chatService.createChatRoom(name);
	}
	// 채팅방 목록
	@GetMapping("/rooms")
	public List<ChatRoom> getChatRooms() {
	    return chatService.getChatRooms();
	}
	// 특정 채팅방 메시지 목록
	@GetMapping("/room/{roomNo}/messages")
	public List<ChatMessage> getChatMessages(@PathVariable("roomNo") int roomNo){
		return chatService.getChatMessages(roomNo);
	}
	
	// WebSocket 메시지 및 파일 삭제 
	@MessageMapping("/chat.deleteAttachment")
	public void deleteAttachment(@Payload Map<String, Object> payload) {
	    int roomNo = (int) payload.get("roomNo");
	    System.out.println("📩 WebSocket 첨부파일 삭제 요청 수신: " + payload);
	    // 실제 삭제는 REST API로 이미 처리됐다고 가정
	    messagingTemplate.convertAndSend("/topic/chat/room/" + roomNo, payload);
	}
	
    // WebSocket 메시지 전송
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message) {
    	System.out.println("[WebSocket] sendMessage 호출됨: " + message);
    	chatService.saveMessage(message);
        messagingTemplate.convertAndSend("/topic/chat/room/" + message.getRoomNo(), message);
        System.out.println("[WebSocket] 메시지 브로드캐스트 완료: roomNo=" + message.getRoomNo());
    }
    // 특정 채팅방 참여자 목록 조회
    @GetMapping("/rooms/{roomNo}/members")
    public ResponseEntity<List<ChatMember>> getChatMember(@PathVariable("roomNo") int roomNo){
    	List<ChatMember> members = chatService.getChatMembers(roomNo);
    	return ResponseEntity.ok(members);
    }
    // 채팅방 입장 시 참여자 등록
    @PostMapping("/room/{roomNo}/member/{userNo}")
    public ResponseEntity<Void> addMemberToRoom(@PathVariable("roomNo") int roomNo, @PathVariable("userNo") int userNo ){
        chatService.addMemberToRoom(roomNo, userNo);
        return ResponseEntity.ok().build();
    }
    // 채팅방 퇴장
    @DeleteMapping("/room/{roomNo}/member/{userNo}")
    public ResponseEntity<Void> leaveChatRoom(@PathVariable("roomNo") int roomNo, @PathVariable("userNo") int userNo) {
        chatService.removeMemberFromRoom(roomNo, userNo);
        return ResponseEntity.ok().build();
    }
}
