package com.news.stream.controller;

import com.news.stream.dto.NewsDto;
import com.news.stream.dto.PageResponse;
import com.news.stream.dto.ApiResponse;
import com.news.stream.model.TranslatedNews;
import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.service.TranslatedNewsService;
import com.news.stream.service.NewsBatchProcessingService;
import com.news.stream.service.NewsProcessingStatusService;
import com.news.stream.service.NewsStreamIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/news")
@Validated
@Tag(name = "News", description = "뉴스 관련 API")
public class NewsController {
    
    private final TranslatedNewsService newsService;
    private final NewsBatchProcessingService batchService;
    private final NewsProcessingStatusService statusService;
    private final NewsStreamIntegrationService streamService;
    private final Logger logger = LoggerFactory.getLogger(NewsController.class);
    
    public NewsController(TranslatedNewsService newsService,
                         NewsBatchProcessingService batchService,
                         NewsProcessingStatusService statusService,
                         NewsStreamIntegrationService streamService) {
        this.newsService = newsService;
        this.batchService = batchService;
        this.statusService = statusService;
        this.streamService = streamService;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "특정 뉴스 조회", description = "뉴스 ID로 특정 뉴스를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<NewsDto> getNewsById(
        @Parameter(description = "뉴스 ID", required = true)
        @PathVariable String id) {
        
        Optional<TranslatedNews> newsOpt = newsService.findById(id);
        if (newsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TranslatedNews news = newsOpt.get();
        NewsDto newsDto = convertToDto(news);
        
        return ResponseEntity.ok(newsDto);
    }
    
    @GetMapping
    @Operation(summary = "뉴스 목록 조회", description = "페이징을 지원하는 뉴스 목록을 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뉴스 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<PageResponse<NewsDto>> getNewsList(
        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "20") int size,
        
        @Parameter(description = "정렬 기준")
        @RequestParam(defaultValue = "publishedAt") String sortBy,
        
        @Parameter(description = "정렬 방향 (asc, desc)")
        @RequestParam(defaultValue = "desc") String direction,
        
        @Parameter(description = "시작 날짜 (ISO 8601 형식)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        
        @Parameter(description = "종료 날짜 (ISO 8601 형식)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<TranslatedNews> newsPage;
            if (startDate != null && endDate != null) {
                // 페이징 없이 날짜 범위로 조회
                List<TranslatedNews> newsList = newsService.findByPublishedAtBetween(startDate, endDate);
                // 수동으로 페이징 처리
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), newsList.size());
                List<TranslatedNews> pageContent = newsList.subList(start, end);
                
                PageResponse<NewsDto> response = new PageResponse<>(
                    pageContent.stream().map(this::convertToDto).collect(Collectors.toList()),
                    page,
                    size,
                    newsList.size(),
                    (int) Math.ceil((double) newsList.size() / size)
                );
                
                return ResponseEntity.ok(response);
            } else {
                newsPage = newsService.findAllByOrderByPublishedAtDesc(pageable);
                
                List<NewsDto> newsDtos = newsPage.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                PageResponse<NewsDto> response = new PageResponse<>(
                    newsDtos,
                    newsPage.getNumber(),
                    newsPage.getSize(),
                    newsPage.getTotalElements(),
                    newsPage.getTotalPages()
                );
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            logger.error("뉴스 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/stream")
    @Operation(summary = "뉴스 스트리밍 시작", description = "특정 뉴스를 실시간으로 스트리밍합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스트리밍 시작 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<String>> startNewsStreaming(
        @Parameter(description = "뉴스 ID", required = true)
        @PathVariable String id) {
        
        try {
            // 뉴스 존재 여부 확인
            Optional<TranslatedNews> newsOpt = newsService.findById(id);
            if (newsOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // 스트리밍 시작
            streamService.processNewsCreated(id);
            
            ApiResponse<String> response = new ApiResponse<>(
                "SUCCESS",
                "뉴스 스트리밍이 시작되었습니다",
                id
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("뉴스 스트리밍 시작 중 오류 발생: {}", id, e);
            
            ApiResponse<String> response = new ApiResponse<>(
                "ERROR",
                "뉴스 스트리밍 시작 실패: " + e.getMessage(),
                null
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/recent")
    @Operation(summary = "최근 뉴스 조회", description = "최근에 발행된 뉴스를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "최근 뉴스 조회 성공")
    })
    public ResponseEntity<List<NewsDto>> getRecentNews(
        @Parameter(description = "조회할 뉴스 수")
        @RequestParam(defaultValue = "10") int limit) {
        
        List<TranslatedNews> recentNews = newsService.findRecentNews(limit);
        List<NewsDto> newsDtos = recentNews.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(newsDtos);
    }
    
    @PostMapping
    @Operation(summary = "뉴스 생성", description = "새로운 뉴스를 생성합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뉴스 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<NewsDto> createNews(@RequestBody TranslatedNews news) {
        TranslatedNews savedNews = newsService.save(news);
        NewsDto dto = convertToDto(savedNews);
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping("/batch-process")
    @Operation(summary = "배치 뉴스 처리 시작", description = "여러 뉴스를 배치로 처리합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배치 처리 시작 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배치 처리 실패")
    })
    public ResponseEntity<String> startBatchProcessing(@RequestBody List<String> newsIds) {
        try {
            batchService.processBatchNews(newsIds);
            return ResponseEntity.ok("배치 처리 시작됨: " + newsIds.size() + "개 뉴스");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("배치 처리 실패: " + e.getMessage());
        }
    }
    
    @GetMapping("/processing-status")
    @Operation(summary = "뉴스 처리 상태 조회", description = "최근 24시간 내 뉴스 처리 상태를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 상태 조회 성공")
    })
    public ResponseEntity<List<NewsProcessingStatus>> getProcessingStatus() {
        List<NewsProcessingStatus> allStatus = statusService.findByCreatedAtAfter(
            LocalDateTime.now().minusDays(1)
        );
        return ResponseEntity.ok(allStatus);
    }
    
    @GetMapping("/{id}/processing-status")
    @Operation(summary = "특정 뉴스 처리 상태 조회", description = "특정 뉴스의 처리 상태를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 상태 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "처리 상태를 찾을 수 없음")
    })
    public ResponseEntity<NewsProcessingStatus> getNewsProcessingStatus(
        @Parameter(description = "뉴스 ID", required = true)
        @PathVariable String id) {
        Optional<NewsProcessingStatus> statusOpt = statusService.findByNewsId(id);
        return statusOpt.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/processing-stats")
    @Operation(summary = "처리 상태별 통계", description = "뉴스 처리 상태별 개수를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공")
    })
    public ResponseEntity<java.util.Map<String, Long>> getProcessingStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("pending", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.PENDING));
        stats.put("processing", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.PROCESSING));
        stats.put("completed", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.COMPLETED));
        stats.put("failed", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.FAILED));
        stats.put("retry", statusService.countByStatus(NewsProcessingStatus.ProcessingStatus.RETRY));
        
        return ResponseEntity.ok(stats);
    }
    
    private NewsDto convertToDto(TranslatedNews news) {
        return new NewsDto(
            news.getId(),
            news.getTitle(),
            news.getContent(),
            news.getPublishedAt()
        );
    }
}
