package com.news.stream.controller;

import com.news.stream.dto.NewsDto;
import com.news.stream.model.TranslatedNews;
import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.service.TranslatedNewsService;
import com.news.stream.service.NewsBatchProcessingService;
import com.news.stream.service.NewsProcessingStatusService;
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
    private final NewsBatchProcessingService batchService;
    private final NewsProcessingStatusService statusService;
    
    public NewsController(TranslatedNewsService newsService,
                         NewsBatchProcessingService batchService,
                         NewsProcessingStatusService statusService) {
        this.newsService = newsService;
        this.batchService = batchService;
        this.statusService = statusService;
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
    
    /**
     * 배치 뉴스 처리 시작
     */
    @PostMapping("/batch-process")
    public ResponseEntity<String> startBatchProcessing(@RequestBody List<String> newsIds) {
        try {
            batchService.processBatchNews(newsIds);
            return ResponseEntity.ok("배치 처리 시작됨: " + newsIds.size() + "개 뉴스");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("배치 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 뉴스 처리 상태 조회
     */
    @GetMapping("/processing-status")
    public ResponseEntity<List<NewsProcessingStatus>> getProcessingStatus() {
        List<NewsProcessingStatus> allStatus = statusService.findByCreatedAtAfter(
            java.time.LocalDateTime.now().minusDays(1)
        );
        return ResponseEntity.ok(allStatus);
    }
    
    /**
     * 특정 뉴스의 처리 상태 조회
     */
    @GetMapping("/{id}/processing-status")
    public ResponseEntity<NewsProcessingStatus> getNewsProcessingStatus(@PathVariable String id) {
        Optional<NewsProcessingStatus> statusOpt = statusService.findByNewsId(id);
        return statusOpt.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 처리 상태별 뉴스 개수 조회
     */
    @GetMapping("/processing-stats")
    public ResponseEntity<java.util.Map<String, Long>> getProcessingStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("pending", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.PENDING));
        stats.put("processing", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.PROCESSING));
        stats.put("completed", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.COMPLETED));
        stats.put("failed", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.FAILED));
        stats.put("retry", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.RETRY));
        
        return ResponseEntity.ok(stats);
    }
}
