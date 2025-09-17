package com.spec.plun.chat.dto;

import java.util.List;

import com.spec.plun.chat.entity.MessageType;

import lombok.Data;
@Data
public class ChatMessageRequestDTO {
	
    private Integer roomNo;
    private Integer userNo;
    private String content;
    private MessageType messageType; // TALK, FILE 등
    private List<Integer> mentions;

}
