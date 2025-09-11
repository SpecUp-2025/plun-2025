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
import org.springframework.web.client.RestTemplate;

import com.spec.plun.auth.oauth2.DTO.UserInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOauth implements SocialOauth{
	
	@Value("${spring.oauth.google.url}")
    private String GOOGLE_SNS_BASE_URL;
	@Value("${spring.oauth.google.client-id}")
    private String GOOGLE_SNS_CLIENT_ID;
    @Value("${spring.oauth.google.callback-url}")
    private String GOOGLE_SNS_CALLBACK_URL;
    @Value("${spring.oauth.google.client-secret}")
    private String GOOGLE_SNS_CLIENT_SECRET;
    @Value("${spring.oauth.google.token-url}")
    private String GOOGLE_SNS_TOKEN_BASE_URL;
    
    @Override
    public String getOauthRedirectURL() {
    	log.info("{}",GOOGLE_SNS_CLIENT_ID);
        Map<String, Object> params = new HashMap<>();
        params.put("scope", "openid email profile");
        params.put("prompt", "select_account");
        params.put("response_type", "code");
        params.put("client_id", GOOGLE_SNS_CLIENT_ID);
        params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);

        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));
     
        return GOOGLE_SNS_BASE_URL + "?" + parameterString;
    }
    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", GOOGLE_SNS_CLIENT_ID);
        params.put("client_secret", GOOGLE_SNS_CLIENT_SECRET);
        params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);
        params.put("grant_type", "authorization_code");

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(GOOGLE_SNS_TOKEN_BASE_URL, params, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        }
        return "구글 로그인 요청 처리 실패";
    }
    
    @Override
    public UserInfo getUserInfo(String accessToken) {
		final String url = "https://www.googleapis.com/oauth2/v2/userinfo";
		HttpHeaders headers = new HttpHeaders();
	    headers.setBearerAuth(accessToken);
	    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

	    HttpEntity<Void> entity = new HttpEntity<>(headers);

	    RestTemplate rt = new RestTemplate();
	    ResponseEntity<UserInfo> res =
	            rt.exchange(url, HttpMethod.GET, entity, UserInfo.class);

	    if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
	        throw new IllegalStateException("구글 사용자 정보 조회 실패: " + res.getStatusCode());
	    }
	    return res.getBody();
	}

    
    
}
