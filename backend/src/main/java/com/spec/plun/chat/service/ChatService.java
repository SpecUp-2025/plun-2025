package com.spec.plun.chat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.spec.plun.chat.dao.ChatDAO;
import com.spec.plun.chat.entity.ChatMember;
import com.spec.plun.chat.entity.ChatMessage;
import com.spec.plun.chat.entity.ChatRoom;
import com.spec.plun.chat.entity.MessageType;

@Service
public class ChatService {
	@Autowired
	private ChatDAO chatDAO;
	
	// 채팅방 메시지 목록
	public List<ChatMessage> getChatMessages(int roomNo){
		return chatDAO.getChatMessageByRoomNo(roomNo);
	}
	// 채팅방 목록 조회
	public List<ChatRoom> getChatRooms(){
		return chatDAO.getChatRooms();
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
	    if (message.getMessageType() == null) {
	        message.setMessageType(MessageType.TALK);
	    }
	    message.setCreateDate(LocalDateTime.now());
		chatDAO.insertMessage(message);
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
	//  채팅방 퇴장
	public void removeMemberFromRoom(int roomNo, int userNo) {
		chatDAO.deleteChatMember(roomNo, userNo);
	}

}