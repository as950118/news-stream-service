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
import lombok.extern.slf4j.Slf4j;
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
import com.news.stream.model.Customer;
import com.news.stream.service.CustomerService;

@Slf4j
@RestController
@RequestMapping("/api/v1/news")
@Validated
@Tag(name = "News", description = "뉴스 관련 API")
public class NewsController {
    
    private final TranslatedNewsService newsService;
    private final NewsBatchProcessingService batchService;
    private final NewsProcessingStatusService statusService;
    private final NewsStreamIntegrationService streamService;
    private final CustomerService customerService;

    public NewsController(TranslatedNewsService newsService,
                         NewsBatchProcessingService batchService,
                         NewsProcessingStatusService statusService,
                         NewsStreamIntegrationService streamService,
                         CustomerService customerService) {
        this.newsService = newsService;
        this.batchService = batchService;
        this.statusService = statusService;
        this.streamService = streamService;
        this.customerService = customerService;
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
            log.error("뉴스 목록 조회 중 오류 발생", e);
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
            log.error("뉴스 스트리밍 시작 중 오류 발생: {}", id, e);
            
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

                log.info("테스트 뉴스 생성 및 전송 완료: {}", savedNews.getId());
                
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
            log.error("테스트 뉴스 생성 중 오류 발생", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                "ERROR",
                "테스트 뉴스 생성 실패: " + e.getMessage(),
                null
            ));
        }
    }
    
    @PostMapping("/test-dlq")
    @Operation(summary = "DLQ 테스트용 뉴스 생성", description = "실제 고객사 데이터 기반으로 Dead Letter Queue를 테스트합니다. 연결 상태에 따라 다른 처리를 수행합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ 테스트 뉴스 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<Object>> createDLQTestNews(
        @Parameter(description = "뉴스 제목")
        @RequestParam(defaultValue = "실제 데이터 기반 DLQ 테스트 뉴스") String title,
        
        @Parameter(description = "뉴스 내용")
        @RequestParam(defaultValue = "이것은 실제 고객사 데이터를 기반으로 한 DLQ 테스트용 뉴스입니다.") String content,
        
        @Parameter(description = "뉴스 ID (선택사항)")
        @RequestParam(required = false) String newsId,
        
        @Parameter(description = "실패 시뮬레이션 여부 (true/false)")
        @RequestParam(defaultValue = "true") String simulateFailure,
        
        @Parameter(description = "재시도 횟수")
        @RequestParam(defaultValue = "3") int retryCount) {
        
        try {
            // 뉴스 ID가 없으면 자동 생성
            if (newsId == null || newsId.trim().isEmpty()) {
                newsId = "dlq-test-" + System.currentTimeMillis();
            }
            
            boolean shouldSimulateFailure = Boolean.parseBoolean(simulateFailure);
            
            // 실제 고객사 데이터 조회
            List<Customer> allCustomers = customerService.findActiveCustomers();
            if (allCustomers.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse<Object>(
                    "ERROR",
                    "활성 고객사가 없습니다. 먼저 고객사를 생성해주세요.",
                    null
                ));
            }
            
            // 고객사별 연결 상태 분석
            Map<String, Boolean> customerConnectionStatus = new HashMap<>();
            Map<String, String> connectionDetails = new HashMap<>();
            
            for (Customer customer : allCustomers) {
                boolean isConnected = customerService.isConnectionAvailable(customer.getId());
                customerConnectionStatus.put(customer.getId(), isConnected);
                
                if (isConnected) {
                    connectionDetails.put(customer.getId(), "연결 가능");
                } else {
                    connectionDetails.put(customer.getId(), "이미 연결됨");
                }
            }
            
            // 뉴스 생성 및 저장
            TranslatedNews news = new TranslatedNews();
            news.setId(newsId);
            news.setTitle(title);
            news.setContent(content);
            news.setPublishedAt(LocalDateTime.now());
            news.setCreatedAt(LocalDateTime.now());
            news.setUpdatedAt(LocalDateTime.now());
            
            TranslatedNews savedNews = newsService.save(news);
            statusService.markAsPending(savedNews.getId());
            
            if (shouldSimulateFailure) {
                // 실제 고객사 데이터 기반 실패 시뮬레이션
                for (int i = 0; i < retryCount; i++) {
                    // 고객사별 실패 정보 시뮬레이션 (연결 상태에 따라 다르게 처리)
                    Map<String, String> failedCustomers = new HashMap<>();
                    Map<String, String> failureReasons = new HashMap<>();
                    
                    for (Customer customer : allCustomers) {
                        String customerId = customer.getId();
                        String customerName = customer.getName();
                        
                        if (customerConnectionStatus.get(customerId)) {
                            // 연결 가능한 고객사: 네트워크 오류 시뮬레이션
                            String failureReason = String.format("네트워크 오류 (시도 #%d)", i + 1);
                            failedCustomers.put(customerId, failureReason);
                            failureReasons.put(customerId, failureReason);
                        } else {
                            // 이미 연결된 고객사: 연결 해제 오류 시뮬레이션
                            String failureReason = String.format("연결 해제됨 (시도 #%d)", i + 1);
                            failedCustomers.put(customerId, failureReason);
                            failureReasons.put(customerId, failureReason);
                        }
                    }
                    
                    // 실패 상태로 마킹
                    statusService.markAsFailed(
                        savedNews.getId(), 
                        String.format("DLQ 테스트용 실패 #%d: %d명 고객사 처리 실패", i + 1, allCustomers.size()),
                        "PROCESSING_ERROR",
                        failedCustomers,
                        allCustomers.size()
                    );
                    
                    if (i < retryCount - 1) {
                        // 마지막이 아니면 재시도 상태로 설정
                        statusService.incrementRetryCount(savedNews.getId());
                    }
                }
                
                // 고객사별 연결 상태 및 실패 정보 로깅
                log.info("DLQ 테스트 뉴스 생성 완료 (실패 시뮬레이션): {}", savedNews.getId());
                log.info("총 고객사 수: {}", allCustomers.size());
                
                for (Customer customer : allCustomers) {
                    String customerId = customer.getId();
                    String customerName = customer.getName();
                    boolean isConnected = customerConnectionStatus.get(customerId);
                    String connectionDetail = connectionDetails.get(customerId);

                    log.info("고객사 {} ({}): 연결 상태 = {}, 상세 = {}",
                        customerName, customerId, isConnected ? "연결 가능" : "이미 연결됨", connectionDetail);
                }
                
                return ResponseEntity.ok(new ApiResponse<Object>(
                    "SUCCESS",
                    String.format("DLQ 테스트 뉴스가 생성되었습니다. %d명 고객사, %d회 실패 후 Dead Letter Queue로 이동 예정", 
                        allCustomers.size(), retryCount),
                    Map.of(
                        "newsId", savedNews.getId(),
                        "totalCustomers", allCustomers.size(),
                        "retryCount", retryCount,
                        "customerConnectionStatus", customerConnectionStatus,
                        "connectionDetails", connectionDetails
                    )
                ));
            } else {
                // 정상 처리 시뮬레이션
                // 연결된 고객사에게만 성공적으로 전송
                Map<String, String> successfulCustomers = new HashMap<>();
                Map<String, String> failedCustomers = new HashMap<>();
                
                for (Customer customer : allCustomers) {
                    String customerId = customer.getId();
                    String customerName = customer.getName();
                    
                    if (customerConnectionStatus.get(customerId)) {
                        // 연결 가능한 고객사: 성공 처리
                        successfulCustomers.put(customerId, "성공적으로 전송됨");
                    } else {
                        // 이미 연결된 고객사: 실패 처리 (연결 상태 오류)
                        failedCustomers.put(customerId, "이미 연결된 고객사");
                    }
                }
                
                if (!successfulCustomers.isEmpty()) {
                    // 성공한 고객사가 있는 경우
                    statusService.markAsCompleted(savedNews.getId());
                    log.info("DLQ 테스트 뉴스 정상 처리 완료: {}명 고객사 성공, {}명 고객사 실패",
                        successfulCustomers.size(), failedCustomers.size());
                    
                    return ResponseEntity.ok(new ApiResponse<Object>(
                        "SUCCESS",
                        String.format("DLQ 테스트 뉴스가 정상적으로 처리되었습니다. %d명 고객사 성공, %d명 고객사 실패", 
                            successfulCustomers.size(), failedCustomers.size()),
                        Map.of(
                            "newsId", savedNews.getId(),
                            "successfulCustomers", successfulCustomers,
                            "failedCustomers", failedCustomers,
                            "totalCustomers", allCustomers.size()
                        )
                    ));
                } else {
                    // 모든 고객사가 실패한 경우
                    statusService.markAsFailed(
                        savedNews.getId(),
                        "모든 고객사가 이미 연결되어 있어 전송 실패",
                        "CONNECTION_ERROR",
                        failedCustomers,
                        allCustomers.size()
                    );

                    log.info("DLQ 테스트 뉴스 처리 실패: 모든 고객사가 이미 연결됨");
                    
                    return ResponseEntity.ok(new ApiResponse<Object>(
                        "SUCCESS",
                        "DLQ 테스트 뉴스가 생성되었지만, 모든 고객사가 이미 연결되어 있어 전송에 실패했습니다.",
                        Map.of(
                            "newsId", savedNews.getId(),
                            "failedCustomers", failedCustomers,
                            "totalCustomers", allCustomers.size(),
                            "failureReason", "모든 고객사가 이미 연결됨"
                        )
                    ));
                }
            }
            
        } catch (Exception e) {
            log.error("DLQ 테스트 뉴스 생성 중 오류 발생", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Object>(
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
            log.error("Dead Letter Queue 조회 중 오류 발생", e);
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
            log.error("Dead Letter Queue 통계 조회 중 오류 발생", e);
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
            log.error("Dead Letter 뉴스 제거 중 오류 발생: {}", newsId, e);
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
            log.error("Dead Letter 뉴스 상세 정보 조회 중 오류 발생: {}", newsId, e);
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
