package com.spec.plun.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	@Bean
	  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    return http
	      .csrf(csrf -> csrf.disable())
	      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	      .authorizeHttpRequests(auth -> auth
	        .requestMatchers("/auth/**").permitAll()
	        .anyRequest().authenticated()
	      )
	      .build();
	  }
	

}