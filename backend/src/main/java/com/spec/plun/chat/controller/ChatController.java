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

import com.spec.plun.alarm.service.AlarmService;
import com.spec.plun.chat.dto.ChatMessageRequestDTO;
import com.spec.plun.chat.dto.CreateChatRoomRequestDTO;
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
	@Autowired AlarmService alarmService;
	
	// 메시지와 파일(선택사항) 함께 전송
	@PostMapping("/send")
	public ResponseEntity<?> sendMessageWithOptionalAttachment(
	        @RequestPart("message") ChatMessageRequestDTO messageDTO,
	        @RequestPart(value = "file", required = false) List<MultipartFile> file
	) {
	    try {
	        ChatMessage savedMessage = chatService.sendMessageWithOptionalAttachment(messageDTO, file);

	        // WebSocket으로 메시지 브로드캐스트
	        messagingTemplate.convertAndSend(
	            "/topic/chat/room/" + savedMessage.getRoomNo(),
	            savedMessage
	        );

	        // 알림 생성 (서비스에 넣어도 되지만 컨트롤러에서 호출해도 무방)
	        int toUserNo = chatService.findOtherUserInRoom(savedMessage.getRoomNo(), savedMessage.getUserNo());
	        if (toUserNo != -1) {
	            alarmService.createChatAlarm(savedMessage.getUserNo(), toUserNo, savedMessage.getRoomNo(), savedMessage.getContent());
	        }

	        return ResponseEntity.ok(savedMessage);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(500).body("메시지 전송 실패");
	    }
	}
	// 채팅방 메시지 및 파일 목록을 조회
	@GetMapping("/message")
	public ResponseEntity<List<ChatMessage>> getMessageWithAttachments(@RequestParam Integer roomNo){
		// 1. 서비스 호출로 roomNo에 해당하는 메시지(첨부파일 포함) 목록을 조회
		List<ChatMessage> messages = chatService.getChatMessagesWithAttachments(roomNo);
		// 2. HTTP 200 OK와 함께 메시지 리스트를 반환
		return ResponseEntity.ok(messages);
	}
	// 채팅방 이름 조회
	@GetMapping("/room/{roomNo}")
	public ResponseEntity<ChatRoom> getChatRoom(@PathVariable("roomNo") Integer roomNo) {
	    ChatRoom chatRoom = chatService.getChatRoom(roomNo);
	    if (chatRoom != null) {
	        return ResponseEntity.ok(chatRoom);
	    } else {
	        return ResponseEntity.notFound().build();
	    }
	}

	// 채팅방 이름 변경
	@PutMapping("/room/{roomNo}/name")
	public ResponseEntity<?> updateRoomName(@PathVariable("roomNo") Integer roomNo, @RequestBody Map<String, String> request) {
	    String newName = request.get("roomName");
	    chatService.updateRoomName(roomNo, newName);
	    
	    Map<String, Object> roomNameUpdate = Map.of(
	        "type", "ROOM_NAME_UPDATE",
	        "roomNo", roomNo,
	        "roomName", newName
	    );
	    messagingTemplate.convertAndSend("/topic/chat/room/" + roomNo, roomNameUpdate);
	    
	    // 팀별 브로드캐스트 추가
	    Integer teamNo = chatService.getTeamNoByRoomNo(roomNo);
	    if (teamNo != null) {
	        messagingTemplate.convertAndSend("/topic/team/" + teamNo + "/roomNameUpdate", roomNameUpdate);
	    }
	    
	    return ResponseEntity.ok().build();
	}
	// 채팅방 생성
	@PostMapping("/room")
	public ResponseEntity<ChatRoom> createChatRoom(@RequestBody CreateChatRoomRequestDTO request) {
		
	    String roomName = request.getRoomName();
	    List<Integer> memberUserNos = request.getMemberUserNos();
	    int userNo = request.getUserNo();
	    int teamNo = request.getTeamNo();
	    
	    ChatRoom createdRoom = chatService.createChatRoomWithMembers(roomName, memberUserNos, userNo, teamNo);
	    return ResponseEntity.ok(createdRoom);
	}
	// 채팅방 목록
	//	@GetMapping("/rooms")
	//	public List<ChatRoom> getChatRooms() {
	//	    return chatService.getChatRooms();
	//	}
	// 채팅방 목록
	@GetMapping("/rooms")
	public List<ChatRoom> getChatRooms(@RequestParam("userNo") Integer userNo,
									   @RequestParam("teamNo") Integer teamNo) {
	    return chatService.getChatRooms(userNo, teamNo);
	}
	// 특정 채팅방 메시지 목록
	@GetMapping("/room/{roomNo}/messages")
	public List<ChatMessage> getChatMessages(@PathVariable("roomNo") Integer roomNo){
		return chatService.getChatMessages(roomNo);
	}
	
	// WebSocket 메시지 및 파일 삭제 
	@MessageMapping("/chat.deleteAttachment")
	public void deleteAttachment(@Payload Map<String, Object> payload) {
		Integer roomNo = (Integer) payload.get("roomNo");
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
    public ResponseEntity<List<ChatMember>> getChatMember(@PathVariable("roomNo") Integer roomNo){
    	List<ChatMember> members = chatService.getChatMembers(roomNo);
    	return ResponseEntity.ok(members);
    }
    // 채팅방 입장
    @PostMapping("/room/{roomNo}/member/{userNo}")
    public ResponseEntity<Void> addMemberToRoom(@PathVariable("roomNo") Integer roomNo, @PathVariable("userNo") Integer userNo ){
    	// 기존 참여자 등록
        chatService.addMemberToRoom(roomNo, userNo);

        // 참여자 목록 조회
        List<ChatMember> members = chatService.getChatMembers(roomNo);

        // WebSocket으로 참여자 목록 전송
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomNo + "/members", members);

        return ResponseEntity.ok().build();
    }
    // 채팅방 퇴장
    @DeleteMapping("/room/{roomNo}/member/{userNo}")
    public ResponseEntity<Void> leaveChatRoom(@PathVariable("roomNo") Integer roomNo, @PathVariable("userNo") Integer userNo) {
        // 기존 퇴장 처리
        chatService.removeMemberFromRoom(roomNo, userNo);

        // 참여자 목록 조회
        List<ChatMember> members = chatService.getChatMembers(roomNo);

        // WebSocket으로 참여자 목록 전송
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomNo + "/members", members);

        return ResponseEntity.ok().build();
    }
}
