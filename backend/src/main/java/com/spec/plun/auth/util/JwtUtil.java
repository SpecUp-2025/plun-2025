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
	  public String generateToken(Map<String,Object> claims, Date exp) {
		    return Jwts.builder()
		      .claims(claims)
		      .expiration(exp)
		      .signWith(secretKey)
		      .compact();
		  }
	
	public Claims validToken(String token){
        return Jwts.parser()
        		.verifyWith(secretKey)
  				.build()
  				.parseSignedClaims(token)
  				.getPayload();
	}
	
}
