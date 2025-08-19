package com.spec.plun.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 클라이언트가 연결할 엔드포인트 설정 (SocJs fallback 포함)
		registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*").withSockJS();
	}
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 클라언트가 메시지를 보낼 prefix
		config.setApplicationDestinationPrefixes("/app");
		// 서버에서 메시지를 브로드캐스트 할 topic prefix
		config.enableSimpleBroker("/topic");
	}

}
