package com.news.stream.controller;

import com.news.stream.dto.NewsDto;
import com.news.stream.model.TranslatedNews;
import com.news.stream.service.TranslatedNewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {
    
    private final TranslatedNewsService newsService;
    
    public NewsController(TranslatedNewsService newsService) {
        this.newsService = newsService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> getNewsById(@PathVariable String id) {
        Optional<TranslatedNews> newsOpt = newsService.findById(id);
        if (newsOpt.isPresent()) {
            TranslatedNews news = newsOpt.get();
            NewsDto dto = new NewsDto(
                news.getId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt()
            );
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping
    public ResponseEntity<Page<TranslatedNews>> getAllNews(Pageable pageable) {
        Page<TranslatedNews> newsPage = newsService.findAllByOrderByPublishedAtDesc(pageable);
        return ResponseEntity.ok(newsPage);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<TranslatedNews>> getRecentNews(@RequestParam(defaultValue = "10") int limit) {
        List<TranslatedNews> recentNews = newsService.findRecentNews(limit);
        return ResponseEntity.ok(recentNews);
    }
    
    @PostMapping
    public ResponseEntity<NewsDto> createNews(@RequestBody TranslatedNews news) {
        TranslatedNews savedNews = newsService.save(news);
        NewsDto dto = new NewsDto(
            savedNews.getId(),
            savedNews.getTitle(),
            savedNews.getContent(),
            savedNews.getPublishedAt()
        );
        return ResponseEntity.ok(dto);
    }
}
