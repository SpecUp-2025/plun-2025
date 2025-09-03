package com.spec.plun.member.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.member.DTO.SetPasswordRequest;
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
	
	@PostMapping("/setPassword")
	public ResponseEntity<Object> setPassword (@RequestBody SetPasswordRequest setPasswordRequest){
		memberService.setPassword(setPasswordRequest);
		return ResponseEntity.ok().build();
	}
	
	@PutMapping("/userDelete/{userNo}")
	public ResponseEntity<Object> userDelete(@PathVariable("userNo") Integer userNo){
		boolean suc = memberService.userDelete(userNo);
		return ResponseEntity.status(suc ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(Map.of("success", suc));
	}

}
