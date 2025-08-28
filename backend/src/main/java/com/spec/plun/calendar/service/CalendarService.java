package com.spec.plun.calendar.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spec.plun.calendar.dao.CalendarDAO;
import com.spec.plun.calendar.entity.CalendarDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarService {
	
	private final CalendarDAO calendarDAO;
	
	// 일정 조회
	public List<CalendarDetail> getEventsBetween(String start, String end){
		return calendarDAO.getEventsBetween(start,end);
	}
	// 일정 등록
    public int insertEvent(CalendarDetail detail) {
        return calendarDAO.insertEvent(detail);
    }
	// 일정 수정
    public void updateEvent(CalendarDetail calendarDetail) {
        calendarDAO.updateEvent(calendarDetail);
    }

}
