package com.news.stream.service;

import com.news.stream.TestDataBuilder;
import com.news.stream.model.TranslatedNews;
import com.news.stream.repository.TranslatedNewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TranslatedNewsService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class TranslatedNewsServiceTest {
    
    @Mock
    private TranslatedNewsRepository newsRepository;
    
    @InjectMocks
    private TranslatedNewsService newsService;
    
    @BeforeEach
    void setUp() {
        // 테스트 전 초기화 작업
    }
    
    @Test
    @DisplayName("뉴스 ID로 뉴스를 찾을 수 있어야 한다")
    void shouldFindNewsById() {
        // Given
        String newsId = "test-news-001";
        TranslatedNews mockNews = TestDataBuilder.createTranslatedNews(newsId);
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(mockNews));
        
        // When
        Optional<TranslatedNews> result = newsService.findById(newsId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(newsId);
        verify(newsRepository).findById(newsId);
    }
    
    @Test
    @DisplayName("존재하지 않는 뉴스 ID로 조회 시 빈 Optional을 반환해야 한다")
    void shouldReturnEmptyWhenNewsNotFound() {
        // Given
        String newsId = "non-existent-news";
        when(newsRepository.findById(newsId)).thenReturn(Optional.empty());
        
        // When
        Optional<TranslatedNews> result = newsService.findById(newsId);
        
        // Then
        assertThat(result).isEmpty();
        verify(newsRepository).findById(newsId);
    }
    
    @Test
    @DisplayName("뉴스를 저장할 수 있어야 한다")
    void shouldSaveNews() {
        // Given
        TranslatedNews news = TestDataBuilder.createTranslatedNews("test-news-001");
        when(newsRepository.save(any(TranslatedNews.class))).thenReturn(news);
        
        // When
        TranslatedNews result = newsService.save(news);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(news.getId());
        verify(newsRepository).save(news);
    }
    
    @Test
    @DisplayName("최근 뉴스를 조회할 수 있어야 한다")
    void shouldFindRecentNews() {
        // Given
        int limit = 5;
        List<TranslatedNews> mockNewsList = TestDataBuilder.createNewsList(limit);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("publishedAt").descending());
        Page<TranslatedNews> mockPage = new PageImpl<>(mockNewsList, pageable, limit);
        
        when(newsRepository.findAllByOrderByPublishedAtDesc(pageable)).thenReturn(mockPage);
        
        // When
        List<TranslatedNews> result = newsService.findRecentNews(limit);
        
        // Then
        assertThat(result).hasSize(limit);
        verify(newsRepository).findAllByOrderByPublishedAtDesc(pageable);
    }
    
    @Test
    @DisplayName("모든 뉴스를 페이징하여 조회할 수 있어야 한다")
    void shouldFindAllNewsWithPaging() {
        // Given
        int pageSize = 10;
        int pageNumber = 0;
        List<TranslatedNews> mockNewsList = TestDataBuilder.createNewsList(pageSize);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<TranslatedNews> mockPage = new PageImpl<>(mockNewsList, pageable, 100);
        
        when(newsRepository.findAllByOrderByPublishedAtDesc(pageable)).thenReturn(mockPage);
        
        // When
        Page<TranslatedNews> result = newsService.findAllByOrderByPublishedAtDesc(pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(pageSize);
        assertThat(result.getTotalElements()).isEqualTo(100);
        verify(newsRepository).findAllByOrderByPublishedAtDesc(pageable);
    }
    
    @Test
    @DisplayName("뉴스를 삭제할 수 있어야 한다")
    void shouldDeleteNews() {
        // Given
        String newsId = "test-news-001";
        
        // When
        newsService.deleteById(newsId);
        
        // Then
        verify(newsRepository).deleteById(newsId);
    }
    
    @Test
    @DisplayName("특정 기간의 뉴스를 조회할 수 있어야 한다")
    void shouldFindNewsByPublishedAtBetween() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<TranslatedNews> mockNewsList = TestDataBuilder.createNewsList(3);
        
        when(newsRepository.findByPublishedAtBetween(start, end)).thenReturn(mockNewsList);
        
        // When
        List<TranslatedNews> result = newsService.findByPublishedAtBetween(start, end);
        
        // Then
        assertThat(result).hasSize(3);
        verify(newsRepository).findByPublishedAtBetween(start, end);
    }
    
    @Test
    @DisplayName("특정 시점 이후의 뉴스를 조회할 수 있어야 한다")
    void shouldFindNewsByPublishedAtAfter() {
        // Given
        LocalDateTime publishedAt = LocalDateTime.now().minusDays(1);
        List<TranslatedNews> mockNewsList = TestDataBuilder.createNewsList(2);
        
        when(newsRepository.findByPublishedAtAfter(publishedAt)).thenReturn(mockNewsList);
        
        // When
        List<TranslatedNews> result = newsService.findByPublishedAtAfter(publishedAt);
        
        // Then
        assertThat(result).hasSize(2);
        verify(newsRepository).findByPublishedAtAfter(publishedAt);
    }
}
