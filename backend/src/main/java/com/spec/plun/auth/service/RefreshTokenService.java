package com.spec.plun.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.auth.util.JwtUtil;

import io.jsonwebtoken.Claims;

@Service
public class RefreshTokenService {
		
	private final JwtUtil jwtUtil;
	private final StringRedisTemplate stringRedisTemplate;	
			
	public RefreshTokenService (@Qualifier("refreshJwtUtil") JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate) {
	        this.jwtUtil = jwtUtil;
	        this.stringRedisTemplate = stringRedisTemplate;
	    }
	
 	private static final String PRE = "jti:";
	

	
	public String generateToken(String email) {
		String jti = UUID.randomUUID().toString();
		Map<String, Object> claims = Map.of("email",email,"jti",jti);
		Date twoWeeks = Date.from(Instant.now().plus(14,ChronoUnit.DAYS));
		whitelist(jti, twoWeeks);
		return jwtUtil.generateToken(claims, twoWeeks);
	}
	
	private void whitelist(String jti, Date twoWeeks) {
		String key = PRE + jti;
		Duration remain = Duration.between(Instant.now(), twoWeeks.toInstant());
		if(remain.isNegative()|| remain.isZero()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"토큰이 만료 되었습니다.");
		}
		stringRedisTemplate.opsForValue().set(key,"1", remain);	
		}
		
	boolean revokeByRefreshToken(String jti) {
		String key = PRE + jti;
		return stringRedisTemplate.delete(key);
	}

	public Claims validToken(String refreshToken) {
		return jwtUtil.validToken(refreshToken);
	}
	
}
