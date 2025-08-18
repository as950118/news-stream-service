package com.news.stream.service;

import com.news.stream.dto.NewsDeletionDto;
import com.news.stream.dto.NewsDto;
import com.news.stream.dto.NewsUpdateDto;
import com.news.stream.dto.WebSocketMessage;
import com.news.stream.model.Customer;
import com.news.stream.model.TranslatedNews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 뉴스 스트림 서비스
 * WebSocket을 통해 뉴스를 실시간으로 브로드캐스트하는 서비스입니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Service
public class NewsStreamService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final TranslatedNewsService newsService;
    private final CustomerService customerService;
    private final Logger logger = LoggerFactory.getLogger(NewsStreamService.class);
    
    public NewsStreamService(SimpMessagingTemplate messagingTemplate,
                            TranslatedNewsService newsService,
                            CustomerService customerService) {
        this.messagingTemplate = messagingTemplate;
        this.newsService = newsService;
        this.customerService = customerService;
    }
    
    /**
     * 뉴스를 모든 활성 고객사에게 브로드캐스트합니다.
     * 
     * @param newsId 브로드캐스트할 뉴스 ID
     */
    public void broadcastNews(String newsId) {
        try {
            logger.info("뉴스 브로드캐스트 시작: {}", newsId);
            
            Optional<TranslatedNews> newsOpt = newsService.findById(newsId);
            if (newsOpt.isEmpty()) {
                logger.warn("뉴스를 찾을 수 없습니다: {}", newsId);
                return;
            }
            
            TranslatedNews news = newsOpt.get();
            NewsDto newsDto = convertToDto(news);
            WebSocketMessage<NewsDto> message = WebSocketMessage.of("NEWS_CREATED", newsDto);
            
            // 모든 활성 고객사에게 브로드캐스트
            List<Customer> activeCustomers = customerService.findActiveCustomers();
            int sentCount = 0;
            
            for (Customer customer : activeCustomers) {
                if (customer.getConnectionId() != null) {
                    try {
                        sendToCustomer(customer.getConnectionId(), message);
                        sentCount++;
                    } catch (Exception e) {
                        logger.error("고객사 {}에게 뉴스 전송 실패: {}", customer.getId(), e.getMessage());
                    }
                }
            }
            
            logger.info("뉴스가 {}명의 고객사에게 전송되었습니다: {}", sentCount, newsId);
            
        } catch (Exception e) {
            logger.error("뉴스 브로드캐스트 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * 뉴스 업데이트를 모든 활성 고객사에게 브로드캐스트합니다.
     * 
     * @param newsId 업데이트할 뉴스 ID
     */
    public void broadcastNewsUpdate(String newsId) {
        try {
            logger.info("뉴스 업데이트 브로드캐스트 시작: {}", newsId);
            
            Optional<TranslatedNews> newsOpt = newsService.findById(newsId);
            if (newsOpt.isEmpty()) {
                logger.warn("업데이트할 뉴스를 찾을 수 없습니다: {}", newsId);
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
            
            // 모든 활성 고객사에게 업데이트 알림
            List<Customer> activeCustomers = customerService.findActiveCustomers();
            int sentCount = 0;
            
            for (Customer customer : activeCustomers) {
                if (customer.getConnectionId() != null) {
                    try {
                        sendToCustomer(customer.getConnectionId(), message);
                        sentCount++;
                    } catch (Exception e) {
                        logger.error("고객사 {}에게 뉴스 업데이트 전송 실패: {}", customer.getId(), e.getMessage());
                    }
                }
            }
            
            logger.info("뉴스 업데이트가 {}명의 고객사에게 전송되었습니다: {}", sentCount, newsId);
            
        } catch (Exception e) {
            logger.error("뉴스 업데이트 브로드캐스트 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * 뉴스 삭제를 모든 활성 고객사에게 브로드캐스트합니다.
     * 
     * @param newsId 삭제된 뉴스 ID
     */
    public void broadcastNewsDeletion(String newsId) {
        try {
            logger.info("뉴스 삭제 알림 브로드캐스트 시작: {}", newsId);
            
            NewsDeletionDto deletionDto = new NewsDeletionDto(
                newsId,
                "DELETED",
                LocalDateTime.now()
            );
            
            WebSocketMessage<NewsDeletionDto> message = WebSocketMessage.of("NEWS_DELETED", deletionDto);
            
            // 모든 활성 고객사에게 삭제 알림
            List<Customer> activeCustomers = customerService.findActiveCustomers();
            int sentCount = 0;
            
            for (Customer customer : activeCustomers) {
                if (customer.getConnectionId() != null) {
                    try {
                        sendToCustomer(customer.getConnectionId(), message);
                        sentCount++;
                    } catch (Exception e) {
                        logger.error("고객사 {}에게 뉴스 삭제 알림 전송 실패: {}", customer.getId(), e.getMessage());
                    }
                }
            }
            
            logger.info("뉴스 삭제 알림이 {}명의 고객사에게 전송되었습니다: {}", sentCount, newsId);
            
        } catch (Exception e) {
            logger.error("뉴스 삭제 알림 브로드캐스트 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * 특정 고객사에게 메시지를 전송합니다.
     * 
     * @param connectionId 고객사의 연결 ID
     * @param message 전송할 메시지
     */
    private void sendToCustomer(String connectionId, Object message) {
        try {
            messagingTemplate.convertAndSendToUser(
                connectionId,
                "/topic/news",
                message
            );
            logger.debug("고객사 {}에게 메시지 전송 완료: {}", connectionId, message);
        } catch (Exception e) {
            logger.error("고객사 {}에게 메시지 전송 실패: {}", connectionId, e.getMessage());
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
