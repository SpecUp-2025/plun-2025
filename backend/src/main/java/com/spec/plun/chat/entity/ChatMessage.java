package com.spec.plun.chat.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessage {
	
	private int messageNo;
	private int roomNo;
	private int userNo;
	private String content; // 텍스트 또는 멘션 포함 문자열
	private LocalDateTime createDate;
	
    private MessageType messageType; // 메시지 종류 규뷴 (텍스트, 파일 등)
}
