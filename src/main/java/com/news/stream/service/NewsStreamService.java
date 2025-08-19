package com.news.stream.service;

import com.news.stream.dto.NewsDeletionDto;
import com.news.stream.dto.NewsDto;
import com.news.stream.dto.NewsUpdateDto;
import com.news.stream.dto.WebSocketMessage;
import com.news.stream.model.Customer;
import com.news.stream.model.TranslatedNews;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 뉴스 스트림 서비스
 * WebSocket을 통해 뉴스를 실시간으로 브로드캐스트하는 서비스입니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class NewsStreamService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final TranslatedNewsService newsService;
    private final CustomerService customerService;
    private final CustomMetrics customMetrics;
    private final StructuredLogging structuredLogging;
    private final NewsProcessingStatusService statusService;
    
    public NewsStreamService(SimpMessagingTemplate messagingTemplate,
                            TranslatedNewsService newsService,
                            CustomerService customerService,
                            CustomMetrics customMetrics,
                            StructuredLogging structuredLogging,
                            NewsProcessingStatusService statusService) {
        this.messagingTemplate = messagingTemplate;
        this.newsService = newsService;
        this.statusService = statusService;
        this.customerService = customerService;
        this.customMetrics = customMetrics;
        this.structuredLogging = structuredLogging;
    }
    
    /**
     * 뉴스를 모든 활성 고객사에게 브로드캐스트합니다.
     * 
     * @param newsId 브로드캐스트할 뉴스 ID
     */
    public void broadcastNews(String newsId) {
        try {
            log.info("뉴스 브로드캐스트 시작: {}", newsId);
            
            Optional<TranslatedNews> newsOpt = newsService.findById(newsId);
            if (newsOpt.isEmpty()) {
                log.warn("뉴스를 찾을 수 없습니다: {}", newsId);
                customMetrics.incrementNewsNotFound();
                
                // 뉴스를 찾을 수 없는 경우 상태를 FAILED로 기록
                statusService.markAsFailed(
                    newsId, 
                    "뉴스를 찾을 수 없습니다", 
                    "NEWS_NOT_FOUND",
                    Map.of("system", "뉴스 ID가 존재하지 않음"),
                    0
                );
                return;
            }
            
            TranslatedNews news = newsOpt.get();
            NewsDto newsDto = convertToDto(news);
            WebSocketMessage<NewsDto> message = WebSocketMessage.of("NEWS_CREATED", newsDto);
            
            broadcastToActiveCustomers(message, newsId, "뉴스");
            
        } catch (Exception e) {
            log.error("뉴스 브로드캐스트 중 오류 발생: {}", newsId, e);
            customMetrics.incrementNewsFailed();
            
            // 브로드캐스트 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    newsId, 
                    "뉴스 브로드캐스트 실패: " + e.getMessage(), 
                    "BROADCAST_ERROR",
                    Map.of("system", "브로드캐스트 처리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: {}", newsId, statusException);
            }
        }
    }
    
    /**
     * 뉴스 업데이트를 모든 활성 고객사에게 브로드캐스트합니다.
     * 
     * @param newsId 업데이트할 뉴스 ID
     */
    public void broadcastNewsUpdate(String newsId) {
        try {
            log.info("뉴스 업데이트 브로드캐스트 시작: {}", newsId);
            
            Optional<TranslatedNews> newsOpt = newsService.findById(newsId);
            if (newsOpt.isEmpty()) {
                log.warn("업데이트할 뉴스를 찾을 수 없습니다: {}", newsId);
                customMetrics.incrementNewsNotFound();
                
                // 뉴스를 찾을 수 없는 경우 상태를 FAILED로 기록
                statusService.markAsFailed(
                    newsId, 
                    "업데이트할 뉴스를 찾을 수 없습니다", 
                    "NEWS_NOT_FOUND",
                    Map.of("system", "뉴스 ID가 존재하지 않음"),
                    0
                );
                return;
            }
            
            TranslatedNews news = newsOpt.get();
            NewsUpdateDto updateDto = new NewsUpdateDto(
                news.getId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                "UPDATED"
            );
            
            WebSocketMessage<NewsUpdateDto> message = WebSocketMessage.of("NEWS_UPDATED", updateDto);
            
            broadcastToActiveCustomers(message, newsId, "뉴스 업데이트");
            
        } catch (Exception e) {
            log.error("뉴스 업데이트 브로드캐스트 중 오류 발생: {}", newsId, e);
            customMetrics.incrementNewsFailed();
            
            // 브로드캐스트 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    newsId, 
                    "뉴스 업데이트 브로드캐스트 실패: " + e.getMessage(), 
                    "BROADCAST_ERROR",
                    Map.of("system", "업데이트 브로드캐스트 처리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: {}", newsId, statusException);
            }
        }
    }
    
    /**
     * 뉴스 삭제를 모든 활성 고객사에게 브로드캐스트합니다.
     * 
     * @param newsId 삭제된 뉴스 ID
     */
    public void broadcastNewsDeletion(String newsId) {
        try {
            log.info("뉴스 삭제 알림 브로드캐스트 시작: {}", newsId);
            
            NewsDeletionDto deletionDto = new NewsDeletionDto(
                newsId,
                "DELETED",
                LocalDateTime.now()
            );
            
            WebSocketMessage<NewsDeletionDto> message = WebSocketMessage.of("NEWS_DELETED", deletionDto);
            
            broadcastToActiveCustomers(message, newsId, "뉴스 삭제 알림");
            
        } catch (Exception e) {
            log.error("뉴스 삭제 알림 브로드캐스트 중 오류 발생: {}", newsId, e);
            customMetrics.incrementNewsFailed();
            
            // 브로드캐스트 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    newsId, 
                    "뉴스 삭제 알림 브로드캐스트 실패: " + e.getMessage(), 
                    "BROADCAST_ERROR",
                    Map.of("system", "삭제 알림 브로드캐스트 처리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: {}", newsId, statusException);
            }
        }
    }
    
    /**
     * 활성 고객사들에게 메시지를 브로드캐스트하는 공통 메서드
     */
    private void broadcastToActiveCustomers(WebSocketMessage<?> message, String newsId, String messageType) {
        List<Customer> activeCustomers = customerService.findActiveCustomers();
        int sentCount = 0;
        int failedCount = 0;
        Map<String, String> failedCustomers = new HashMap<>();
        
        for (Customer customer : activeCustomers) {
            if (customer.getConnectionId() != null) {
                try {
                    sendToCustomer(customer.getConnectionId(), message, customer.getId());
                    sentCount++;
                } catch (Exception e) {
                    failedCount++;
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
                    failedCustomers.put(customer.getId(), errorMessage);
                    
                    log.error("고객사 {}에게 {} 전송 실패: {}", customer.getId(), messageType, errorMessage);
                    customMetrics.incrementWebSocketMessageSendFailed();
                    structuredLogging.logWebSocketMessageSendFailed(
                        customer.getConnectionId(), 
                        customer.getId(), 
                        messageType, 
                        e
                    );
                }
            }
        }
        
        // 고객사별 전송 실패가 있는 경우 상태를 FAILED로 기록
        if (!failedCustomers.isEmpty()) {
            try {
                statusService.markAsFailed(
                    newsId, 
                    String.format("%s 전송 중 %d명의 고객사에게 실패", messageType, failedCount), 
                    "CUSTOMER_DELIVERY_FAILED",
                    failedCustomers,
                    activeCustomers.size()
                );
                
                log.warn("{} 전송 중 {}명의 고객사에게 실패: {}", messageType, failedCount, newsId);
            } catch (Exception statusException) {
                log.error("고객사별 실패 정보 기록 실패: {}", newsId, statusException);
            }
        }
        
        log.info("{}이 {}명의 고객사에게 전송되었습니다 (성공: {}, 실패: {}): {}", 
                messageType, activeCustomers.size(), sentCount, failedCount, newsId);
    }
    
    /**
     * 특정 고객사에게 메시지를 전송합니다.
     * 
     * @param connectionId 고객사의 연결 ID
     * @param message 전송할 메시지
     * @param customerId 고객사 ID (로깅용)
     */
    private void sendToCustomer(String connectionId, Object message, String customerId) {
        try {
            messagingTemplate.convertAndSendToUser(
                connectionId,
                "/topic/news",
                message
            );
            log.debug("고객사 {}에게 메시지 전송 완료: {}", customerId, message);
        } catch (Exception e) {
            log.error("고객사 {}에게 메시지 전송 실패: {}", customerId, e.getMessage());
            customMetrics.incrementWebSocketMessageSendFailed();
            structuredLogging.logWebSocketMessageSendFailed(
                connectionId, 
                customerId, 
                "NEWS_MESSAGE", 
                e
            );
            throw e;
        }
    }
    
    /**
     * TranslatedNews를 NewsDto로 변환합니다.
     * 
     * @param news 변환할 뉴스
     * @return NewsDto
     */
    private NewsDto convertToDto(TranslatedNews news) {
        return new NewsDto(
            news.getId(),
            news.getTitle(),
            news.getContent(),
            news.getPublishedAt()
        );
    }
}
