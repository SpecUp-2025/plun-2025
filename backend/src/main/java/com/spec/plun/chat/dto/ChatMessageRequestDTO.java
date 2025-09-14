package com.spec.plun.chat.dto;

import java.util.List;

import com.spec.plun.chat.entity.MessageType;

import lombok.Data;
@Data
public class ChatMessageRequestDTO {
	
    private int roomNo;
    private int userNo;
    private String content;
    private MessageType messageType; // TALK, FILE 등
    private List<Integer> mentions;

}
