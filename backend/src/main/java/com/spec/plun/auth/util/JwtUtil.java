package com.spec.plun.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtUtil {

	private SecretKey secretKey;
	
	public JwtUtil(String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
	

	  public String generateToken(Map<String,Object> claims, int minutes) {
		    long now = System.currentTimeMillis();
		    return Jwts.builder()
		      .claims(claims)
		      .issuedAt(new Date(now))
		      .expiration(new Date(now + minutes * 60_000L))
		      .signWith(secretKey, Jwts.SIG.HS256)
		      .compact();
		  }

	
	public Claims vaildToken(String token){
		
		 //인증 토큰 문자열을 이용하여 클래임 객체를 얻는다
        return Jwts.parser()
        		.verifyWith(secretKey)
  				.build()
  				.parseSignedClaims(token)
  				.getPayload();

    
	}
	

	
	
}
