package com.spec.plun.calendar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.calendar.entity.CalendarDetail;
import com.spec.plun.calendar.service.CalendarService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
	
	private final CalendarService calendarService;
	
	// 일정 조회
	@GetMapping("/events")
	public List <CalendarDetail> getEventsBetween(@RequestParam("start") String start, @RequestParam("end") String end){
		return calendarService.getEventsBetween(start,end);
	}
	// 일정 등록
	@PostMapping("/event")
	public ResponseEntity<String> insertEvent(@RequestBody CalendarDetail detail) {
	    int result = calendarService.insertEvent(detail);
	    return result > 0
	        ? ResponseEntity.ok("일정이 등록되었습니다.")
	        : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("등록 실패");
	}
	// 일정 수정
	@PutMapping("/event")
	public void updateEvent(@RequestBody CalendarDetail calendarDetail) {
	    calendarService.updateEvent(calendarDetail);
	}

}
