package com.news.stream.service;

import com.news.stream.dto.NewsDto;
import com.news.stream.dto.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 뉴스 캐싱을 위한 서비스 클래스
 * Redis를 활용한 뉴스 데이터 캐싱 전략을 구현합니다.
 */
@Slf4j
@Service
public class NewsCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String NEWS_CACHE_PREFIX = "news:";
    private static final String NEWS_LIST_CACHE_PREFIX = "news:list:";
    private static final Duration NEWS_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration NEWS_LIST_CACHE_TTL = Duration.ofMinutes(5);
    
    public NewsCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Cacheable(value = "news", key = "#newsId", unless = "#result == null")
    public Optional<NewsDto> getNewsFromCache(String newsId) {
        String key = NEWS_CACHE_PREFIX + newsId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof NewsDto) {
                log.debug("뉴스 캐시 히트: {}", newsId);
                return Optional.of((NewsDto) cached);
            }
        } catch (Exception e) {
            log.warn("뉴스 캐시 조회 중 오류: {}", newsId, e);
        }
        
        log.debug("뉴스 캐시 미스: {}", newsId);
        return Optional.empty();
    }
    
    @CachePut(value = "news", key = "#newsDto.id")
    public void cacheNews(NewsDto newsDto) {
        String key = NEWS_CACHE_PREFIX + newsDto.id();
        try {
            redisTemplate.opsForValue().set(key, newsDto, NEWS_CACHE_TTL);
            log.debug("뉴스 캐시 저장: {}", newsDto.id());
        } catch (Exception e) {
            log.warn("뉴스 캐시 저장 중 오류: {}", newsDto.id(), e);
        }
    }
    
    @CacheEvict(value = "news", key = "#newsId")
    public void evictNews(String newsId) {
        String key = NEWS_CACHE_PREFIX + newsId;
        try {
            redisTemplate.delete(key);
            log.debug("뉴스 캐시 제거: {}", newsId);
        } catch (Exception e) {
            log.warn("뉴스 캐시 제거 중 오류: {}", newsId, e);
        }
    }
    
    @Cacheable(value = "news", key = "'list:' + #page + ':' + #size + ':' + #sortBy + ':' + #direction")
    public Optional<PageResponse<NewsDto>> getNewsListFromCache(int page, int size, 
                                                              String sortBy, String direction) {
        String key = NEWS_LIST_CACHE_PREFIX + page + ":" + size + ":" + sortBy + ":" + direction;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PageResponse) {
                log.debug("뉴스 목록 캐시 히트: page={}, size={}", page, size);
                return Optional.of((PageResponse<NewsDto>) cached);
            }
        } catch (Exception e) {
            log.warn("뉴스 목록 캐시 조회 중 오류: page={}, size={}", page, size, e);
        }
        
        log.debug("뉴스 목록 캐시 미스: page={}, size={}", page, size);
        return Optional.empty();
    }
    
    @CachePut(value = "news", key = "'list:' + #page + ':' + #size + ':' + #sortBy + ':' + #direction")
    public void cacheNewsList(PageResponse<NewsDto> newsList, int page, int size, 
                             String sortBy, String direction) {
        String key = NEWS_LIST_CACHE_PREFIX + page + ":" + size + ":" + sortBy + ":" + direction;
        try {
            redisTemplate.opsForValue().set(key, newsList, NEWS_LIST_CACHE_TTL);
            log.debug("뉴스 목록 캐시 저장: page={}, size={}", page, size);
        } catch (Exception e) {
            log.warn("뉴스 목록 캐시 저장 중 오류: page={}, size={}", page, size, e);
        }
    }
    
    @CacheEvict(value = "news", allEntries = true)
    public void evictAllNewsCache() {
        try {
            Set<String> keys = redisTemplate.keys(NEWS_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("모든 뉴스 캐시 제거: {}개", keys.size());
            }
        } catch (Exception e) {
            log.warn("모든 뉴스 캐시 제거 중 오류", e);
        }
    }
    
    public void preloadPopularNews(List<String> popularNewsIds) {
        log.debug("인기 뉴스 프리로드 시작: {}개", popularNewsIds.size());
        
        popularNewsIds.parallelStream().forEach(newsId -> {
            try {
                // 실제 구현에서는 뉴스 서비스에서 조회하여 캐시
                log.debug("인기 뉴스 프리로드: {}", newsId);
            } catch (Exception e) {
                log.warn("인기 뉴스 프리로드 실패: {}", newsId, e);
            }
        });
        
        log.debug("인기 뉴스 프리로드 완료: {}개", popularNewsIds.size());
    }
}
