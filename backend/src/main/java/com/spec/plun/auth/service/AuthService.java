package com.spec.plun.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.DTO.QualificationCheckDTO;
import com.spec.plun.auth.DTO.TokenResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AccessTokenService accessTokenService;
	private final RefreshTokenService refreshTokenService;
	private final QualificationService qualificationCheckService;

	public TokenResponse login(LoginDTO member) {
		QualificationCheckDTO qualification = qualificationCheckService.getByEmail(member);
		
		if(qualification == null || !qualification.getPassword().equals(member.getPassword())) {
			 throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"아이디 혹은 비밀번호가 틀렸습니다.");
		}
	    return new TokenResponse(
	    		accessTokenService.generateToken(qualification.getEmail()),
	    		refreshTokenService.generateToken(qualification.getEmail())
	    		);
	}

}