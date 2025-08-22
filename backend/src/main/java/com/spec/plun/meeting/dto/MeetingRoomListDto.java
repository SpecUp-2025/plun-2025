package com.spec.plun.meeting.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MeetingRoomListDto {
    private Integer roomNo;
    private String  roomCode;
    private String  title;
    private LocalDateTime scheduledTime;
    private LocalDateTime scheduledEndTime;
    private String  isPrivate; // 'Y'/'N'
}
