package com.news.stream.controller;

import com.news.stream.dto.NewsDto;
import com.news.stream.dto.WebSocketMessage;
import com.news.stream.service.CustomMetrics;
import com.news.stream.service.NewsStreamService;
import com.news.stream.service.StructuredLogging;
import com.news.stream.service.TranslatedNewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket 컨트롤러
 * WebSocket을 통해 클라이언트로부터 메시지를 받고 응답을 보냅니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Controller
public class WebSocketController {
    
    private final NewsStreamService newsStreamService;
    private final TranslatedNewsService newsService;
    private final CustomMetrics customMetrics;
    private final StructuredLogging structuredLogging;
    private final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    public WebSocketController(NewsStreamService newsStreamService,
                              TranslatedNewsService newsService,
                              CustomMetrics customMetrics,
                              StructuredLogging structuredLogging) {
        this.newsStreamService = newsStreamService;
        this.newsService = newsService;
        this.customMetrics = customMetrics;
        this.structuredLogging = structuredLogging;
    }
    
    /**
     * 뉴스 요청 메시지를 처리합니다.
     * 
     * @param request 뉴스 요청
     * @return 뉴스 응답 메시지
     */
    @MessageMapping("/news/request")
    @SendTo("/topic/news")
    public WebSocketMessage<NewsDto> handleNewsRequest(@Payload NewsRequest request) {
        logger.debug("뉴스 요청 수신: {}", request);
        
        try {
            // 특정 뉴스 요청 처리
            var newsOpt = newsService.findById(request.newsId());
            if (newsOpt.isPresent()) {
                var news = newsOpt.get();
                var newsDto = new NewsDto(
                    news.getId(),
                    news.getTitle(),
                    news.getContent(),
                    news.getPublishedAt()
                );
                
                logger.debug("뉴스 요청 처리 완료: {}", request.newsId());
                return WebSocketMessage.of("NEWS_RESPONSE", newsDto);
            } else {
                logger.warn("요청된 뉴스를 찾을 수 없습니다: {}", request.newsId());
                customMetrics.incrementNewsNotFound();
                structuredLogging.logNewsNotFound(request.newsId(), "WebSocket 요청");
                return WebSocketMessage.of("NEWS_NOT_FOUND", null);
            }
        } catch (Exception e) {
            logger.error("뉴스 요청 처리 중 오류 발생: {}", request.newsId(), e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("WebSocket 뉴스 요청", "TranslatedNews", request.newsId(), e);
            return WebSocketMessage.of("ERROR", null);
        }
    }
    
    /**
     * 하트비트 메시지를 처리합니다.
     * 
     * @param request 하트비트 요청
     * @return 하트비트 응답 메시지
     */
    @MessageMapping("/heartbeat")
    @SendTo("/topic/heartbeat")
    public WebSocketMessage<String> handleHeartbeat(@Payload HeartbeatRequest request) {
        logger.debug("하트비트 수신: {}", request);
        
        return WebSocketMessage.of("HEARTBEAT_RESPONSE", "OK");
    }
    
    /**
     * 뉴스 구독 요청을 처리합니다.
     * 
     * @param request 구독 요청
     * @return 구독 응답 메시지
     */
    @MessageMapping("/news/subscribe")
    @SendTo("/topic/subscription")
    public WebSocketMessage<String> handleNewsSubscription(@Payload SubscriptionRequest request) {
        logger.debug("뉴스 구독 요청 수신: {}", request);
        
        // WebSocket 연결 시점에서 이미 인증이 완료되었으므로 추가 검증 불필요
        // 단순히 구독 확인 응답만 반환
        logger.info("고객사 {}의 뉴스 구독이 활성화되었습니다: {}", request.customerId(), request.category());
        return WebSocketMessage.of("SUBSCRIPTION_CONFIRMED", "뉴스 구독이 활성화되었습니다.");
    }
    
    /**
     * 뉴스 요청 DTO
     */
    public record NewsRequest(String newsId) {}
    
    /**
     * 하트비트 요청 DTO
     */
    public record HeartbeatRequest(String clientId, LocalDateTime timestamp) {}
    
    /**
     * 구독 요청 DTO
     */
    public record SubscriptionRequest(String customerId, String category) {}
}
