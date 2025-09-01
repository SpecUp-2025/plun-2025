package com.spec.plun.calendar.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.calendar.entity.CalendarDetail;

@Mapper
public interface CalendarDAO {
	
	// 일정 조회
	List<CalendarDetail> getEventsBetween(@Param("start") String start, @Param("end") String end);
	// 일정 등록
	int insertEvent(CalendarDetail detail);
	// 일정 수정
	void updateEvent(CalendarDetail calendarDetail);

}
