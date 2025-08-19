package com.news.stream.config;

import com.news.stream.websocket.WebSocketAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket 설정 클래스
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthenticationInterceptor authenticationInterceptor;

    public WebSocketConfig(WebSocketAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 수 있는 destination prefix 설정
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 prefix 설정
        config.setApplicationDestinationPrefixes("/app");
        
        // 특정 사용자에게 메시지를 보낼 때 사용할 prefix 설정
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결을 위한 엔드포인트 등록 (SockJS 포함)
        registry.addEndpoint("/ws/news")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // SockJS를 사용하지 않는 WebSocket 연결
        registry.addEndpoint("/ws/news")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 메시지 크기 제한 설정
        registration.setMessageSizeLimit(64 * 1024)      // 64KB
                   .setSendBufferSizeLimit(512 * 1024)   // 512KB
                   .setSendTimeLimit(20000);             // 20초
    }

    public void configureClientInboundMessageProcessing(ChannelRegistration registration) {
        // 인증 인터셉터 추가
        registration.interceptors(authenticationInterceptor);
    }
}
