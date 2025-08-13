package com.spec.plun.chat.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.spec.plun.chat.entity.ChatMessage;

@Mapper
public interface ChatDAO {
	
	List<ChatMessage> getChatMessageByRoomNo(int roomNo);

}
