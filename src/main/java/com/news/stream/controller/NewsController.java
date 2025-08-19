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
import com.news.stream.util.Profiled;
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
import java.util.Map;
import java.util.HashMap;

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
    @Profiled
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
    @Profiled
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
    
    @PostMapping("/test")
    @Operation(summary = "테스트 뉴스 생성 및 전송", description = "테스트용 뉴스를 생성하고 모든 연결된 고객사에게 전송합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 뉴스 생성 및 전송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<String>> createTestNews(
        @Parameter(description = "뉴스 제목")
        @RequestParam(defaultValue = "테스트 뉴스") String title,
        
        @Parameter(description = "뉴스 내용")
        @RequestParam(defaultValue = "이것은 테스트용 뉴스입니다.") String content,
        
        @Parameter(description = "뉴스 ID (선택사항)")
        @RequestParam(required = false) String newsId) {
        
        try {
            // 뉴스 ID가 없으면 자동 생성
            String finalNewsId = newsId != null ? newsId : "test-" + System.currentTimeMillis();
            
            // 테스트 뉴스 생성
            TranslatedNews testNews = new TranslatedNews(
                finalNewsId,
                title,
                content,
                LocalDateTime.now()
            );
            
            // 뉴스 저장
            TranslatedNews savedNews = newsService.save(testNews);
            
            // 처리 상태를 PENDING으로 설정
            statusService.markAsPending(savedNews.getId());
            
            try {
                // 뉴스 스트림 서비스로 전송하여 고객사들에게 브로드캐스트
                streamService.processNewsCreated(savedNews.getId());
                
                // 성공 시 상태를 COMPLETED로 변경
                statusService.markAsCompleted(savedNews.getId());
                
                logger.info("테스트 뉴스 생성 및 전송 완료: {}", savedNews.getId());
                
                return ResponseEntity.ok(new ApiResponse<>(
                    "SUCCESS",
                    "테스트 뉴스가 성공적으로 생성되고 전송되었습니다",
                    savedNews.getId()
                ));
                
            } catch (Exception e) {
                // 전송 실패 시 상태를 FAILED로 변경
                statusService.markAsFailed(savedNews.getId(), "뉴스 전송 실패: " + e.getMessage());
                throw e;
            }
            
        } catch (Exception e) {
            logger.error("테스트 뉴스 생성 중 오류 발생", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                "ERROR",
                "테스트 뉴스 생성 실패: " + e.getMessage(),
                null
            ));
        }
    }
    
    @PostMapping("/test-dlq")
    @Operation(summary = "DLQ 테스트용 뉴스 생성", description = "임의로 실패한 뉴스를 생성하여 Dead Letter Queue를 테스트합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ 테스트 뉴스 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<String>> createDLQTestNews(
        @Parameter(description = "뉴스 제목")
        @RequestParam(defaultValue = "DLQ 테스트 뉴스") String title,
        
        @Parameter(description = "뉴스 내용")
        @RequestParam(defaultValue = "이것은 DLQ 테스트용 뉴스입니다.") String content,
        
        @Parameter(description = "뉴스 ID (선택사항)")
        @RequestParam(required = false) String newsId,
        
        @Parameter(description = "실패 시뮬레이션 여부 (true/false)")
        @RequestParam(defaultValue = "true") String simulateFailure,
        
        @Parameter(description = "재시도 횟수")
        @RequestParam(defaultValue = "3") int retryCount) {
        
        try {
            // 뉴스 ID가 없으면 자동 생성
            String finalNewsId = newsId != null ? newsId : "dlq-test-" + System.currentTimeMillis();
            
            // simulateFailure를 boolean으로 변환
            boolean shouldSimulateFailure = "true".equalsIgnoreCase(simulateFailure);
            
            // 테스트 뉴스 생성
            TranslatedNews testNews = new TranslatedNews(
                finalNewsId,
                title,
                content,
                LocalDateTime.now()
            );
            
            // 생성 시간과 업데이트 시간 명시적 설정
            testNews.setCreatedAt(LocalDateTime.now());
            testNews.setUpdatedAt(LocalDateTime.now());
            
            // 뉴스 저장
            TranslatedNews savedNews = newsService.save(testNews);
            
            // 처리 상태를 PENDING으로 설정
            statusService.markAsPending(savedNews.getId());
            
            if (shouldSimulateFailure) {
                // 실패 시뮬레이션: 여러 번 실패 상태로 설정하여 재시도 횟수 초과
                for (int i = 0; i < retryCount; i++) {
                    // 고객사별 실패 정보 시뮬레이션
                    Map<String, String> failedCustomers = Map.of(
                        "customer-001", "네트워크 오류",
                        "customer-002", "인증 실패",
                        "customer-003", "연결 해제됨"
                    );
                    
                    statusService.markAsFailed(
                        savedNews.getId(), 
                        String.format("DLQ 테스트용 실패 #%d: 의도적인 실패", i + 1),
                        "PROCESSING_ERROR",
                        failedCustomers,
                        5 // 총 고객사 수
                    );
                    
                    if (i < retryCount - 1) {
                        // 마지막이 아니면 재시도 상태로 설정
                        statusService.incrementRetryCount(savedNews.getId());
                    }
                }
                
                logger.info("DLQ 테스트 뉴스 생성 완료 (실패 시뮬레이션): {}", savedNews.getId());
                
                return ResponseEntity.ok(new ApiResponse<>(
                    "SUCCESS",
                    String.format("DLQ 테스트 뉴스가 생성되었습니다. %d회 실패 후 Dead Letter Queue로 이동 예정", retryCount),
                    savedNews.getId()
                ));
            } else {
                // 정상 처리
                statusService.markAsCompleted(savedNews.getId());
                
                logger.info("DLQ 테스트 뉴스 생성 완료 (정상 처리): {}", savedNews.getId());
                
                return ResponseEntity.ok(new ApiResponse<>(
                    "SUCCESS",
                    "DLQ 테스트 뉴스가 정상적으로 생성되고 처리되었습니다",
                    savedNews.getId()
                ));
            }
            
        } catch (Exception e) {
            logger.error("DLQ 테스트 뉴스 생성 중 오류 발생", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                "ERROR",
                "DLQ 테스트 뉴스 생성 실패: " + e.getMessage(),
                null
            ));
        }
    }
    
    @GetMapping("/dead-letter-queue")
    @Operation(summary = "Dead Letter Queue 목록 조회", description = "Dead Letter Queue에 있는 모든 뉴스를 조회합니다")
    public ResponseEntity<ApiResponse<List<NewsProcessingStatus>>> getDeadLetterQueue() {
        try {
            List<NewsProcessingStatus> deadLetterNews = statusService.getDeadLetterNews();
            return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Dead Letter Queue 조회 성공",
                deadLetterNews
            ));
        } catch (Exception e) {
            logger.error("Dead Letter Queue 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                "ERROR",
                "Dead Letter Queue 조회 실패: " + e.getMessage(),
                null
            ));
        }
    }
    
    @GetMapping("/dead-letter-queue/stats")
    @Operation(summary = "Dead Letter Queue 통계 조회", description = "Dead Letter Queue의 통계 정보를 조회합니다")
    public ResponseEntity<ApiResponse<NewsProcessingStatusService.DeadLetterQueueStats>> getDeadLetterQueueStats() {
        try {
            NewsProcessingStatusService.DeadLetterQueueStats stats = statusService.getDeadLetterQueueStats();
            return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Dead Letter Queue 통계 조회 성공",
                stats
            ));
        } catch (Exception e) {
            logger.error("Dead Letter Queue 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                "ERROR",
                "Dead Letter Queue 통계 조회 실패: " + e.getMessage(),
                null
            ));
        }
    }
    
    @DeleteMapping("/dead-letter-queue/{newsId}")
    @Operation(summary = "Dead Letter 뉴스 제거", description = "Dead Letter Queue에서 특정 뉴스를 제거합니다")
    public ResponseEntity<ApiResponse<String>> removeFromDeadLetterQueue(
        @Parameter(description = "뉴스 ID", required = true)
        @PathVariable String newsId) {
        
        try {
            statusService.deleteByNewsId(newsId);
            return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Dead Letter 뉴스 제거 성공",
                newsId
            ));
        } catch (Exception e) {
            logger.error("Dead Letter 뉴스 제거 중 오류 발생: {}", newsId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                "ERROR",
                "Dead Letter 뉴스 제거 실패: " + e.getMessage(),
                null
            ));
        }
    }
    
    @GetMapping("/dead-letter-queue/{newsId}/details")
    @Operation(summary = "Dead Letter 뉴스 상세 정보 조회", description = "특정 Dead Letter 뉴스의 고객사별 실패 정보를 상세히 조회합니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeadLetterNewsDetails(
        @Parameter(description = "뉴스 ID", required = true)
        @PathVariable String newsId) {
        
        try {
            Optional<NewsProcessingStatus> statusOpt = statusService.findByNewsId(newsId);
            if (statusOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            NewsProcessingStatus status = statusOpt.get();
            if (status.getStatus() != NewsProcessingStatus.ProcessingStatus.DEAD_LETTER) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "ERROR",
                    "해당 뉴스는 Dead Letter Queue에 없습니다",
                    null
                ));
            }
            
            // 고객사별 실패 정보 조회
            Map<String, String> failedCustomers = statusService.getFailedCustomerDetails(newsId);
            
            // 상세 정보 구성
            Map<String, Object> details = new HashMap<>();
            details.put("newsId", status.getNewsId());
            details.put("status", status.getStatus());
            details.put("failureReason", status.getFailureReason());
            details.put("errorMessage", status.getErrorMessage());
            details.put("retryCount", status.getRetryCount());
            details.put("failedCustomerCount", status.getFailedCustomerCount());
            details.put("totalCustomerCount", status.getTotalCustomerCount());
            details.put("lastFailureAt", status.getLastFailureAt());
            details.put("createdAt", status.getCreatedAt());
            details.put("updatedAt", status.getUpdatedAt());
            details.put("failedCustomers", failedCustomers);
            
            return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Dead Letter 뉴스 상세 정보 조회 성공",
                details
            ));
            
        } catch (Exception e) {
            logger.error("Dead Letter 뉴스 상세 정보 조회 중 오류 발생: {}", newsId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                "ERROR",
                "Dead Letter 뉴스 상세 정보 조회 실패: " + e.getMessage(),
                null
            ));
        }
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
