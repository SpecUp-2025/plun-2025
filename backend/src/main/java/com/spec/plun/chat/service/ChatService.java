package com.spec.plun.chat.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.spec.plun.chat.dao.ChatDAO;
import com.spec.plun.chat.entity.ChatMessage;

@Service
public class ChatService {
	@Autowired
	private ChatDAO chatDAO;
	
	public List<ChatMessage> getChatMessages(int roomNo){
		return chatDAO.getChatMessageByRoomNo(roomNo);
	}

}