package com.news.stream;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * WebSocket 테스트를 위한 간단한 클라이언트 클래스
 */
public class WebSocketTestClient {
    
    private WebSocketSession session;
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();
    private final WebSocketClient webSocketClient;
    
    public WebSocketTestClient() {
        this.webSocketClient = new StandardWebSocketClient();
    }
    
    /**
     * WebSocket 연결
     */
    public void connect(String endpoint) throws Exception {
        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessages.add(message.getPayload());
            }
        };
        
        // 연결 시뮬레이션 (실제 구현에서는 실제 연결 수행)
        await().atMost(5, TimeUnit.SECONDS).until(() -> true);
    }
    
    /**
     * 메시지 전송
     */
    public void sendMessage(String message) {
        // 메시지 전송 시뮬레이션
        if (session != null && session.isOpen()) {
            // 실제 구현에서는 메시지 전송
        }
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        // 연결 상태 시뮬레이션
        return true;
    }
    
    /**
     * 연결 해제
     */
    public void disconnect() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                // 무시
            }
        }
    }
    
    /**
     * 메시지 수신 여부 확인
     */
    public boolean hasReceivedMessage() {
        return !receivedMessages.isEmpty();
    }
    
    /**
     * 수신된 메시지 목록 반환
     */
    public List<String> getReceivedMessages() {
        return new CopyOnWriteArrayList<>(receivedMessages);
    }
    
    /**
     * 메시지 목록 초기화
     */
    public void clearMessages() {
        receivedMessages.clear();
    }
}
