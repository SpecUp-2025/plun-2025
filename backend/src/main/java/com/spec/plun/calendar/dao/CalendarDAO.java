package com.spec.plun.calendar.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.calendar.entity.Calendar;
import com.spec.plun.calendar.entity.CalendarDetail;

@Mapper
public interface CalendarDAO {
	
	int insertParticipant(@Param("calDetailNo") Integer calDetailNo, @Param("userNo") Integer userNo);
	// teamNo와 userNo로 cal_no 조회
	Integer getCalNoByTeamAndUser(@Param("teamNo") Integer teamNo,
	                               @Param("userNo") Integer userNo);
	int insertCalendar(Calendar calendar);
	// 일정 조회
	List<CalendarDetail> getEventsBetween(@Param("userNo") Integer userNo,
			                              @Param("teamNo") Integer teamNo,
			                              @Param("start") String start,
			                              @Param("end") String end);
	// 일정 등록
	int insertEvent(CalendarDetail detail);
	// 일정 수정
	void updateEvent(CalendarDetail calendarDetail);
	// 일정 삭제
	int deleteEvent(@Param("calDetailNo") Integer calDetailNo);
	// 참가자 삭제
	int deleteParticipantsByCalDetailNo(Integer calDetailNo);
    // user_no로 cal_no 조회
    Integer getCalNoByUserNo(@Param("userNo") Integer userNo);
    // 일정에 참가한 사용자 번호 리스트 조회
    List<Integer> getParticipantsByCalDetailNo(@Param("calDetailNo") Integer calDetailNo);
    void insertParticipants(@Param("calDetailNo") Integer calDetailNo, @Param("userNos") List<Integer> userNos);
    CalendarDetail getEventByCalDetailNo(@Param("calDetailNo") Integer calDetailNo);


}
