package com.spec.plun.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.DTO.QualificationCheckDTO;
import com.spec.plun.auth.DTO.TokenResponse;
import com.spec.plun.auth.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	
	
	@Qualifier("accessJwtUtil")
	  private final JwtUtil accessJwt;

	  @Value("${jwt.access-exp-seconds:1800}")
	  private long accessTtlSec;
	  
	
	private final QualificationService qualificationCheckService;

	public TokenResponse login(LoginDTO member) {
		QualificationCheckDTO qualification = qualificationCheckService.getByEmail(member);
		
		if(qualification == null || !qualification.getPassword().equals(member.getPassword())) {
			 throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"아이디 혹은 비밀번호가 틀렸습니다.");
		}
		var roles = List.of("ROLE_USER");
	    int minutes = (int)(accessTtlSec / 60);

	    String access = accessJwt.generateToken(
	      Map.of("sub", qualification.getEmail(), "roles", roles),
	      minutes
	    );

	    return new TokenResponse("Bearer", access, accessTtlSec);
	}

}
