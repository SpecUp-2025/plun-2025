package com.spec.plun.chat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.chat.entity.ChatMessage;
import com.spec.plun.chat.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
	@Autowired
	private ChatService chatService;
	
	@GetMapping("/room/{roomNo}/Messages")
	public List<ChatMessage> getChatMessages(@PathVariable int roomNo){
		return chatService.getChatMessages(roomNo);
	}

}
