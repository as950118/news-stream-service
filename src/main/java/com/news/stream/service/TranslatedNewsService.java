package com.news.stream.service;

import com.news.stream.model.TranslatedNews;
import com.news.stream.repository.TranslatedNewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TranslatedNewsService {
    
    private final TranslatedNewsRepository newsRepository;
    
    public TranslatedNewsService(TranslatedNewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }
    
    public Optional<TranslatedNews> findById(String id) {
        return newsRepository.findById(id);
    }
    
    public List<TranslatedNews> findRecentNews(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("publishedAt").descending());
        return newsRepository.findAllByOrderByPublishedAtDesc(pageable).getContent();
    }
    
    public List<TranslatedNews> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end) {
        return newsRepository.findByPublishedAtBetween(start, end);
    }
    
    public List<TranslatedNews> findByPublishedAtAfter(LocalDateTime publishedAt) {
        return newsRepository.findByPublishedAtAfter(publishedAt);
    }
    
    public Page<TranslatedNews> findAllByOrderByPublishedAtDesc(Pageable pageable) {
        return newsRepository.findAllByOrderByPublishedAtDesc(pageable);
    }
    
    public TranslatedNews save(TranslatedNews news) {
        if (news.getCreatedAt() == null) {
            news.setCreatedAt(LocalDateTime.now());
        }
        news.setUpdatedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }
    
    public void deleteById(String id) {
        newsRepository.deleteById(id);
    }
}
