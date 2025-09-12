package com.spec.plun.calendar.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spec.plun.calendar.dao.CalendarDAO;
import com.spec.plun.calendar.dto.EventRequestDTO;
import com.spec.plun.calendar.entity.Calendar;
import com.spec.plun.calendar.entity.CalendarDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarService {
	
	private final CalendarDAO calendarDAO;
	
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
	        }
	    }

	    // 3️⃣ 공유 대상에게도 동일한 일정 복사 (그들의 캘린더에 추가)
	    if (dto.getParticipantUserNos() != null) {
	        for (Integer userNoStr : dto.getParticipantUserNos()) {
	            Integer userNo = Integer.valueOf(userNoStr);
	            Integer targetCalNo = calendarDAO.getCalNoByUserNo(userNo);

	            if (targetCalNo != null) {
	                CalendarDetail copied = new CalendarDetail();
	                
	                copied.setCalNo(targetCalNo);
	                copied.setTitle(detail.getTitle());
	                copied.setContents(detail.getContents());
	                copied.setStartDate(detail.getStartDate());
	                copied.setStartTime(detail.getStartTime());
	                copied.setEndDate(detail.getEndDate());
	                copied.setEndTime(detail.getEndTime());
	                copied.setRegUserNo(detail.getRegUserNo()); // 등록자는 나

	                calendarDAO.insertEvent(copied);
	            }
	        }
	    }

	    return inserted;
	}
	// 일정 수정
    public void updateEvent(CalendarDetail calendarDetail) {
        calendarDAO.updateEvent(calendarDetail);
    }
    // 일정 삭제
    public int deleteEvent(Integer calDetailNo) {
        return calendarDAO.deleteEvent(calDetailNo);
    }


}
