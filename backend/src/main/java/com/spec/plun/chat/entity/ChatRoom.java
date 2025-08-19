package com.spec.plun.chat.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatRoom {
	private int roomNo;
	private String roomName;
	private LocalDateTime createDate;
	
    // 기본 생성자
    public ChatRoom() {}

    // 명시적 생성자 - 파라미터 변수를 사용
    public ChatRoom(int roomNo, String roomName) {
        this.roomNo = roomNo;
        this.roomName = roomName;
        this.createDate = LocalDateTime.now();
    }
}
