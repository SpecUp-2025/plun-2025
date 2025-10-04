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
	
	public ChatMessage sendMessageWithOptionalAttachment(ChatMessageRequestDTO dto, List<MultipartFile> files) throws IOException {
	
		ChatMessage message = new ChatMessage();
	    message.setRoomNo(dto.getRoomNo());
	    message.setUserNo(dto.getUserNo());
	    message.setContent(dto.getContent());
	    message.setMessageType(dto.getMessageType());
	    message.setCreateDate(LocalDateTime.now());
	    
	    // Mentions 저장 (transient 필드이므로 DB 저장은 안 됨)
	    message.setMentions(dto.getMentions());

	    // 메시지 저장
	    chatDAO.insertMessage(message); // messageNo가 생성됨
	    System.out.println("[ChatService] 메시지 저장 완료: messageNo=" + message.getMessageNo());
	    
	    if (files != null && !files.isEmpty()) {
	        for (MultipartFile file : files) {
	            attachmentService.saveFile(file, message.getMessageNo());
	        }
	    }
	    // 작성자 이름 세팅 추가
	    String name = chatDAO.getUserNameByUserNo(message.getUserNo());
	    message.setName(name);
	    
	    // 첨부파일 리스트 세팅
	    List<Attachment> attachments = attachmentDAO.getAttachmentsByMessageNo(message.getMessageNo());
	    message.setAttachments(attachments);
	    
	    // Mentions 알림 처리
	    if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
	        for (Integer mentionedUserNo : dto.getMentions()) {
	            if (mentionedUserNo != dto.getUserNo()) {
	                alarmService.createMentionAlarm(
	                    dto.getUserNo(),
	                    mentionedUserNo,
	                    dto.getRoomNo(),
	                    name + "님이 당신을 멘션했습니다: " + dto.getContent()
	                );
	            }
	        }
	    }

	    return message;
	}
	
	public List<ChatMessage> getChatMessagesWithAttachments(Integer roomNo) {
	    List<ChatMessage> messages = chatDAO.getChatMessageByRoomNo(roomNo);

	    for (ChatMessage message : messages) {
	        // 각 메시지에 대한 첨부파일 조회
	        List<Attachment> attachments = attachmentDAO.getAttachmentsByMessageNo(message.getMessageNo());
	        message.setAttachments(attachments); // 메시지 객체에 추가
	    }

	    return messages;
	}
	
	// 채팅방 메시지 목록
	public List<ChatMessage> getChatMessages(Integer roomNo){
		List<ChatMessage> messages = chatDAO.getChatMessageByRoomNo(roomNo);
		
		for (ChatMessage message : messages) {
			List<Attachment> attachment = attachmentDAO.getAttachmentsByMessageNo(message.getMessageNo());
			message.setAttachments(attachment);
		}
		return messages;
	}
	// 채팅방 목록 조회
	public List<ChatRoom> getChatRooms(Integer userNo, Integer teamNo) {
	    return chatDAO.getChatRoomsByUserNo(userNo, teamNo);
	}
	// 채팅방 이름 변경
	public void updateRoomName(Integer roomNo, String newName) {
		if (newName == null || newName.trim().isEmpty()) {
			throw new IllegalArgumentException("❌ 채팅방 이름은 비워둘 수 없습니다.");
		}
		
		int updatedCount = chatDAO.updateRoomName(roomNo, newName);
		if (updatedCount == 0) {
			throw new RuntimeException("❌ 채팅방 이름 변경 실패 (roomNo=" + roomNo + ")");
		}	
	}
	public Integer getTeamNoByRoomNo(Integer roomNo) {
	    return chatDAO.getTeamNoByRoomNo(roomNo);
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
		
		//  Mentions 알림 처리 추가
	    if (message.getMentions() != null && !message.getMentions().isEmpty()) {
	        for (Integer mentionedUserNo : message.getMentions()) {
	            if (!mentionedUserNo.equals(message.getUserNo())) {
	                alarmService.createMentionAlarm(
	                    message.getUserNo(),
	                    mentionedUserNo,
	                    message.getRoomNo(),
	                    name + "님이 당신을 멘션했습니다: " + message.getContent()
	                );
	            }
	        }
	    } else {
	        // 📌 mentions 없을 때 일반 알림 처리
	        int toUserNo = findOtherUserInRoom(message.getRoomNo(), message.getUserNo());
	        if (toUserNo != -1) {
	            alarmService.createChatAlarm(
	                message.getUserNo(),
	                toUserNo,
	                message.getRoomNo(),
	                message.getContent()
	            );
	        }
	    }
	}
    // 상대방 userNo 찾는 헬퍼 메서드
    public int findOtherUserInRoom(Integer roomNo, Integer senderUserNo) {
        List<ChatMember> members = getChatMembers(roomNo);
        for (ChatMember member : members) {
            if (member.getUserNo() != senderUserNo) {
                return member.getUserNo();
            }
        }
        return -1;
    }
	// 채팅방 목록 조회
	public List<ChatMember> getChatMembers(Integer roomNo){
		return chatDAO.getChatMembers(roomNo);
	}
	// 채팅방 입장 시 등록
	public void addMemberToRoom(Integer roomNo, Integer userNo) {
		// 참여자가 이미 존재하는지 체크
		if(chatDAO.existMember(roomNo,userNo)) {
			return;
		}
		chatDAO.insertMember(roomNo, userNo);
	}
	// 채팅방 퇴장
	public void removeMemberFromRoom(Integer roomNo, Integer userNo) {
		chatDAO.deleteChatMember(roomNo, userNo);
	}
	// 채팅방 이름 조회
	public ChatRoom getChatRoom(Integer roomNo) {
		return chatDAO.getChatRoom(roomNo);
	}
	// 팀원 초대 채팅방 초대
	public ChatRoom createChatRoomWithMembers(String roomName, List<Integer> memberUserNos, Integer creatorUserNo, Integer teamNo) {
	    // 1. 채팅방 생성
	    ChatRoom room = new ChatRoom();
	    room.setRoomName(roomName);
	    room.setCreateDate(LocalDateTime.now());
	    room.setTeamNo(teamNo);  // 여기에 팀번호 세팅 필수!
	    chatDAO.createChatRoom(room); // DB에서 roomNo 생성됨

	    // 2. 참여자 등록
	    if (memberUserNos != null && !memberUserNos.isEmpty()) {
	        for (Integer userNo : memberUserNos) {
	            chatDAO.insertMember(room.getRoomNo(), userNo);
	            
	         // 알림 생성 및 전송 (WebSocket 포함)
	            alarmService.createChatAlarm(
	            	creatorUserNo, // 시스템 또는 방 생성자로 추후 변경 가능
	                userNo,
	                room.getRoomNo(),
	                "'" + roomName + "' 방에 초대되었습니다."
	            );
	        }
	    }

	    // 3. 반환
	    return room;
	}
}