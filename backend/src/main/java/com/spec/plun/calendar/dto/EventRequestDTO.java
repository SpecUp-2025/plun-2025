package com.spec.plun.calendar.dto;

import java.util.List;

import com.spec.plun.calendar.entity.CalendarDetail;

import lombok.Data;

@Data
public class EventRequestDTO {
	private Integer teamNo;
    private CalendarDetail detail;       // 일정 상세 정보
    private List<Integer> participantUserNos;  // 공유할 팀원 user_no 리스트
}
