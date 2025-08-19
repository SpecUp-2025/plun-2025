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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	@GetMapping("/room/{roomNo}/Messages")
	public List<ChatMessage> getChatMessages(@PathVariable("roomNo") int roomNo){
		return chatService.getChatMessages(roomNo);
	}
	
    // WebSocket 메시지 전송
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage message) {
    	chatService.saveMessage(message);
        messagingTemplate.convertAndSend("/topic/chat/room/" + message.getRoomNo(), message);
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
