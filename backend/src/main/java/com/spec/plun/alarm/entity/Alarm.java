package com.spec.plun.alarm.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자 자동 생성
public class Alarm {
    private Integer alarmNo;
    private Integer userNo;
    private String name;
    private Integer senderNo;
    private String senderName;
    private String alarmType;     // 예: "CHAT", "CALENDAR"
    private Integer referenceNo; 
    private String content;
    private String isRead;        // "Y" or "N"
    private LocalDateTime createDate;
}
