package com.spec.plun.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.DTO.RefreshTokenRequest;
import com.spec.plun.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthService authService;
	
	@PostMapping("/login")
	public ResponseEntity<Object> login(@RequestBody LoginDTO member){
		return ResponseEntity.ok(authService.login(member));
	}
	@PostMapping("/newAcessToken")
	public ResponseEntity<Object> newAcessToken(@RequestBody RefreshTokenRequest refreshTokenRequest){
		return ResponseEntity.ok(authService.newAcessToken(refreshTokenRequest));
	}
	@PostMapping("/logout")
	public ResponseEntity<Object> logout(@RequestBody RefreshTokenRequest refreshTokenRequest){
		authService.logout(refreshTokenRequest);
		return ResponseEntity.ok().build();
	}

	}

