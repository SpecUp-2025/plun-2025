package com.spec.plun.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
	

	
	@RequestMapping("/secure/ping")
	public ResponseEntity<Object> ping(){return ResponseEntity.ok( Map.of("ok",true));}
}
