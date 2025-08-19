package com.spec.plun.meeting.room;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MeetingRoom {
    private Integer roomNo;              // PK (AUTO_INCREMENT)
    private Integer teamNo;              // FK (nullable)
    private String title;               // NOT NULL
    private LocalDateTime scheduledTime;     // DEFAULT NOW
    private LocalDateTime scheduledEndTime;  // nullable
    private LocalDateTime startedTime;       // nullable
    private LocalDateTime endedTime;         // nullable
    private String roomCode;                // UNIQUE, Jitsi roomName으로 사용
    private String isPrivate;               // 'Y' or 'N' (DEFAULT 'N')
    private String roomPasswordHash;        // nullable
    private LocalDateTime createDate;        // DEFAULT NOW
    private LocalDateTime updateDate;        // on update
}
