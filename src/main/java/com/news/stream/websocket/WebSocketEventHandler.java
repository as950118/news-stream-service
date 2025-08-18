package com.news.stream.websocket;

import com.news.stream.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * WebSocket 이벤트 핸들러
 * WebSocket 연결, 해제, 구독 등의 이벤트를 처리합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Component
public class WebSocketEventHandler {
    
    private final Logger logger = LoggerFactory.getLogger(WebSocketEventHandler.class);
    private final CustomerService customerService;
    
    public WebSocketEventHandler(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * WebSocket 연결 이벤트 처리
     * 
     * @param event 연결 이벤트
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        logger.info("WebSocket 연결됨: {}", sessionId);
    }
    
    /**
     * WebSocket 연결 해제 이벤트 처리
     * 
     * @param event 연결 해제 이벤트
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        logger.info("WebSocket 연결 해제됨: {}", sessionId);
        
        // 연결 해제 시 고객사 연결 정보 정리
        try {
            customerService.removeConnectionIdBySessionId(sessionId);
            logger.info("고객사 연결 정보 정리 완료: {}", sessionId);
        } catch (Exception e) {
            logger.error("고객사 연결 정보 정리 중 오류 발생: {}", sessionId, e);
        }
    }
    
    /**
     * WebSocket 구독 이벤트 처리
     * 
     * @param event 구독 이벤트
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String destination = sha.getDestination();
        logger.info("WebSocket 구독: sessionId={}, destination={}", sessionId, destination);
    }
    
    /**
     * WebSocket 구독 해제 이벤트 처리
     * 
     * @param event 구독 해제 이벤트
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String destination = sha.getDestination();
        logger.info("WebSocket 구독 해제: sessionId={}, destination={}", sessionId, destination);
    }
}
