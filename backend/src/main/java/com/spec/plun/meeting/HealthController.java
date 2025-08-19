package com.spec.plun.meeting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HealthController {
	
	private final DbHealthMapper dbHealthMapper;
	
	@GetMapping("/health")
	public String health() {
		return "OK";
	}
	
	@GetMapping("/health/db")
	public String db() {
		Integer one = dbHealthMapper.selectOne();
		return (one != null && one == 1) ? "DB OK" : "DB NG";
	}
}
