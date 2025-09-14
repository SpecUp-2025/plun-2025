package com.spec.plun.calendar.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spec.plun.calendar.dao.CalendarDAO;
import com.spec.plun.calendar.dto.EventRequestDTO;
import com.spec.plun.calendar.entity.Calendar;
import com.spec.plun.calendar.entity.CalendarDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarService {
	
	private final CalendarDAO calendarDAO;
	private final SimpMessagingTemplate messagingTemplate;

	
	public Integer getCalNoByTeamAndUser(Integer teamNo, Integer userNo) {
	    return calendarDAO.getCalNoByTeamAndUser(teamNo, userNo);
	}
	
	public int createCalendar(Calendar calendar) {
	    return calendarDAO.insertCalendar(calendar);
	}
	// 일정 조회
	public List<CalendarDetail> getEventsBetween(Integer userNo, String start, String end) {
	    List<CalendarDetail> events = calendarDAO.getEventsBetween(userNo, start, end);
	    for (CalendarDetail event : events) {
	    	Integer calDetailNo = event.getCalDetailNo();
	        List<Integer> participants = calendarDAO.getParticipantsByCalDetailNo(event.getCalDetailNo());
	        System.out.println("📌 calDetailNo: " + calDetailNo + " → participants: " + participants);
	        event.setParticipantUserNos(participants);
	        System.out.println("Event calDetailNo: " + event.getCalDetailNo());
	    }
	    return events;
	}
	// 일정 등록
	public int insertSharedEvent(EventRequestDTO dto) {
	    CalendarDetail detail = dto.getDetail();
	    
	    // 1️. 내 일정 먼저 등록 (자동 증가 키를 detail.calDetailNo에 주입되도록 mapper 설정 필요)
	    int inserted = calendarDAO.insertEvent(detail);
	    Integer calDetailNo = detail.getCalDetailNo();

	    // 2️. 내가 만든 일정에 대한 참가자 정보 추가
	    if (dto.getParticipantUserNos() != null && calDetailNo != null) {
	        for (Integer userNoStr : dto.getParticipantUserNos()) {
	            Integer userNo = Integer.valueOf(userNoStr);

	            calendarDAO.insertParticipant(calDetailNo, userNo);
	            
	            // 실시간 메시지 전송
	            messagingTemplate.convertAndSend(
	                "/topic/calendar/refresh/" + userNo,
	                "newEventCreated"
	            );
	        }
	    }
	    return inserted;
	}
	// 일정 수정
    public void updateEvent(CalendarDetail calendarDetail) {
        calendarDAO.updateEvent(calendarDetail);
        
        // 🔔 일정 수정 → 참가자에게 WebSocket 메시지 발송
        List<Integer> participants = calendarDAO.getParticipantsByCalDetailNo(calendarDetail.getCalDetailNo());
        for (Integer userNo : participants) {
            messagingTemplate.convertAndSend(
                "/topic/calendar/refresh/" + userNo,
                "eventUpdated"
            );
        }
    }
    // 일정 삭제
    @Transactional
    public int deleteEvent(Integer calDetailNo) {
    	
        // 🔍 참가자 정보는 삭제 전에 가져와야 함!
        List<Integer> participants = calendarDAO.getParticipantsByCalDetailNo(calDetailNo);
        
        int result1 = calendarDAO.deleteEvent(calDetailNo); // 일정 삭제
        int result2 = calendarDAO.deleteParticipantsByCalDetailNo(calDetailNo); // 참가자 삭제
        
        if (result1 > 0 && result2 >= 0) {

            for (Integer userNo : participants) {
                messagingTemplate.convertAndSend(
                    "/topic/calendar/refresh/" + userNo,
                    "eventDeleted:" + calDetailNo
                );
            }
            return 1;
        }
        return 0;
    }
}
