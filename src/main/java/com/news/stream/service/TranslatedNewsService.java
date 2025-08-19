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
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class TranslatedNewsService {
    
    private final TranslatedNewsRepository newsRepository;
    
    public TranslatedNewsService(TranslatedNewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }
    
    public Optional<TranslatedNews> findById(String id) {
        log.debug("뉴스 조회 요청: {}", id);
        Optional<TranslatedNews> news = newsRepository.findById(id);
        if (news.isEmpty()) {
            log.warn("뉴스를 찾을 수 없습니다: {}", id);
        }
        return news;
    }
    
    public List<TranslatedNews> findRecentNews(int limit) {
        log.debug("최근 뉴스 {}개 조회 요청", limit);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("publishedAt").descending());
        List<TranslatedNews> news = newsRepository.findAllByOrderByPublishedAtDesc(pageable).getContent();
        log.debug("최근 뉴스 {}개 조회 완료", news.size());
        return news;
    }
    
    public List<TranslatedNews> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end) {
        log.debug("기간별 뉴스 조회 요청: {} ~ {}", start, end);
        List<TranslatedNews> news = newsRepository.findByPublishedAtBetween(start, end);
        log.debug("기간별 뉴스 {}개 조회 완료", news.size());
        return news;
    }
    
    public List<TranslatedNews> findByPublishedAtAfter(LocalDateTime publishedAt) {
        log.debug("특정 시점 이후 뉴스 조회 요청: {}", publishedAt);
        List<TranslatedNews> news = newsRepository.findByPublishedAtAfter(publishedAt);
        log.debug("특정 시점 이후 뉴스 {}개 조회 완료", news.size());
        return news;
    }
    
    public Page<TranslatedNews> findAllByOrderByPublishedAtDesc(Pageable pageable) {
        log.debug("페이징 뉴스 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<TranslatedNews> newsPage = newsRepository.findAllByOrderByPublishedAtDesc(pageable);
        log.debug("페이징 뉴스 조회 완료: 총 {}개, 현재 페이지 {}개", 
                 newsPage.getTotalElements(), newsPage.getContent().size());
        return newsPage;
    }
    
    public TranslatedNews save(TranslatedNews news) {
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
    }
    
    public void deleteById(String id) {
        log.info("뉴스 삭제 요청: {}", id);
        newsRepository.deleteById(id);
        log.info("뉴스가 삭제되었습니다: {}", id);
    }
}
