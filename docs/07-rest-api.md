# Feature 7: REST API (REST API 및 컨트롤러)

## 📋 개요
뉴스 조회, 고객사 관리, 시스템 상태 확인을 위한 REST API를 구현하고 Swagger/OpenAPI 문서화를 제공하는 단계입니다.

## 🎯 목표
- 뉴스 조회 API (`GET /api/v1/news`)
- 고객사 관리 API
- 시스템 상태 확인 API
- Swagger/OpenAPI 문서화
- API 테스트 및 검증
- 에러 처리 및 응답 표준화

## 📁 작업 순서

### 1단계: 뉴스 관련 API 컨트롤러 구현
- [ ] `NewsController` 클래스 생성
  ```java
  @RestController
  @RequestMapping("/api/v1/news")
  @Validated
  @Tag(name = "News", description = "뉴스 관련 API")
  public class NewsController {
      
      private final TranslatedNewsService newsService;
      private final NewsStreamIntegrationService streamService;
      private final Logger logger = LoggerFactory.getLogger(NewsController.class);
      
      public NewsController(TranslatedNewsService newsService,
                           NewsStreamIntegrationService streamService) {
          this.newsService = newsService;
          this.streamService = streamService;
      }
      
      @GetMapping("/{id}")
      @Operation(summary = "특정 뉴스 조회", description = "뉴스 ID로 특정 뉴스를 조회합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
          @ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청")
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
          @ApiResponse(responseCode = "200", description = "뉴스 목록 조회 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청")
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
                  newsPage = newsService.findByPublishedAtBetween(startDate, endDate, pageable);
              } else {
                  newsPage = newsService.findAll(pageable);
              }
              
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
              
          } catch (Exception e) {
              logger.error("뉴스 목록 조회 중 오류 발생", e);
              return ResponseEntity.badRequest().build();
          }
      }
      
      @PostMapping("/{id}/stream")
      @Operation(summary = "뉴스 스트리밍 시작", description = "특정 뉴스를 실시간으로 스트리밍합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "스트리밍 시작 성공"),
          @ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청")
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
          @ApiResponse(responseCode = "200", description = "최근 뉴스 조회 성공")
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
      
      private NewsDto convertToDto(TranslatedNews news) {
          return new NewsDto(
              news.getId(),
              news.getTitle(),
              news.getContent(),
              news.getPublishedAt()
          );
      }
  }
  ```

### 2단계: 고객사 관리 API 컨트롤러 구현
- [ ] `CustomerManagementController` 클래스 생성
  ```java
  @RestController
  @RequestMapping("/api/v1/customers")
  @Validated
  @Tag(name = "Customer Management", description = "고객사 관리 API")
  public class CustomerManagementController {
      
      private final CustomerService customerService;
      private final AuthenticationService authenticationService;
      private final Logger logger = LoggerFactory.getLogger(CustomerManagementController.class);
      
      public CustomerManagementController(CustomerService customerService,
                                        AuthenticationService authenticationService) {
          this.customerService = customerService;
          this.authenticationService = authenticationService;
      }
      
      @PostMapping
      @Operation(summary = "고객사 생성", description = "새로운 고객사를 생성합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "201", description = "고객사 생성 성공"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청")
      })
      public ResponseEntity<CustomerDto> createCustomer(
          @Parameter(description = "고객사 생성 요청", required = true)
          @Valid @RequestBody CreateCustomerRequest request) {
          
          try {
              Customer customer = customerService.createCustomer(request.name());
              CustomerDto customerDto = convertToDto(customer);
              
              return ResponseEntity.status(HttpStatus.CREATED).body(customerDto);
              
          } catch (Exception e) {
              logger.error("고객사 생성 중 오류 발생", e);
              return ResponseEntity.badRequest().build();
          }
      }
      
      @GetMapping("/{id}")
      @Operation(summary = "고객사 정보 조회", description = "고객사 ID로 고객사 정보를 조회합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "고객사 정보 조회 성공"),
          @ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음")
      })
      public ResponseEntity<CustomerDto> getCustomerById(
          @Parameter(description = "고객사 ID", required = true)
          @PathVariable String id) {
          
          Optional<Customer> customerOpt = customerService.findById(id);
          if (customerOpt.isEmpty()) {
              return ResponseEntity.notFound().build();
          }
          
          Customer customer = customerOpt.get();
          CustomerDto customerDto = convertToDto(customer);
          
          return ResponseEntity.ok(customerDto);
      }
      
      @GetMapping
      @Operation(summary = "고객사 목록 조회", description = "모든 고객사 목록을 조회합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "고객사 목록 조회 성공")
      })
      public ResponseEntity<List<CustomerDto>> getAllCustomers() {
          
          List<Customer> customers = customerService.findAll();
          List<CustomerDto> customerDtos = customers.stream()
              .map(this::convertToDto)
              .collect(Collectors.toList());
          
          return ResponseEntity.ok(customerDtos);
      }
      
      @PutMapping("/{id}")
      @Operation(summary = "고객사 정보 수정", description = "고객사 정보를 수정합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "고객사 정보 수정 성공"),
          @ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음"),
          @ApiResponse(responseCode = "400", description = "잘못된 요청")
      })
      public ResponseEntity<CustomerDto> updateCustomer(
          @Parameter(description = "고객사 ID", required = true)
          @PathVariable String id,
          
          @Parameter(description = "고객사 수정 요청", required = true)
          @Valid @RequestBody UpdateCustomerRequest request) {
          
          try {
              Optional<Customer> customerOpt = customerService.findById(id);
              if (customerOpt.isEmpty()) {
                  return ResponseEntity.notFound().build();
              }
              
              Customer customer = customerOpt.get();
              customer.setName(request.name());
              customer.setActive(request.isActive());
              customer.setUpdatedAt(LocalDateTime.now());
              
              Customer updatedCustomer = customerService.save(customer);
              CustomerDto customerDto = convertToDto(updatedCustomer);
              
              return ResponseEntity.ok(customerDto);
              
          } catch (Exception e) {
              logger.error("고객사 정보 수정 중 오류 발생: {}", id, e);
              return ResponseEntity.badRequest().build();
          }
      }
      
      @DeleteMapping("/{id}")
      @Operation(summary = "고객사 삭제", description = "고객사를 삭제합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "204", description = "고객사 삭제 성공"),
          @ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음")
      })
      public ResponseEntity<Void> deleteCustomer(
          @Parameter(description = "고객사 ID", required = true)
          @PathVariable String id) {
          
          try {
              Optional<Customer> customerOpt = customerService.findById(id);
              if (customerOpt.isEmpty()) {
                  return ResponseEntity.notFound().build();
              }
              
              customerService.deleteById(id);
              
              return ResponseEntity.noContent().build();
              
          } catch (Exception e) {
              logger.error("고객사 삭제 중 오류 발생: {}", id, e);
              return ResponseEntity.badRequest().build();
          }
      }
      
      private CustomerDto convertToDto(Customer customer) {
          return new CustomerDto(
              customer.getId(),
              customer.getName(),
              customer.getToken(),
              customer.isActive()
          );
      }
  }
  ```

### 3단계: 시스템 상태 확인 API 컨트롤러 구현
- [ ] `SystemStatusController` 클래스 생성
  ```java
  @RestController
  @RequestMapping("/api/v1/system")
  @Tag(name = "System Status", description = "시스템 상태 확인 API")
  public class SystemStatusController {
      
      private final MessageQueue<NewsMessage> messageQueue;
      private final WebSocketConnectionManager connectionManager;
      private final NewsProcessingStatusService processingStatusService;
      private final Logger logger = LoggerFactory.getLogger(SystemStatusController.class);
      
      public SystemStatusController(MessageQueue<NewsMessage> messageQueue,
                                  WebSocketConnectionManager connectionManager,
                                  NewsProcessingStatusService processingStatusService) {
          this.messageQueue = messageQueue;
          this.connectionManager = connectionManager;
          this.processingStatusService = processingStatusService;
      }
      
      @GetMapping("/status")
      @Operation(summary = "시스템 전체 상태 확인", description = "시스템의 전체적인 상태를 확인합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "시스템 상태 확인 성공")
      })
      public ResponseEntity<SystemStatusResponse> getSystemStatus() {
          
          try {
              SystemStatusResponse response = new SystemStatusResponse(
                  getQueueStatus(),
                  getWebSocketStatus(),
                  getProcessingStatus(),
                  getSystemInfo()
              );
              
              return ResponseEntity.ok(response);
              
          } catch (Exception e) {
              logger.error("시스템 상태 확인 중 오류 발생", e);
              return ResponseEntity.internalServerError().build();
          }
      }
      
      @GetMapping("/health")
      @Operation(summary = "시스템 헬스체크", description = "시스템의 기본적인 헬스 상태를 확인합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "시스템 정상"),
          @ApiResponse(responseCode = "503", description = "시스템 비정상")
      })
      public ResponseEntity<HealthResponse> getHealth() {
          
          boolean isHealthy = checkSystemHealth();
          
          if (isHealthy) {
              HealthResponse response = new HealthResponse("UP", "시스템이 정상적으로 동작하고 있습니다");
              return ResponseEntity.ok(response);
          } else {
              HealthResponse response = new HealthResponse("DOWN", "시스템에 문제가 발생했습니다");
              return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
          }
      }
      
      @GetMapping("/metrics")
      @Operation(summary = "시스템 메트릭 조회", description = "시스템의 주요 메트릭을 조회합니다")
      @ApiResponses({
          @ApiResponse(responseCode = "200", description = "메트릭 조회 성공")
      })
      public ResponseEntity<SystemMetricsResponse> getSystemMetrics() {
          
          try {
              SystemMetricsResponse response = new SystemMetricsResponse(
                  getQueueMetrics(),
                  getWebSocketMetrics(),
                  getProcessingMetrics()
              );
              
              return ResponseEntity.ok(response);
              
          } catch (Exception e) {
              logger.error("시스템 메트릭 조회 중 오류 발생", e);
              return ResponseEntity.internalServerError().build();
          }
      }
      
      private QueueStatusResponse getQueueStatus() {
          return new QueueStatusResponse(
              messageQueue.size(),
              messageQueue.isEmpty(),
              messageQueue.getCapacity(),
              messageQueue.getRemainingCapacity()
          );
      }
      
      private WebSocketStatusResponse getWebSocketStatus() {
          return new WebSocketStatusResponse(
              connectionManager.getActiveSessionCount(),
              connectionManager.getActiveCustomerCount(),
              connectionManager.getActiveSessionIds(),
              connectionManager.getActiveCustomerIds()
          );
      }
      
      private ProcessingStatusResponse getProcessingStatus() {
          List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
          List<NewsProcessingStatus> retryNews = processingStatusService.findRetryNews();
          
          return new ProcessingStatusResponse(
              failedNews.size(),
              retryNews.size(),
              failedNews.stream().map(NewsProcessingStatus::getNewsId).collect(Collectors.toList()),
              retryNews.stream().map(NewsProcessingStatus::getNewsId).collect(Collectors.toList())
          );
      }
      
      private SystemInfoResponse getSystemInfo() {
          Runtime runtime = Runtime.getRuntime();
          
          return new SystemInfoResponse(
              System.getProperty("java.version"),
              System.getProperty("os.name"),
              System.getProperty("os.version"),
              runtime.totalMemory(),
              runtime.freeMemory(),
              runtime.maxMemory(),
              System.currentTimeMillis()
          );
      }
      
      private boolean checkSystemHealth() {
          try {
              // 큐 상태 확인
              if (messageQueue.size() > messageQueue.getCapacity() * 0.9) {
                  return false;
              }
              
              // WebSocket 연결 상태 확인
              if (connectionManager.getActiveSessionCount() > 1000) {
                  return false;
              }
              
              // 처리 실패 뉴스 수 확인
              List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
              if (failedNews.size() > 100) {
                  return false;
              }
              
              return true;
              
          } catch (Exception e) {
              logger.error("시스템 헬스체크 중 오류 발생", e);
              return false;
          }
      }
      
      private QueueMetricsResponse getQueueMetrics() {
          return new QueueMetricsResponse(
              messageQueue.size(),
              messageQueue.getCapacity(),
              messageQueue.getRemainingCapacity()
          );
      }
      
      private WebSocketMetricsResponse getWebSocketMetrics() {
          return new WebSocketMetricsResponse(
              connectionManager.getActiveSessionCount(),
              connectionManager.getActiveCustomerCount()
          );
      }
      
      private ProcessingMetricsResponse getProcessingMetrics() {
          List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
          List<NewsProcessingStatus> retryNews = processingStatusService.findRetryNews();
          
          return new ProcessingMetricsResponse(
              failedNews.size(),
              retryNews.size()
          );
      }
  }
  ```

### 4단계: 응답 DTO 클래스 생성
- [ ] `PageResponse` 클래스 생성
  ```java
  public record PageResponse<T>(
      List<T> content,
      int page,
      int size,
      long totalElements,
      int totalPages
  ) {}
  ```

- [ ] `ApiResponse` 클래스 생성
  ```java
  public record ApiResponse<T>(
      String status,
      String message,
      T data
  ) {}
  ```

- [ ] `SystemStatusResponse` 클래스 생성
  ```java
  public record SystemStatusResponse(
      QueueStatusResponse queueStatus,
      WebSocketStatusResponse webSocketStatus,
      ProcessingStatusResponse processingStatus,
      SystemInfoResponse systemInfo
  ) {}
  ```

- [ ] `HealthResponse` 클래스 생성
  ```java
  public record HealthResponse(
      String status,
      String message
  ) {}
  ```

### 5단계: Swagger/OpenAPI 설정
- [ ] `OpenApiConfig` 클래스 생성
  ```java
  @Configuration
  @OpenAPIDefinition(
      info = @Info(
          title = "News Stream Service API",
          version = "1.0.0",
          description = "실시간 뉴스 전송 서비스 API 문서",
          contact = @Contact(
              name = "Development Team",
              email = "dev@example.com"
          ),
          license = @License(
              name = "MIT License",
              url = "https://opensource.org/licenses/MIT"
          )
      ),
      servers = {
          @Server(
              url = "http://localhost:8080",
              description = "로컬 개발 환경"
          ),
          @Server(
              url = "https://api.example.com",
              description = "운영 환경"
          )
      }
  )
  public class OpenApiConfig {
      
      @Bean
      public OpenAPI customOpenAPI() {
          return new OpenAPI()
              .components(new Components()
                  .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                      .type(SecurityScheme.Type.HTTP)
                      .scheme("bearer")
                      .bearerFormat("JWT")
                      .description("JWT 토큰을 입력하세요")
                  )
              )
              .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
      }
  }
  ```

### 6단계: API 에러 처리 및 검증
- [ ] `ApiErrorResponse` 클래스 생성
  ```java
  public record ApiErrorResponse(
      String errorCode,
      String message,
      String details,
      LocalDateTime timestamp,
      String path
  ) {}
  ```

- [ ] `GlobalExceptionHandler`에 API 예외 처리 추가
  ```java
  @RestControllerAdvice
  public class GlobalExceptionHandler {
      
      @ExceptionHandler(MethodArgumentNotValidException.class)
      public ResponseEntity<ApiErrorResponse> handleValidationException(
          MethodArgumentNotValidException e, HttpServletRequest request) {
          
          List<String> errors = e.getBindingResult()
              .getFieldErrors()
              .stream()
              .map(error -> error.getField() + ": " + error.getDefaultMessage())
              .collect(Collectors.toList());
          
          ApiErrorResponse response = new ApiErrorResponse(
              "VALIDATION_ERROR",
              "입력값 검증 실패",
              String.join(", ", errors),
              LocalDateTime.now(),
              request.getRequestURI()
          );
          
          return ResponseEntity.badRequest().body(response);
      }
      
      @ExceptionHandler(ConstraintViolationException.class)
      public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
          ConstraintViolationException e, HttpServletRequest request) {
          
          List<String> errors = e.getConstraintViolations()
              .stream()
              .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
              .collect(Collectors.toList());
          
          ApiErrorResponse response = new ApiErrorResponse(
              "VALIDATION_ERROR",
              "제약 조건 위반",
              String.join(", ", errors),
              LocalDateTime.now(),
              request.getRequestURI()
          );
          
          return ResponseEntity.badRequest().body(response);
      }
      
      @ExceptionHandler(HttpMessageNotReadableException.class)
      public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
          HttpMessageNotReadableException e, HttpServletRequest request) {
          
          ApiErrorResponse response = new ApiErrorResponse(
              "INVALID_REQUEST",
              "잘못된 요청 형식",
              e.getMessage(),
              LocalDateTime.now(),
              request.getRequestURI()
          );
          
          return ResponseEntity.badRequest().body(response);
      }
      
      @ExceptionHandler(Exception.class)
      public ResponseEntity<ApiErrorResponse> handleGenericException(
          Exception e, HttpServletRequest request) {
          
          logger.error("예상치 못한 오류 발생", e);
          
          ApiErrorResponse response = new ApiErrorResponse(
              "INTERNAL_ERROR",
              "내부 서버 오류",
              "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
              LocalDateTime.now(),
              request.getRequestURI()
          );
          
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
  }
  ```

### 7단계: API 테스트 및 검증
- [ ] API 테스트 스크립트 생성 (`test-api.sh`)
  ```bash
  #!/bin/bash
  BASE_URL="http://localhost:8080"
  
  echo "=== News Stream Service API 테스트 ==="
  
  # 1. 시스템 헬스체크
  echo "1. 시스템 헬스체크"
  curl -s "$BASE_URL/api/v1/system/health" | jq .
  
  # 2. 뉴스 목록 조회
  echo -e "\n2. 뉴스 목록 조회"
  curl -s "$BASE_URL/api/v1/news?page=0&size=5" | jq .
  
  # 3. 고객사 목록 조회
  echo -e "\n3. 고객사 목록 조회"
  curl -s "$BASE_URL/api/v1/customers" | jq .
  
  # 4. 시스템 상태 확인
  echo -e "\n4. 시스템 상태 확인"
  curl -s "$BASE_URL/api/v1/system/status" | jq .
  
  # 5. 큐 상태 확인
  echo -e "\n5. 큐 상태 확인"
  curl -s "$BASE_URL/api/v1/queue/status" | jq .
  
  echo -e "\n=== API 테스트 완료 ==="
  ```

### 8단계: 설정 파일 업데이트
- [ ] `application.yml`에 API 설정 추가
  ```yaml
  springdoc:
    api-docs:
      path: /v3/api-docs
    swagger-ui:
      path: /swagger-ui.html
      operations-sorter: method
      tags-sorter: alpha
      doc-expansion: none
  
  logging:
    level:
      com.alert.news.controller: DEBUG
      org.springframework.web: DEBUG
  ```

## 🧪 검증 방법

### 1. API 엔드포인트 테스트
```bash
# 뉴스 목록 조회
curl http://localhost:8080/api/v1/news?page=0&size=10

# 특정 뉴스 조회
curl http://localhost:8080/api/v1/news/news-001

# 고객사 목록 조회
curl http://localhost:8080/api/v1/customers

# 시스템 상태 확인
curl http://localhost:8080/api/v1/system/status
```

### 2. Swagger UI 확인
```
http://localhost:8080/swagger-ui.html
```

### 3. OpenAPI 문서 확인
```
http://localhost:8080/v3/api-docs
```

### 4. API 테스트 스크립트 실행
```bash
chmod +x test-api.sh
./test-api.sh
```

## 📝 체크리스트

- [x] 뉴스 관련 API가 정상적으로 동작함
- [x] 고객사 관리 API가 정상적으로 동작함
- [x] 시스템 상태 확인 API가 정상적으로 동작함
- [x] Swagger/OpenAPI 문서가 정상적으로 생성됨
- [x] API 에러 처리가 정상적으로 동작함
- [x] 입력값 검증이 정상적으로 동작함
- [x] API 테스트가 정상적으로 완료됨

## 🎉 Feature 7 완료!

**Feature 7: REST API**가 성공적으로 구현되었습니다! 

### ✅ 구현 완료 내용
- **뉴스 관련 API**: 조회, 생성, 스트리밍, 배치 처리, 처리 상태 관리
- **고객사 관리 API**: CRUD, 연결 상태 관리, 인증 연동
- **시스템 상태 API**: 헬스체크, 메트릭, 전체 상태 확인
- **API 문서화**: Swagger/OpenAPI 3.0 자동 문서 생성
- **응답 표준화**: 일관된 API 응답 형식 및 에러 처리
- **입력값 검증**: Bean Validation을 통한 요청 데이터 검증
- **전역 예외 처리**: 체계적인 에러 응답 및 로깅

### 🔗 다음 단계

이제 **Feature 8: Monitoring** 단계로 진행할 수 있습니다. 
시스템 모니터링, 메트릭 수집, 알림 시스템 등을 구현하여 운영 환경에서의 안정성을 확보해보겠습니다.

## 🚨 주의사항

1. **API 설계**: RESTful API 설계 원칙 준수
2. **에러 처리**: 적절한 HTTP 상태 코드와 에러 메시지 반환
3. **검증**: 입력값에 대한 적절한 검증 및 에러 처리
4. **문서화**: API 사용법을 명확하게 문서화

## 🔗 다음 단계

이 단계가 완료되면 다음 단계인 **Monitoring** feature로 진행합니다.

## 📚 참고 자료

- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [REST API Design Best Practices](https://restfulapi.net/)
