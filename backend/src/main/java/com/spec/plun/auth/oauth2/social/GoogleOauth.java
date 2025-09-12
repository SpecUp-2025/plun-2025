package com.spec.plun.auth.oauth2.social;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8"));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", GOOGLE_SNS_CLIENT_ID);
        form.add("client_secret", GOOGLE_SNS_CLIENT_SECRET);
        form.add("redirect_uri", GOOGLE_SNS_CALLBACK_URL);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);
        ResponseEntity<String> res = restTemplate.exchange(
                GOOGLE_SNS_TOKEN_BASE_URL, HttpMethod.POST, req, String.class);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("구글 토큰 발급 실패: " + res.getStatusCode() + " / " + res.getBody());
        }
        return res.getBody();
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
