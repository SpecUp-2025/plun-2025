package com.spec.plun.chat.dto;

import com.spec.plun.chat.entity.MessageType;

import lombok.Data;
@Data
public class ChatMessageRequestDTO {
    private int roomNo;
    private int userNo;
    private String content;
    private MessageType messageType; // TALK, FILE 등
}
