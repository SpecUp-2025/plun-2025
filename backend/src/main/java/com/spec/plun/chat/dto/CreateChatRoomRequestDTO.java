package com.spec.plun.chat.dto;

import java.util.List;

import lombok.Data;
@Data
public class CreateChatRoomRequestDTO {
	
    private String roomName;
    private List<Integer> memberUserNos;
    private Integer teamNo;
    private Integer userNo;

}
