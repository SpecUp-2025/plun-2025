package com.spec.plun.meeting.room;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MeetingRoom {
    private Integer roomNo;                // PK (AUTO_INCREMENT)
    private Integer teamNo;                // FK (nullable)
    private String  title;                 // NOT NULL
    private LocalDateTime scheduledTime;   // DEFAULT NOW
    private LocalDateTime scheduledEndTime;// nullable
    private LocalDateTime startedTime;     // nullable
    private LocalDateTime endedTime;       // nullable
    private String  roomCode;              // UNIQUE, Jitsi roomName 등으로 사용
    private LocalDateTime createDate;      // DEFAULT NOW
    private LocalDateTime updateDate;      // ON UPDATE
    private Integer calDetailNo;
}
