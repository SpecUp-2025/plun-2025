package com.spec.plun.auth.oauth2.social;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spec.plun.auth.oauth2.DTO.UserInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOauth implements SocialOauth{
	
	@Value("${spring.oauth.kakao.url}")
    private String KAKAO_SNS_BASE_URL;
	@Value("${spring.oauth.kakao.client-id}")
    private String KAKAO_SNS_CLIENT_ID;
    @Value("${spring.oauth.kakao.callback-url}")
    private String KAKAO_SNS_CALLBACK_URL;
    @Value("${spring.oauth.kakao.client-secret}")
    private String KAKAO_SNS_CLIENT_SECRET;
    @Value("${spring.oauth.kakao.token-url}")
    private String KAKAO_SNS_TOKEN_BASE_URL;
    
    
    @Override
    public String getOauthRedirectURL() {
        Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", KAKAO_SNS_CLIENT_ID);
        params.put("redirect_uri", KAKAO_SNS_CALLBACK_URL);
        params.put("scope", "account_email profile_nickname");
        params.put("prompt", "login");

        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));

        return KAKAO_SNS_BASE_URL + "?" + parameterString;
    }
    
    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
            MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8")
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", KAKAO_SNS_CLIENT_ID);
        form.add("redirect_uri", KAKAO_SNS_CALLBACK_URL); // 콘솔 등록값과 완전 동일해야 함
        form.add("code", code);
        if (KAKAO_SNS_CLIENT_SECRET != null && !KAKAO_SNS_CLIENT_SECRET.isBlank()) {
            form.add("client_secret", KAKAO_SNS_CLIENT_SECRET);
        }
        
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        ResponseEntity<String> res =
        		restTemplate.exchange(KAKAO_SNS_TOKEN_BASE_URL, HttpMethod.POST, req, String.class);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("카카오 토큰 발급 실패: " + res.getStatusCode() + " / " + res.getBody());
        }
        return res.getBody();
    }    
    @Override
    public UserInfo getUserInfo(String accessToken) {
		final String url = "https://kapi.kakao.com/v2/user/me";
		HttpHeaders headers = new HttpHeaders();
	    headers.setBearerAuth(accessToken);   
	    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

	    HttpEntity<Void> entity = new HttpEntity<>(headers);

	    RestTemplate rt = new RestTemplate();
	    ResponseEntity<String> res =
	            rt.exchange(url, HttpMethod.GET, entity, String.class);

	    if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
	        throw new IllegalStateException("카카오 사용자 정보 조회 실패: " + res.getStatusCode());
	    }
	    try {
	        ObjectMapper om = new ObjectMapper();
	        JsonNode root = om.readTree(res.getBody());
	        JsonNode account = root.path("kakao_account");

	        String email = account.path("email").asText(null);
	        String name  = account.path("profile").path("nickname").asText(null);

	        if (email == null || email.isBlank()) {
	          throw new ResponseStatusException(
	            HttpStatus.BAD_REQUEST,
	            "카카오 이메일을 가져오지 못했습니다. 동의 항목(이메일 제공)을 확인해주세요."
	          );
	        }
	        return new UserInfo(email, name);
	      } catch (Exception e) {
	        throw new IllegalStateException("카카오 사용자 정보 파싱 실패", e);
	      }	
	    }

}