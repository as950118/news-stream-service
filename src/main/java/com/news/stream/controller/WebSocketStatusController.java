package com.news.stream.controller;

import com.news.stream.dto.WebSocketStatusResponse;
import com.news.stream.websocket.WebSocketConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/websocket")
@Tag(name = "WebSocket Status", description = "WebSocket 상태 확인 API")
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
    @Operation(summary = "WebSocket 상태 조회", description = "WebSocket 연결의 전체 상태 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상태 조회 성공")
    })
    public ResponseEntity<WebSocketStatusResponse> getWebSocketStatus() {
        log.debug("WebSocket 상태 조회 요청");
        
        try {
            WebSocketStatusResponse response = new WebSocketStatusResponse(
                connectionManager.getActiveSessionCount(),
                connectionManager.getActiveCustomerCount(),
                connectionManager.getActiveSessionIds(),
                connectionManager.getActiveCustomerIds()
            );
            
            log.debug("WebSocket 상태 조회 완료: 활성 세션={}, 활성 고객사={}", 
                     response.activeSessionCount(), response.activeCustomerCount());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("WebSocket 상태 조회 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * 활성 WebSocket 세션 ID 목록을 반환합니다.
     * 
     * @return 활성 세션 ID 목록
     */
    @GetMapping("/sessions")
    @Operation(summary = "활성 세션 목록 조회", description = "현재 활성화된 WebSocket 세션 ID 목록을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "세션 목록 조회 성공")
    })
    public ResponseEntity<List<String>> getActiveSessions() {
        log.debug("활성 세션 목록 조회 요청");
        
        try {
            List<String> sessions = connectionManager.getActiveSessionIds();
            log.debug("활성 세션 목록 조회 완료: {}개", sessions.size());
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            log.error("활성 세션 목록 조회 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * 활성 고객사 ID 목록을 반환합니다.
     * 
     * @return 활성 고객사 ID 목록
     */
    @GetMapping("/customers")
    @Operation(summary = "활성 고객사 목록 조회", description = "현재 활성화된 고객사 ID 목록을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고객사 목록 조회 성공")
    })
    public ResponseEntity<List<String>> getActiveCustomers() {
        log.debug("활성 고객사 목록 조회 요청");
        
        try {
            List<String> customers = connectionManager.getActiveCustomerIds();
            log.debug("활성 고객사 목록 조회 완료: {}명", customers.size());
            return ResponseEntity.ok(customers);
            
        } catch (Exception e) {
            log.error("활성 고객사 목록 조회 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * WebSocket 연결 통계 정보를 반환합니다.
     * 
     * @return 연결 통계 정보
     */
    @GetMapping("/stats")
    @Operation(summary = "WebSocket 통계 조회", description = "WebSocket 연결의 통계 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    })
    public ResponseEntity<WebSocketStats> getWebSocketStats() {
        log.debug("WebSocket 통계 조회 요청");
        
        try {
            WebSocketStats stats = new WebSocketStats(
                connectionManager.getActiveSessionCount(),
                connectionManager.getActiveCustomerCount(),
                connectionManager.getActiveSessionCount() > 0
            );
            
            log.debug("WebSocket 통계 조회 완료: 활성 세션={}, 활성 고객사={}, 연결상태={}", 
                     stats.activeSessionCount, stats.activeCustomerCount, stats.hasActiveConnections);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("WebSocket 통계 조회 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * WebSocket 통계 정보를 담는 내부 클래스
     */
    public static class WebSocketStats {
        public final int activeSessionCount;
        public final int activeCustomerCount;
        public final boolean hasActiveConnections;
        
        public WebSocketStats(int activeSessionCount, int activeCustomerCount, boolean hasActiveConnections) {
            this.activeSessionCount = activeSessionCount;
            this.activeCustomerCount = activeCustomerCount;
            this.hasActiveConnections = hasActiveConnections;
        }
    }
}
