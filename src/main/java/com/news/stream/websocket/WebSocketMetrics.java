package com.news.stream.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * WebSocket 메트릭 컴포넌트
 * WebSocket 연결 및 메시지 전송에 대한 메트릭을 수집합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Component
public class WebSocketMetrics {
    
    private final WebSocketConnectionManager connectionManager;
    private final MeterRegistry meterRegistry;
    
    public WebSocketMetrics(WebSocketConnectionManager connectionManager,
                            MeterRegistry meterRegistry) {
        this.connectionManager = connectionManager;
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }
    
    /**
     * 메트릭을 초기화합니다.
     */
    private void initializeMetrics() {
        // 활성 세션 수 게이지
        Gauge.builder("websocket.sessions.active", connectionManager, WebSocketConnectionManager::getActiveSessionCount)
            .description("현재 활성 WebSocket 세션 수")
            .register(meterRegistry);
        
        // 활성 고객사 수 게이지
        Gauge.builder("websocket.customers.active", connectionManager, WebSocketConnectionManager::getActiveCustomerCount)
            .description("현재 활성 고객사 수")
            .register(meterRegistry);
        
        // 메시지 전송 카운터
        Counter.builder("websocket.messages.sent")
            .description("전송된 WebSocket 메시지 수")
            .register(meterRegistry);
        
        // 메시지 수신 카운터
        Counter.builder("websocket.messages.received")
            .description("수신된 WebSocket 메시지 수")
            .register(meterRegistry);
        
        // 연결 성공 카운터
        Counter.builder("websocket.connections.success")
            .description("성공한 WebSocket 연결 수")
            .register(meterRegistry);
        
        // 연결 실패 카운터
        Counter.builder("websocket.connections.failed")
            .description("실패한 WebSocket 연결 수")
            .register(meterRegistry);
        
        // 연결 해제 카운터
        Counter.builder("websocket.connections.disconnected")
            .description("해제된 WebSocket 연결 수")
            .register(meterRegistry);
    }
    
    /**
     * 메시지 전송을 기록합니다.
     */
    public void recordMessageSent() {
        Counter.builder("websocket.messages.sent")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 메시지 수신을 기록합니다.
     */
    public void recordMessageReceived() {
        Counter.builder("websocket.messages.received")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 연결 성공을 기록합니다.
     */
    public void recordConnectionSuccess() {
        Counter.builder("websocket.connections.success")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 연결 실패를 기록합니다.
     */
    public void recordConnectionFailed() {
        Counter.builder("websocket.connections.failed")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 연결 해제를 기록합니다.
     */
    public void recordConnectionDisconnected() {
        Counter.builder("websocket.connections.disconnected")
            .register(meterRegistry)
            .increment();
    }
}
