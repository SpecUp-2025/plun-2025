package com.spec.plun.meeting.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RoomDetailRes {
    private Integer roomNo;
    private String  roomCode;
    private String  title;
    private LocalDateTime scheduledTime;
    private LocalDateTime scheduledEndTime;
    private Integer calDetailNo;

    private Integer creatorUserNo;
    
    @JsonProperty("isCreator")
    private boolean creator; 

    private List<ParticipantDto> participants;
}
