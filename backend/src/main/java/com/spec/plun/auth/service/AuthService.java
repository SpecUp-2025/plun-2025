package com.spec.plun.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.DTO.QualificationCheckDTO;
import com.spec.plun.auth.DTO.RefreshTokenRequest;
import com.spec.plun.auth.DTO.TokenResponse;

import io.jsonwebtoken.Claims;
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

	public TokenResponse newAcessToken(RefreshTokenRequest refreshTokenRequest) {
		Claims claims = refreshTokenService.validToken(refreshTokenRequest.getRefreshToken());
		String jti = claims.get("jti",String.class);
		if(jti== null || !refreshTokenService.revokeByRefreshToken(jti)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"토큰이 유효하지 않습니다.");
		}
		String email = claims.get("email",String.class);
		return new TokenResponse(
	    		accessTokenService.generateToken(email),
	    		refreshTokenService.generateToken(email));
	}

	public void logout(RefreshTokenRequest refreshTokenRequest) {
		Claims claims = refreshTokenService.validToken(refreshTokenRequest.getRefreshToken());
		refreshTokenService.revokeByRefreshToken(claims.get("jti",String.class));
	}

}