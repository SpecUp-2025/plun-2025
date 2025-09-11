package com.spec.plun.auth.oauth2.social;

import com.spec.plun.auth.oauth2.DTO.UserInfo;
import com.spec.plun.auth.oauth2.type.SocialLoginType;

public interface SocialOauth {
    String getOauthRedirectURL();
    String requestAccessToken(String code);
    UserInfo getUserInfo(String accessToken);
    default SocialLoginType type() {
        if (this instanceof GoogleOauth) {
            return SocialLoginType.google;
        } else if (this instanceof NaverOauth) {
            return SocialLoginType.naver;
        } else if (this instanceof KakaoOauth) {
            return SocialLoginType.kakao;
        } else {
            return null;
        }
    }
}