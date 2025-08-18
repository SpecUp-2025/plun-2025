package com.spec.plun.auth.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.service.AuthService;
import com.spec.plun.auth.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	
	@Qualifier("accessJwtUtil")
	private final JwtUtil jwtUtil;
	
	private final AuthService authService;
	
	@PostMapping("/login")
	public ResponseEntity<Object> login(@RequestBody LoginDTO member){
		return ResponseEntity.ok(authService.login(member));
	}
	
}
