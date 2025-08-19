package com.news.stream.controller;

import com.news.stream.dto.NewsDto;
import com.news.stream.dto.WebSocketMessage;
import com.news.stream.model.TranslatedNews;
import com.news.stream.service.CustomMetrics;
import com.news.stream.service.NewsStreamService;
import com.news.stream.service.StructuredLogging;
import com.news.stream.service.TranslatedNewsService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Controller
public class WebSocketController {
    
    private final NewsStreamService newsStreamService;
    private final TranslatedNewsService newsService;
    private final CustomMetrics customMetrics;
    private final StructuredLogging structuredLogging;
    
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
        log.debug("뉴스 요청 수신: {}", request);
        
        try {
            // 특정 뉴스 요청 처리
            var newsOpt = newsService.findById(request.newsId());
            if (newsOpt.isPresent()) {
                var news = newsOpt.get();
                var newsDto = convertToDto(news);
                
                log.debug("뉴스 요청 처리 완료: {}", request.newsId());
                return WebSocketMessage.of("NEWS_RESPONSE", newsDto);
            } else {
                log.warn("요청된 뉴스를 찾을 수 없습니다: {}", request.newsId());
                customMetrics.incrementNewsNotFound();
                structuredLogging.logNewsNotFound(request.newsId(), "WebSocket 요청");
                return WebSocketMessage.of("NEWS_NOT_FOUND", null);
            }
        } catch (Exception e) {
            log.error("뉴스 요청 처리 중 오류 발생: {}", request.newsId(), e);
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
        log.debug("하트비트 수신: {}", request);
        
        return WebSocketMessage.of("HEARTBEAT_RESPONSE", "OK");
    }
    
    /**
     * 뉴스 구독 요청을 처리합니다.
     * 
     * @param request 구독 요청
     * @return 구독 응답 메시지
     */
    @MessageMapping("/news/subscribe")
    @SendTo("/topic/news")
    public WebSocketMessage<String> handleNewsSubscription(@Payload SubscriptionRequest request) {
        log.debug("뉴스 구독 요청 수신: {}", request);
        
        try {
            // 구독 처리 로직
            log.debug("뉴스 구독 처리 완료: {}", request.customerId());
            return WebSocketMessage.of("SUBSCRIPTION_CONFIRMED", "구독이 확인되었습니다");
        } catch (Exception e) {
            log.error("뉴스 구독 처리 중 오류 발생: {}", request.customerId(), e);
            return WebSocketMessage.of("SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다");
        }
    }
    
    /**
     * TranslatedNews를 NewsDto로 변환하는 공통 메서드
     */
    private NewsDto convertToDto(TranslatedNews news) {
        return new NewsDto(
            news.getId(),
            news.getTitle(),
            news.getContent(),
            news.getPublishedAt()
        );
    }
    
    // 내부 클래스들
    public record NewsRequest(String newsId) {}
    public record HeartbeatRequest(String customerId, LocalDateTime timestamp) {}
    public record SubscriptionRequest(String customerId, String topic) {}
}
