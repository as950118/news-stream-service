package com.news.stream.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 연결 관리 서비스
 * WebSocket 세션과 고객사 연결을 관리합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class WebSocketConnectionManager {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> customerConnections = new ConcurrentHashMap<>();
    
    /**
     * WebSocket 세션을 추가합니다.
     * 
     * @param sessionId 세션 ID
     * @param session WebSocket 세션
     */
    public void addSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
        log.info("WebSocket 세션 추가됨: {}", sessionId);
    }
    
    /**
     * WebSocket 세션을 제거합니다.
     * 
     * @param sessionId 세션 ID
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        customerConnections.remove(sessionId);
        log.info("WebSocket 세션 제거됨: {}", sessionId);
    }
    
    /**
     * 고객사를 세션과 연결합니다.
     * 
     * @param sessionId 세션 ID
     * @param customerId 고객사 ID
     */
    public void associateCustomer(String sessionId, String customerId) {
        customerConnections.put(sessionId, customerId);
        log.info("고객사 {}가 세션 {}에 연결됨", customerId, sessionId);
    }
    
    /**
     * 세션 ID로 고객사 ID를 조회합니다.
     * 
     * @param sessionId 세션 ID
     * @return 고객사 ID
     */
    public String getCustomerId(String sessionId) {
        return customerConnections.get(sessionId);
    }
    
    /**
     * 세션 ID로 WebSocket 세션을 조회합니다.
     * 
     * @param sessionId 세션 ID
     * @return WebSocket 세션
     */
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 활성 세션 수를 반환합니다.
     * 
     * @return 활성 세션 수
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * 활성 고객사 수를 반환합니다.
     * 
     * @return 활성 고객사 수
     */
    public int getActiveCustomerCount() {
        return customerConnections.size();
    }
    
    /**
     * 활성 세션 ID 목록을 반환합니다.
     * 
     * @return 활성 세션 ID 목록
     */
    public List<String> getActiveSessionIds() {
        return new ArrayList<>(sessions.keySet());
    }
    
    /**
     * 활성 고객사 ID 목록을 반환합니다.
     * 
     * @return 활성 고객사 ID 목록
     */
    public List<String> getActiveCustomerIds() {
        return new ArrayList<>(customerConnections.values());
    }
    
    /**
     * 총 연결 수를 반환합니다.
     * 
     * @return 총 연결 수
     */
    public int getTotalConnectionCount() {
        return sessions.size();
    }
    
    /**
     * 연결 상태가 정상인지 확인합니다.
     * 
     * @return 연결 상태
     */
    public boolean isHealthy() {
        return !sessions.isEmpty() && sessions.size() == customerConnections.size();
    }
    
    /**
     * 특정 고객사가 연결되어 있는지 확인합니다.
     * 
     * @param customerId 고객사 ID
     * @return 연결 여부
     */
    public boolean isCustomerConnected(String customerId) {
        return customerConnections.containsValue(customerId);
    }
    
    /**
     * 특정 세션이 활성 상태인지 확인합니다.
     * 
     * @param sessionId 세션 ID
     * @return 활성 여부
     */
    public boolean isSessionActive(String sessionId) {
        return sessions.containsKey(sessionId);
    }
    
    /**
     * 모든 세션을 정리합니다.
     */
    public void clearAllSessions() {
        int sessionCount = sessions.size();
        int customerCount = customerConnections.size();
        
        sessions.clear();
        customerConnections.clear();
        
        log.info("모든 WebSocket 세션 정리 완료: 세션 {}개, 고객사 {}개", sessionCount, customerCount);
    }
}
