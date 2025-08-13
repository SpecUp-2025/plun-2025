package com.spec.plun.chat.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessage {
	
	private int messageNo;
	private int roomNo;
	private int userNo;
	private String content;
	private LocalDateTime careateDate;
}
