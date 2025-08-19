package com.news.stream.controller;

import com.news.stream.dto.WebSocketStatusResponse;
import com.news.stream.websocket.WebSocketConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WebSocket 상태 확인 컨트롤러
 * WebSocket 연결 상태 및 통계 정보를 제공합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/websocket")
public class WebSocketStatusController {
    
    private final WebSocketConnectionManager connectionManager;
    
    public WebSocketStatusController(WebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    /**
     * WebSocket 전체 상태 정보를 반환합니다.
     * 
     * @return WebSocket 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<WebSocketStatusResponse> getWebSocketStatus() {
        WebSocketStatusResponse response = new WebSocketStatusResponse(
            connectionManager.getActiveSessionCount(),
            connectionManager.getActiveCustomerCount(),
            connectionManager.getActiveSessionIds(),
            connectionManager.getActiveCustomerIds()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 활성 WebSocket 세션 ID 목록을 반환합니다.
     * 
     * @return 활성 세션 ID 목록
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<String>> getActiveSessions() {
        return ResponseEntity.ok(connectionManager.getActiveSessionIds());
    }
    
    /**
     * 활성 고객사 ID 목록을 반환합니다.
     * 
     * @return 활성 고객사 ID 목록
     */
    @GetMapping("/customers")
    public ResponseEntity<List<String>> getActiveCustomers() {
        return ResponseEntity.ok(connectionManager.getActiveCustomerIds());
    }
    
    /**
     * WebSocket 연결 통계 정보를 반환합니다.
     * 
     * @return 연결 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getWebSocketStats() {
        var stats = new Object() {
            public final int activeSessionCount = connectionManager.getActiveSessionCount();
            public final int activeCustomerCount = connectionManager.getActiveCustomerCount();
            public final boolean hasActiveConnections = connectionManager.getActiveSessionCount() > 0;
        };
        
        return ResponseEntity.ok(stats);
    }
}
