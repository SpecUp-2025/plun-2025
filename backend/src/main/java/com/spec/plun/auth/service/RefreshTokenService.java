package com.spec.plun.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.spec.plun.auth.util.JwtUtil;

import io.jsonwebtoken.Claims;

@Service
public class RefreshTokenService {

		private final JwtUtil jwtUtil;
		
		public RefreshTokenService(@Qualifier("refreshJwtUtil") JwtUtil jwtUtil) {
			this.jwtUtil = jwtUtil;
		}
		
		public String generateToken(String email) {
			Map<String, Object> claims = Map.of("email",email);
			Date fiveMinites = Date.from(Instant.now().plus(14,ChronoUnit.DAYS));
			return jwtUtil.generateToken(claims, fiveMinites);
		}
		
		public Claims validToken(String refreshToken) {
			return jwtUtil.validToken(refreshToken);
		}
		
}
