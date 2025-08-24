package com.spec.plun.auth.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.spec.plun.auth.service.AccessTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{

	private final AccessTokenService accessTokenService;
	private static final String[] PUBLICE_URL = {"/auth/**","/swagger-ui/**","/v3/api-docs/**","/ws-chat","/error","/attachments/**"};
	private final AntPathMatcher matcher = new AntPathMatcher();

	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		if("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
	    return Arrays.stream(PUBLICE_URL)
	                 .anyMatch(p -> matcher.match(p, path));
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
			throws ServletException,IOException{
		String path = request.getHeader("Authorization");
		
		if(path == null || !path.startsWith("Bearer ")) {
			chain.doFilter(request, response) ;
			return ;
		}
		String token = path.substring(7).trim();
		try {
			Claims claims = accessTokenService.validToken(token);
			String email = claims.get("email", String.class);
			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null,List.of());
			SecurityContextHolder.getContext().setAuthentication(auth);
			
		} catch (JwtException e) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;		}
		chain.doFilter(request, response);
	
	}
}
