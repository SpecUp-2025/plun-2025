package com.spec.plun.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spec.plun.auth.util.JwtUtil;

@Configuration
public class JwtConfig {
	
	 @Bean(name = "accessJwtUtil")
	    public JwtUtil accessJwtUtil(@Value("${jwt.access-token-secret}") String accessTokenSecret) {
	        return new JwtUtil(accessTokenSecret);
	    }
}
