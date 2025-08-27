package com.spec.plun.chat.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.chat.entity.ChatMember;
import com.spec.plun.chat.entity.ChatMessage;
import com.spec.plun.chat.entity.ChatRoom;

@Mapper
public interface ChatDAO {
	
	// 채팅방의 모든 메시지 조회
	List<ChatMessage> getChatMessageByRoomNo(int roomNo);
	// 채팅방 생성
	void createChatRoom(ChatRoom roomName);
	// 채팅방 목록
	List<ChatRoom> getChatRooms();
	// 메시지 저장
	void insertMessage(ChatMessage message);
	// 특정 채팅방의 참여자 목록 조회 (개선된 방식 - 파라미터 명시)
	List<ChatMember> getChatMembers(@Param("roomNo") int roomNo);
	// 채팅방에 사용자 추가 (참여자 등록)
    void insertMember(@Param("roomNo") int roomNo, @Param("userNo") int userNo);
	// 해당 사용자가 채팅방에 이미 참여 중인지 확인
    boolean existMember(@Param("roomNo") int roomNo, @Param("userNo") int userNo);
    // 채팅방에서 사용자 제거 (퇴장 - 나가기)
    void deleteChatMember(@Param("roomNo") int roomNo, @Param("userNo") int userNo);
    // 채팅방 이름 변경
    int updateRoomName(@Param("roomNo") int roomNo, @Param("newName") String newName);
    // 채팅방 이름 조회
    ChatRoom getChatRoom(int roomNo);
    
    String getUserNameByUserNo(int userNo);

}
