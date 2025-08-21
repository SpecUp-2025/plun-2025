package com.spec.plun.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/member")
@RequiredArgsConstructor
@RestController
public class MemberController {

    private final MemberService memberService;
   	
	@GetMapping("/me")
	public ResponseEntity<Object> getEmail (@AuthenticationPrincipal String email){
		return ResponseEntity.ok(memberService.getEmail(email));
	}

}
