package com.news.stream.repository;

import com.news.stream.model.TranslatedNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TranslatedNewsRepository extends JpaRepository<TranslatedNews, String> {
    
    List<TranslatedNews> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<TranslatedNews> findByPublishedAtAfter(LocalDateTime publishedAt);
    
    Page<TranslatedNews> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
