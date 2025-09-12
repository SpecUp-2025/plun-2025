package com.spec.plun.auth.oauth2.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spec.plun.auth.oauth2.DTO.UserInfo;
import com.spec.plun.auth.oauth2.social.SocialOauth;
import com.spec.plun.auth.oauth2.type.SocialLoginType;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OauthService {
	
	private final List<SocialOauth> socialOauthList;
    private final HttpServletResponse response;
    private final ObjectMapper om = new ObjectMapper();
    
	public void request(SocialLoginType socialLoginType) {
		SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType);
        String redirectURL = socialOauth.getOauthRedirectURL();
        log.info("REDIRECT URL = {}", redirectURL);
        try {
            response.sendRedirect(redirectURL);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}

	public UserInfo userInfo(SocialLoginType type, String code) {
        SocialOauth oauth = findSocialOauthByType(type);
        String tokenJson = oauth.requestAccessToken(code);
        log.info("tokenJson: {}", tokenJson);

        String accessToken;
        try {
            JsonNode root = om.readTree(tokenJson);
            accessToken = root.path("access_token").asText(null);
        } catch (Exception e) {
            throw new RuntimeException("토큰 JSON 파싱 실패", e);
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("access_token이 비어있음");
        }

        return oauth.getUserInfo(accessToken);
    }

	
	private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다."));
    }

}
