package com.spec.plun.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.DTO.RefreshTokenRequest;
import com.spec.plun.auth.service.AuthService;
import com.spec.plun.email.DTO.EmailRequest;
import com.spec.plun.email.DTO.VerifyCodeRequest;
import com.spec.plun.email.service.EmailService;
import com.spec.plun.member.DTO.RegisterRequest;
import com.spec.plun.member.DTO.ResetPasswordRequest;
import com.spec.plun.member.service.MemberService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
	
	private final AuthService authService;
	private final EmailService emailService;
	private final MemberService memberService;
	
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
	
	@PostMapping("/email-code")
	public ResponseEntity<Object> emailCode(@RequestBody @Valid EmailRequest EmailRequest)throws MessagingException{
		String email = EmailRequest.getEmail();
		Integer type = memberService.findtype(email);
		log.info("type = {}",type);
		if (Integer.valueOf(1).equals(type)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 가입된 메일입니다.");
			}
		
		boolean isSend = emailService.sendSimpleMessage(email);
		if (!isSend) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 코드 발급에 실패하였습니다.");
	    }
		
		return ResponseEntity.ok(Map.of("type", type==null ? 0 : type));
            	
	}
	@PostMapping("/verifyCode")
	public ResponseEntity<Object> verifyCode(@RequestBody VerifyCodeRequest verifyCodeRequest)throws MessagingException{
		boolean isSend = emailService.verifyCode(verifyCodeRequest.email(),verifyCodeRequest.code());
		if(!isSend) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("msg","인증코드가 일치하지 않습니다."));
		}
		
		return ResponseEntity.ok(Map.of("msg", "인증코드가 일치합니다."));
	}
	
	@PostMapping("/register")
	public ResponseEntity<Object> register(@RequestBody @Valid RegisterRequest registerRequest){
		authService.register(registerRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("msg","회원가입 성공"));		
	}
	
	@PutMapping("/socialRegister")
	public ResponseEntity<Object> socialRegister(@RequestBody @Valid RegisterRequest registerRequest){
		authService.socialRegister(registerRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("msg","회원가입 성공"));		
	}
	
	
	@PutMapping("/resetPassword")
	public ResponseEntity<Object> resetPassword(@RequestBody @Valid ResetPasswordRequest ResetPasswordRequest){
		authService.resetPassword(ResetPasswordRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("msg","비밀번호 재생성 성공"));		
	}
	
	@PostMapping("/reset-email-code")
	public ResponseEntity<Object> reseteEmailCode(@RequestBody @Valid EmailRequest EmailRequest)throws MessagingException{
		String email = EmailRequest.getEmail();
		boolean isEmail = memberService.findEmail(email);
		if(!isEmail) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("등록된 메일이 없습니다.");
		}
		boolean isSend = emailService.sendSimpleMessage(email);
		
		return isSend ? ResponseEntity.ok("인증 코드가 전송되었습니다.") :
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 코드 발급에 실패하였습니다.");	
	}
	
	}

