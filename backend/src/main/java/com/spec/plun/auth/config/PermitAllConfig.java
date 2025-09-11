package com.spec.plun.auth.config;

public class PermitAllConfig {
	
	public static String[]  permit_URL = {
			"/auth/**",
			"/swagger-ui/**",
			"/v3/api-docs/**",
			"/ws-chat/**","/error",
			"/attachments/**",
			"/oauth/**"
	};

}
