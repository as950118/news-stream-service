package com.news.stream.websocket;

import com.news.stream.service.CustomerService;
import com.news.stream.service.CustomMetrics;
import com.news.stream.service.StructuredLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final CustomMetrics customMetrics;
    private final StructuredLogging structuredLogging;
    
    // 연결 시도 중인 세션 추적
    private final Map<String, String> pendingConnections = new ConcurrentHashMap<>();
    // 연결 타임아웃 체크를 위한 스케줄러
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
    
    public WebSocketEventHandler(CustomerService customerService,
                               CustomMetrics customMetrics,
                               StructuredLogging structuredLogging) {
        this.customerService = customerService;
        this.customMetrics = customMetrics;
        this.structuredLogging = structuredLogging;
    }
    
    /**
     * WebSocket 연결 시도 이벤트 처리
     * 
     * @param event 연결 시도 이벤트
     */
    @EventListener
    public void handleWebSocketConnectAttempt(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        
        try {
            // 연결 시도 시 인증 토큰 확인
            String authToken = sha.getFirstNativeHeader("Authorization");
            if (authToken == null || !authToken.startsWith("Bearer ")) {
                logger.warn("인증 토큰이 없는 WebSocket 연결 시도: {}", sessionId);
                customMetrics.incrementWebSocketAuthenticationFailed();
                structuredLogging.logWebSocketAuthenticationFailed(sessionId, "인증 토큰 없음", "unknown");
                
                // 연결 실패로 기록
                customMetrics.incrementWebSocketConnectionFailed();
                structuredLogging.logWebSocketConnectionFailed(sessionId, "인증 토큰 없음", 
                    new IllegalArgumentException("인증 토큰이 없습니다"));
                return;
            }
            
            // 토큰에서 고객사 ID 추출 (실제 구현에서는 JWT 토큰 검증 필요)
            String customerId = extractCustomerIdFromToken(authToken);
            if (customerId == null) {
                logger.warn("유효하지 않은 인증 토큰으로 WebSocket 연결 시도: {}", sessionId);
                customMetrics.incrementWebSocketAuthenticationFailed();
                structuredLogging.logWebSocketAuthenticationFailed(sessionId, "유효하지 않은 토큰", "unknown");
                
                // 연결 실패로 기록
                customMetrics.incrementWebSocketConnectionFailed();
                structuredLogging.logWebSocketConnectionFailed(sessionId, "유효하지 않은 토큰", 
                    new IllegalArgumentException("토큰 형식이 올바르지 않습니다"));
                return;
            }
            
            // 고객사 유효성 검증
            if (!customerService.isValidCustomer(customerId)) {
                logger.warn("존재하지 않는 고객사 ID로 WebSocket 연결 시도: {} - {}", sessionId, customerId);
                customMetrics.incrementWebSocketAuthenticationFailed();
                structuredLogging.logWebSocketAuthenticationFailed(sessionId, "존재하지 않는 고객사", customerId);
                
                // 연결 실패로 기록
                customMetrics.incrementWebSocketConnectionFailed();
                structuredLogging.logWebSocketConnectionFailed(sessionId, "존재하지 않는 고객사", 
                    new IllegalArgumentException("존재하지 않는 고객사 ID: " + customerId));
                return;
            }
            
            // 연결 시도 성공 시 pendingConnections에 추가
            pendingConnections.put(sessionId, customerId);
            logger.info("WebSocket 연결 시도 성공: {} - 고객사: {}", sessionId, customerId);
            
            // 연결 타임아웃 체크 (10초 후 연결이 안되면 실패로 간주)
            scheduleConnectionTimeout(sessionId, customerId);
            
        } catch (Exception e) {
            logger.error("WebSocket 연결 시도 처리 중 오류 발생: {}", sessionId, e);
            customMetrics.incrementWebSocketConnectionFailed();
            structuredLogging.logWebSocketConnectionFailed(sessionId, "연결 시도 처리 실패", e);
        }
    }
    
    /**
     * 연결 타임아웃을 체크합니다.
     * 일정 시간 내에 연결이 완료되지 않으면 실패로 간주합니다.
     */
    private void scheduleConnectionTimeout(String sessionId, String customerId) {
        timeoutScheduler.schedule(() -> {
            if (pendingConnections.containsKey(sessionId)) {
                logger.warn("WebSocket 연결 타임아웃: {} - 고객사: {}", sessionId, customerId);
                customMetrics.incrementWebSocketConnectionFailed();
                structuredLogging.logWebSocketConnectionFailed(sessionId, "연결 타임아웃", 
                    new RuntimeException("연결 타임아웃 (10초)"));
                pendingConnections.remove(sessionId);
            }
        }, 10, TimeUnit.SECONDS);
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
        
        try {
            // pendingConnections에서 고객사 ID 가져오기
            String customerId = pendingConnections.get(sessionId);
            if (customerId != null) {
                // 연결 성공 시 고객사 연결 정보 저장
                customerService.associateCustomer(sessionId, customerId);
                pendingConnections.remove(sessionId);
                
                structuredLogging.logWebSocketEvent("CONNECTED", sessionId, customerId);
                logger.info("고객사 {}의 WebSocket 연결이 성공했습니다: {}", customerId, sessionId);
            } else {
                logger.warn("연결된 WebSocket 세션에 연결된 고객사가 없습니다: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("WebSocket 연결 이벤트 처리 중 오류 발생: {}", sessionId, e);
            customMetrics.incrementWebSocketConnectionFailed();
            structuredLogging.logWebSocketConnectionFailed(sessionId, "연결 이벤트 처리 실패", e);
        }
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
            String customerId = customerService.getCustomerIdBySessionId(sessionId);
            if (customerId != null) {
                structuredLogging.logWebSocketEvent("DISCONNECTED", sessionId, customerId);
                logger.info("고객사 {}의 WebSocket 연결이 해제되었습니다: {}", customerId, sessionId);
            }
            
            customerService.removeConnectionIdBySessionId(sessionId);
            logger.info("고객사 연결 정보 정리 완료: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("고객사 연결 정보 정리 중 오류 발생: {}", sessionId, e);
            customMetrics.incrementWebSocketConnectionFailed();
            structuredLogging.logWebSocketConnectionFailed(sessionId, "연결 해제 처리 실패", e);
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
        
        try {
            String customerId = customerService.getCustomerIdBySessionId(sessionId);
            if (customerId != null) {
                structuredLogging.logWebSocketEvent("SUBSCRIBED", sessionId, customerId);
                logger.info("고객사 {}가 {}에 구독했습니다: {}", customerId, destination, sessionId);
            }
        } catch (Exception e) {
            logger.error("WebSocket 구독 이벤트 처리 중 오류 발생: {}", sessionId, e);
        }
    }
    
    /**
     * WebSocket 구독 해제 이벤트 처리
     * 
     * @param event 구독 해제 이벤트
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String destination = sha.getDestination();
        logger.info("WebSocket 구독 해제: sessionId={}, destination={}", sessionId, destination);
        
        try {
            String customerId = customerService.getCustomerIdBySessionId(sessionId);
            if (customerId != null) {
                structuredLogging.logWebSocketEvent("UNSUBSCRIBED", sessionId, customerId);
                logger.info("고객사 {}가 {}에서 구독 해제했습니다: {}", customerId, destination, sessionId);
            }
        } catch (Exception e) {
            logger.error("WebSocket 구독 해제 이벤트 처리 중 오류 발생: {}", sessionId, e);
        }
    }
    
    /**
     * 인증 토큰에서 고객사 ID를 추출합니다.
     * 실제 구현에서는 JWT 토큰 검증 및 디코딩이 필요합니다.
     * 
     * @param authToken 인증 토큰
     * @return 고객사 ID 또는 null
     */
    private String extractCustomerIdFromToken(String authToken) {
        try {
            // "Bearer " 제거
            String token = authToken.substring(7);
            
            // 실제 구현에서는 JWT 토큰 검증 및 디코딩 필요
            // 여기서는 간단한 예시로 구현
            if (token.contains("customer:")) {
                return token.split("customer:")[1].split("\\.")[0];
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("토큰에서 고객사 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}
