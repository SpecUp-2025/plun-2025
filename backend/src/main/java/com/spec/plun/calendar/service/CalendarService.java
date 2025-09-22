package com.spec.plun.calendar.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spec.plun.calendar.dao.CalendarDAO;
import com.spec.plun.calendar.dto.EventRequestDTO;
import com.spec.plun.calendar.entity.Calendar;
import com.spec.plun.calendar.entity.CalendarDetail;
import com.spec.plun.alarm.service.AlarmService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarService {
	
	private final CalendarDAO calendarDAO;
	private final SimpMessagingTemplate messagingTemplate;
	private final AlarmService alarmService;

	
	public Integer getCalNoByTeamAndUser(Integer teamNo, Integer userNo) {
	    return calendarDAO.getCalNoByTeamAndUser(teamNo, userNo);
	}
	
	public int createCalendar(Calendar calendar) {
	    return calendarDAO.insertCalendar(calendar);
	}

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

	public int insertSharedEvent(EventRequestDTO dto) {
	    CalendarDetail detail = dto.getDetail();
	    Integer creatorUserNo = detail.getRegUserNo(); // 일정 생성자
	    
	    int inserted = calendarDAO.insertEvent(detail);
	    Integer calDetailNo = detail.getCalDetailNo();

	    if (dto.getParticipantUserNos() != null && calDetailNo != null) {
	        for (Integer userNo : dto.getParticipantUserNos()) {
	            calendarDAO.insertParticipant(calDetailNo, userNo);
	            
	            // 캘린더 갱신 메시지 전송
	            messagingTemplate.convertAndSend(
	                "/topic/calendar/refresh/" + userNo,
	                "newEventCreated"
	            );
	            
	            // 초대 알림 생성 (본인 제외)
	            if (!userNo.equals(creatorUserNo)) {
	                System.out.println("📩 캘린더 초대 알림 생성: " + creatorUserNo + " → " + userNo);
	                alarmService.createCalendarInviteAlarm(
	                    creatorUserNo, 
	                    userNo, 
	                    calDetailNo, 
	                    detail.getTitle(),
	                    detail.getStartDate() + "T" + detail.getStartTime(),
	                    null // teamNo - 필요시 추가
	                );
	            }
	        }
	    }
	    return inserted;
	}

    public void updateEvent(EventRequestDTO dto) {
    	CalendarDetail detail = dto.getDetail();
    	Integer updaterUserNo = detail.getRegUserNo();
    	
    	// 기존 참가자 목록 조회
    	List<Integer> oldParticipants = calendarDAO.getParticipantsByCalDetailNo(detail.getCalDetailNo());
    	
        calendarDAO.updateEvent(detail);
        updateParticipants(detail.getCalDetailNo(), dto.getParticipantUserNos());
        
        // 참가자에게 갱신 메시지 발송
        List<Integer> participants = calendarDAO.getParticipantsByCalDetailNo(detail.getCalDetailNo());
        for (Integer userNo : participants) {
            messagingTemplate.convertAndSend(
                "/topic/calendar/refresh/" + userNo,
                "eventUpdated"
            );
        }
        
        // 새로 추가된 참가자에게 초대 알림 발송
        if (dto.getParticipantUserNos() != null) {
            for (Integer newUserNo : dto.getParticipantUserNos()) {
                // 기존에 없던 참가자이고, 수정자 본인이 아닌 경우
                if (!oldParticipants.contains(newUserNo) && !newUserNo.equals(updaterUserNo)) {
                    System.out.println("📩 일정 수정 시 새 초대 알림: " + updaterUserNo + " → " + newUserNo);
                    alarmService.createCalendarInviteAlarm(
                        updaterUserNo, 
                        newUserNo, 
                        detail.getCalDetailNo(), 
                        detail.getTitle(),
                        detail.getStartDate() + "T" + detail.getStartTime(),
                        null
                    );
                }
            }
        }
    }
    @Transactional
    public void updateParticipants(Integer calDetailNo, List<Integer> userNos) {
        calendarDAO.deleteParticipantsByCalDetailNo(calDetailNo);
        
        if (userNos != null && !userNos.isEmpty()) {
            calendarDAO.insertParticipants(calDetailNo, userNos);
        }
    }
    @Transactional
    public int deleteEvent(Integer calDetailNo) {
        List<Integer> participants = calendarDAO.getParticipantsByCalDetailNo(calDetailNo);
        
        int result1 = calendarDAO.deleteEvent(calDetailNo);
        int result2 = calendarDAO.deleteParticipantsByCalDetailNo(calDetailNo);
        
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