package com.spec.plun.chat.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.spec.plun.alarm.service.AlarmService;
import com.spec.plun.attachment.dao.AttachmentDAO;
import com.spec.plun.attachment.entity.Attachment;
import com.spec.plun.attachment.service.AttachmentService;
import com.spec.plun.chat.dao.ChatDAO;
import com.spec.plun.chat.dto.ChatMessageRequestDTO;
import com.spec.plun.chat.entity.ChatMember;
import com.spec.plun.chat.entity.ChatMessage;
import com.spec.plun.chat.entity.ChatRoom;
import com.spec.plun.chat.entity.MessageType;

@Service
public class ChatService {
	@Autowired
	private ChatDAO chatDAO;
	
	@Autowired
	private AttachmentDAO attachmentDAO;
	
	@Autowired
	private AttachmentService attachmentService;
	
	@Autowired
	private AlarmService alarmService;
	
	public ChatMessage sendMessageWithOptionalAttachment(ChatMessageRequestDTO dto, MultipartFile file) throws IOException {
		System.out.println("[ChatService] sendMessageWithOptionalAttachment 호출됨: dto=" + dto + ", file=" + (file != null ? file.getOriginalFilename() : "null"));
		ChatMessage message = new ChatMessage();
	    message.setRoomNo(dto.getRoomNo());
	    message.setUserNo(dto.getUserNo());
	    message.setContent(dto.getContent());
	    message.setMessageType(dto.getMessageType());
	    message.setCreateDate(LocalDateTime.now());

	    // 1. 메시지 저장
	    chatDAO.insertMessage(message); // messageNo가 생성됨
	    System.out.println("[ChatService] 메시지 저장 완료: messageNo=" + message.getMessageNo());
	    
	    // 작성자 이름 세팅 추가
	    String name = chatDAO.getUserNameByUserNo(message.getUserNo());
	    message.setName(name);


	    // 2. 파일이 있을 경우 첨부파일 저장
	    if (file != null && !file.isEmpty()) {
	    	System.out.println("[ChatService] 첨부파일 저장 시작: " + file.getOriginalFilename());
	        attachmentService.saveFile(file, message.getMessageNo());
	        System.out.println("[ChatService] 첨부파일 저장 완료");
	    }
	    
	    // 3. 메시지 객체에 첨부파일 리스트 세팅
	    List<Attachment> attachments = attachmentDAO.getAttachmentsByMessageNo(message.getMessageNo());
	    message.setAttachments(attachments);

	    return message;
	}
	
	public List<ChatMessage> getChatMessagesWithAttachments(int roomNo) {
	    List<ChatMessage> messages = chatDAO.getChatMessageByRoomNo(roomNo);

	    for (ChatMessage message : messages) {
	        // 각 메시지에 대한 첨부파일 조회
	        List<Attachment> attachments = attachmentDAO.getAttachmentsByMessageNo(message.getMessageNo());
	        message.setAttachments(attachments); // 메시지 객체에 추가
	    }

	    return messages;
	}
	
	// 채팅방 메시지 목록
	public List<ChatMessage> getChatMessages(int roomNo){
		List<ChatMessage> messages = chatDAO.getChatMessageByRoomNo(roomNo);
		
		for (ChatMessage message : messages) {
			List<Attachment> attachment = attachmentDAO.getAttachmentsByMessageNo(message.getMessageNo());
			message.setAttachments(attachment);
		}
		return messages;
	}
	// 채팅방 목록 조회
	public List<ChatRoom> getChatRooms(){
		return chatDAO.getChatRooms();
	}
	// 채팅방 이름 변경
	public void updateRoomName(int roomNo, String newName) {
		if (newName == null || newName.trim().isEmpty()) {
			throw new IllegalArgumentException("❌ 채팅방 이름은 비워둘 수 없습니다.");
		}
		
		int updatedCount = chatDAO.updateRoomName(roomNo, newName);
		if (updatedCount == 0) {
			throw new RuntimeException("❌ 채팅방 이름 변경 실패 (roomNo=" + roomNo + ")");
		}	
	}
	// 채팅방 생성
	public ChatRoom createChatRoom(String roomName) {
	    ChatRoom room = new ChatRoom();
	    room.setRoomName(roomName);
	    // createDate도 세팅할 수 있으면 세팅
	    chatDAO.createChatRoom(room);
	    return room;
	}
	// 메시지 전송 및 저장 -> 메시지 타입 설정 부분
	public void saveMessage(ChatMessage message) {
		System.out.println("[ChatService] saveMessage 호출됨: " + message);
	    if (message.getMessageType() == null) {
	        message.setMessageType(MessageType.TALK);
	        System.out.println("[ChatService] messageType이 null이어서 TALK으로 설정함");
	    }
	    message.setCreateDate(LocalDateTime.now());
		chatDAO.insertMessage(message);
		
	    // userNo -> name 변환 후 세팅
	    String name = chatDAO.getUserNameByUserNo(message.getUserNo());
	    message.setName(name);
		System.out.println("[ChatService] 메시지 저장 완료: messageNo=" + message.getMessageNo());
		
        // 상대방 userNo 조회 (간단한 예)
        int toUserNo = findOtherUserInRoom(message.getRoomNo(), message.getUserNo());
        if (toUserNo != -1) {
            alarmService.createChatAlarm(message.getUserNo(),toUserNo, message.getRoomNo(), message.getContent());
        }
    }

    // 상대방 userNo 찾는 헬퍼 메서드
    public int findOtherUserInRoom(int roomNo, int senderUserNo) {
        List<ChatMember> members = getChatMembers(roomNo);
        for (ChatMember member : members) {
            if (member.getUserNo() != senderUserNo) {
                return member.getUserNo();
            }
        }
        return -1;
    }
	// 채팅방 목록 조회
	public List<ChatMember> getChatMembers(int roomNo){
		return chatDAO.getChatMembers(roomNo);
	}
	// 채팅방 입장 시 등록
	public void addMemberToRoom(int roomNo, int userNo) {
		// 참여자가 이미 존재하는지 체크
		if(chatDAO.existMember(roomNo,userNo)) {
			return;
		}
		chatDAO.insertMember(roomNo, userNo);
	}
	// 채팅방 퇴장
	public void removeMemberFromRoom(int roomNo, int userNo) {
		chatDAO.deleteChatMember(roomNo, userNo);
	}
	// 채팅방 이름 조회
	public ChatRoom getChatRoom(int roomNo) {
		return chatDAO.getChatRoom(roomNo);
	}

}