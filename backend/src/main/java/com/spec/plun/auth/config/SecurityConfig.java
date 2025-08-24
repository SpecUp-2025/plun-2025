package com.spec.plun.auth.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spec.plun.auth.filter.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private static final String[] PUBLICE_URL = {"/auth/**","/swagger-ui/**","/v3/api-docs/**","/ws-chat","/error","/attachments/**","/chat/**"};
	private final ObjectMapper objectMapper;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	
	
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
		return http
			      .csrf(csrf -> csrf.disable())
			      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			      .authorizeHttpRequests(auth -> 
			      		auth.requestMatchers(PUBLICE_URL).permitAll()
			      		.anyRequest().authenticated())
			      .exceptionHandling(e -> e.
			    		  authenticationEntryPoint(unauthorized401())
			    		  .accessDeniedHandler(denied403()))
			      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			      	.build();
		}
	private AuthenticationEntryPoint unauthorized401() {
		return (request, response,authenticationEntryPoint) -> {
			if(response.isCommitted()) return;
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getOutputStream(), Map.of("error","Unauthorized"));
		};
	}

	
	private AccessDeniedHandler denied403() {
		return (request,response,accessDeniedHandler) -> {
			if(response.isCommitted()) return;
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getOutputStream(), Map.of("error","Denied"));
		};		
	}
}