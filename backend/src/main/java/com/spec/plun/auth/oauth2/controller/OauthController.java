package com.spec.plun.auth.oauth2.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.auth.DTO.TokenResponse;
import com.spec.plun.auth.oauth2.DTO.UserInfo;
import com.spec.plun.auth.oauth2.service.OauthService;
import com.spec.plun.auth.oauth2.type.SocialLoginType;
import com.spec.plun.auth.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/oauth")
@Slf4j
@RequiredArgsConstructor
public class OauthController {
	
	private final OauthService oauthService;
	private final AuthService authService;
	private final HttpServletResponse response;
	
	@GetMapping("/{name}/login")
	public void socialLogin(@PathVariable("name") SocialLoginType socialLoginType){
		log.info("sns 로그인 요청 받음 : {}", socialLoginType);
		oauthService.request(socialLoginType);
		
	}
	@GetMapping("/code/{name}")
    public void callback(@PathVariable("name") SocialLoginType socialLoginType,
    						@RequestParam(value = "error", required = false) String error,
    						 @RequestParam(value = "code", required = false) String code
                           ) throws IOException {
		
	    if (error != null || code == null || code.isBlank()) {
	        String fe = "http://localhost:5173/oauth/callback"
	                + "#error=" + URLEncoder.encode(error != null ? error : "missing_code", StandardCharsets.UTF_8);
	        response.sendRedirect(fe);
	        return;
	    }
		try {
			log.info(">> 소셜 로그인 API 서버로부터 받은 code :: {}", code);
	        UserInfo user  = oauthService.userInfo(socialLoginType, code);
	        log.info(">>  user :: {}", user);
	        TokenResponse tokens =  authService.socialLogin(user);
	        String fe = "http://localhost:5173/oauth/callback"
	                + "#access="  + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
	                + "&refresh=" + URLEncoder.encode(tokens.refreshToken(), StandardCharsets.UTF_8);
	        response.sendRedirect(fe);
		} catch (ResponseStatusException e) {
			String fe = "http://localhost:5173/oauth/callback"
	                + "#error=" + e.getStatusCode().value();
	        response.sendRedirect(fe);
		}
	        
    }
}
