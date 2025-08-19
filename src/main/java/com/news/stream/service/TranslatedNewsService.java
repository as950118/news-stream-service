package com.news.stream.service;

import com.news.stream.model.TranslatedNews;
import com.news.stream.repository.TranslatedNewsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class TranslatedNewsService {
    
    private final TranslatedNewsRepository newsRepository;
    private final CustomMetrics customMetrics;
    private final StructuredLogging structuredLogging;
    private final NewsProcessingStatusService statusService;
    
    public TranslatedNewsService(TranslatedNewsRepository newsRepository,
                               CustomMetrics customMetrics,
                               StructuredLogging structuredLogging,
                               NewsProcessingStatusService statusService) {
        this.newsRepository = newsRepository;
        this.customMetrics = customMetrics;
        this.structuredLogging = structuredLogging;
        this.statusService = statusService;
    }
    
    public Optional<TranslatedNews> findById(String id) {
        log.debug("뉴스 조회 요청: {}", id);
        try {
            Optional<TranslatedNews> news = newsRepository.findById(id);
            if (news.isEmpty()) {
                log.warn("뉴스를 찾을 수 없습니다: {}", id);
                customMetrics.incrementNewsNotFound();
                structuredLogging.logNewsNotFound(id, "findById");
                
                // 뉴스를 찾을 수 없는 경우 상태를 FAILED로 기록
                statusService.markAsFailed(
                    id, 
                    "뉴스를 찾을 수 없습니다", 
                    "NEWS_NOT_FOUND",
                    Map.of("system", "뉴스 ID가 존재하지 않음"),
                    0
                );
            }
            return news;
        } catch (Exception e) {
            log.error("뉴스 조회 중 오류 발생: {}", id, e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("findById", "TranslatedNews", id, e);
            
            // DB 조회 실패 시 상태를 FAILED로 기록하여 DLQ 처리 대상으로 만듦
            try {
                statusService.markAsFailed(
                    id, 
                    "데이터베이스 조회 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 연결 또는 쿼리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: {}", id, statusException);
            }
            
            throw e;
        }
    }
    
    public List<TranslatedNews> findRecentNews(int limit) {
        log.debug("최근 뉴스 {}개 조회 요청", limit);
        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by("publishedAt").descending());
            List<TranslatedNews> news = newsRepository.findAllByOrderByPublishedAtDesc(pageable).getContent();
            log.debug("최근 뉴스 {}개 조회 완료", news.size());
            return news;
        } catch (Exception e) {
            log.error("최근 뉴스 조회 중 오류 발생: limit={}", limit, e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("findRecentNews", "TranslatedNews", "limit:" + limit, e);
            
            // DB 조회 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    "recent-news-" + System.currentTimeMillis(), 
                    "최근 뉴스 조회 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 연결 또는 쿼리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: recent-news", statusException);
            }
            
            throw e;
        }
    }
    
    public List<TranslatedNews> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end) {
        log.debug("기간별 뉴스 조회 요청: {} ~ {}", start, end);
        try {
            List<TranslatedNews> news = newsRepository.findByPublishedAtBetween(start, end);
            log.debug("기간별 뉴스 {}개 조회 완료", news.size());
            return news;
        } catch (Exception e) {
            log.error("기간별 뉴스 조회 중 오류 발생: {} ~ {}", start, end, e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("findByPublishedAtBetween", "TranslatedNews", 
                start + "~" + end, e);
            
            // DB 조회 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    "period-news-" + System.currentTimeMillis(), 
                    "기간별 뉴스 조회 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 연결 또는 쿼리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: period-news", statusException);
            }
            
            throw e;
        }
    }
    
    public List<TranslatedNews> findByPublishedAtAfter(LocalDateTime publishedAt) {
        log.debug("특정 시점 이후 뉴스 조회 요청: {}", publishedAt);
        try {
            List<TranslatedNews> news = newsRepository.findByPublishedAtAfter(publishedAt);
            log.debug("특정 시점 이후 뉴스 {}개 조회 완료", news.size());
            return news;
        } catch (Exception e) {
            log.error("특정 시점 이후 뉴스 조회 중 오류 발생: {}", publishedAt, e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("findByPublishedAtAfter", "TranslatedNews", 
                publishedAt.toString(), e);
            
            // DB 조회 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    "after-news-" + System.currentTimeMillis(), 
                    "특정 시점 이후 뉴스 조회 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 연결 또는 쿼리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: after-news", statusException);
            }
            
            throw e;
        }
    }
    
    public Page<TranslatedNews> findAllByOrderByPublishedAtDesc(Pageable pageable) {
        log.debug("페이징 뉴스 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<TranslatedNews> newsPage = newsRepository.findAllByOrderByPublishedAtDesc(pageable);
            log.debug("페이징 뉴스 조회 완료: 총 {}개, 현재 페이지 {}개", 
                     newsPage.getTotalElements(), newsPage.getContent().size());
            return newsPage;
        } catch (Exception e) {
            log.error("페이징 뉴스 조회 중 오류 발생: page={}, size={}", 
                     pageable.getPageNumber(), pageable.getPageSize(), e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("findAllByOrderByPublishedAtDesc", "TranslatedNews", 
                "page:" + pageable.getPageNumber() + ",size:" + pageable.getPageSize(), e);
            
            // DB 조회 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    "paging-news-" + System.currentTimeMillis(), 
                    "페이징 뉴스 조회 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 연결 또는 쿼리 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: paging-news", statusException);
            }
            
            throw e;
        }
    }
    
    public TranslatedNews save(TranslatedNews news) {
        try {
            if (news.getCreatedAt() == null) {
                news.setCreatedAt(LocalDateTime.now());
            }
            news.setUpdatedAt(LocalDateTime.now());
            
            TranslatedNews savedNews = newsRepository.save(news);
            if (news.getId() == null) {
                log.info("새 뉴스가 생성되었습니다: {} (ID: {})", news.getTitle(), savedNews.getId());
            } else {
                log.info("뉴스가 업데이트되었습니다: {} (ID: {})", news.getTitle(), savedNews.getId());
            }
            return savedNews;
        } catch (Exception e) {
            log.error("뉴스 저장 중 오류 발생: {}", news.getTitle(), e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("save", "TranslatedNews", 
                news.getId() != null ? news.getId() : "NEW", e);
            
            // DB 저장 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    news.getId() != null ? news.getId() : "new-news-" + System.currentTimeMillis(), 
                    "뉴스 저장 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 저장 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: save", statusException);
            }
            
            throw e;
        }
    }
    
    public void deleteById(String id) {
        log.info("뉴스 삭제 요청: {}", id);
        try {
            newsRepository.deleteById(id);
            log.info("뉴스가 삭제되었습니다: {}", id);
        } catch (Exception e) {
            log.error("뉴스 삭제 중 오류 발생: {}", id, e);
            customMetrics.incrementDatabaseQueryFailed();
            structuredLogging.logDatabaseQueryFailed("deleteById", "TranslatedNews", id, e);
            
            // DB 삭제 실패 시 상태를 FAILED로 기록
            try {
                statusService.markAsFailed(
                    id, 
                    "뉴스 삭제 실패: " + e.getMessage(), 
                    "DATABASE_ERROR",
                    Map.of("system", "데이터베이스 삭제 오류"),
                    0
                );
            } catch (Exception statusException) {
                log.error("뉴스 처리 상태 기록 실패: deleteById", statusException);
            }
            
            throw e;
        }
    }
}
