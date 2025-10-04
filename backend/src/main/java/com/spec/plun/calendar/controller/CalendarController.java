package com.spec.plun.calendar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.calendar.dto.EventRequestDTO;
import com.spec.plun.calendar.entity.Calendar;
import com.spec.plun.calendar.entity.CalendarDetail;
import com.spec.plun.calendar.service.CalendarService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
	
	private final CalendarService calendarService;
	
	// 📌 캘린더 존재 여부 확인 (존재 시 cal_no 반환)
	@GetMapping("/calno")
	public ResponseEntity<Integer> getCalendarNoByTeamAndUser(
	    @RequestParam("teamNo") Integer teamNo,
	    @RequestParam("userNo") Integer userNo) {

	    Integer calNo = calendarService.getCalNoByTeamAndUser(teamNo, userNo);
	    return ResponseEntity.ok(calNo);
	}
	@PostMapping("/create")
	public ResponseEntity<String> createCalendar(@RequestBody Calendar calendar) {
	    int result = calendarService.createCalendar(calendar);
	    if(result > 0) {
	        return ResponseEntity.ok("캘린더가 생성되었습니다.");
	    } else {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("캘린더 생성 실패");
	    }
	}
	// 일정 조회
	@GetMapping("/events")
	public List<CalendarDetail> getEventsBetween(
	    @RequestParam("userNo") Integer userNo,
	    @RequestParam("teamNo") Integer teamNo,
	    @RequestParam("start") String start,
	    @RequestParam("end") String end) {

		System.out.println("getEventsBetween called with userNo=" + userNo + ", start=" + start + ", end=" + end);
	    return calendarService.getEventsBetween(userNo, teamNo, start, end);
	}
	// 일정 등록
	@PostMapping("/event")
	public ResponseEntity<String> insertEvent(@RequestBody EventRequestDTO dto) {
	    int result = calendarService.insertSharedEvent(dto);
	    return result > 0
	        ? ResponseEntity.ok("일정이 등록되었습니다.")
	        : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("등록 실패");
	}

	// 일정 수정
	@PutMapping("/event")
	public void updateEvent(@RequestBody EventRequestDTO dto) {
	    calendarService.updateEvent(dto);
	}
	// 일정 삭제
	@DeleteMapping("/event")
	public ResponseEntity<String> deleteEvent(@RequestParam("calDetailNo") Integer calDetailNo) {
	    int result = calendarService.deleteEvent(calDetailNo);
	    return result > 0
	        ? ResponseEntity.ok("일정이 삭제되었습니다.")
	        : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 실패");
	}


}
