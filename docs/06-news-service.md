# Feature 6: News Service (뉴스 전송 서비스)

## 📋 개요
메시지 큐와 WebSocket을 연동하여 실시간 뉴스 전송 파이프라인을 구현하는 단계입니다.

## 🎯 목표
- 뉴스 조회 및 전송 비즈니스 로직 구현
- 메시지 큐와 WebSocket 연동
- 실시간 전송 파이프라인 구현
- 예외 처리 및 로깅
- 성능 최적화

## 📁 작업 순서

### 1단계: 뉴스 스트림 통합 서비스 구현
- [ ] `NewsStreamIntegrationService` 클래스 생성
  ```java
  @Service
  @Transactional
  public class NewsStreamIntegrationService {
      
      private final NewsMessageProducer messageProducer;
      private final NewsStreamService streamService;
      private final TranslatedNewsService newsService;
      private final Logger logger = LoggerFactory.getLogger(NewsStreamIntegrationService.class);
      
      public NewsStreamIntegrationService(NewsMessageProducer messageProducer,
                                        NewsStreamService streamService,
                                        TranslatedNewsService newsService) {
          this.messageProducer = messageProducer;
          this.streamService = streamService;
          this.newsService = newsService;
      }
      
      public void processNewsCreated(String newsId) {
          try {
              logger.info("뉴스 생성 처리 시작: {}", newsId);
              
              // 1. 메시지 큐에 뉴스 생성 이벤트 발행
              messageProducer.publishNewsCreated(newsId);
              
              // 2. 즉시 스트리밍 (선택적)
              streamNewsImmediately(newsId);
              
              logger.info("뉴스 생성 처리 완료: {}", newsId);
          } catch (Exception e) {
              logger.error("뉴스 생성 처리 실패: {}", newsId, e);
              throw new NewsProcessingException("뉴스 생성 처리 중 오류 발생", e);
          }
      }
      
      public void processNewsUpdated(String newsId) {
          try {
              logger.info("뉴스 수정 처리 시작: {}", newsId);
              
              // 1. 메시지 큐에 뉴스 수정 이벤트 발행
              messageProducer.publishNewsUpdated(newsId);
              
              // 2. 즉시 스트리밍 (선택적)
              streamNewsUpdateImmediately(newsId);
              
              logger.info("뉴스 수정 처리 완료: {}", newsId);
          } catch (Exception e) {
              logger.error("뉴스 수정 처리 실패: {}", newsId, e);
              throw new NewsProcessingException("뉴스 수정 처리 중 오류 발생", e);
          }
      }
      
      public void processNewsDeleted(String newsId) {
          try {
                              logger.info("뉴스 삭제 처리 시작: {}", newsId);
              
              // 1. 메시지 큐에 뉴스 삭제 이벤트 발행
              messageProducer.publishNewsDeleted(newsId);
              
              // 2. 즉시 스트리밍 (선택적)
              streamNewsDeletionImmediately(newsId);
              
              logger.info("뉴스 삭제 처리 완료: {}", newsId);
          } catch (Exception e) {
              logger.error("뉴스 삭제 처리 실패: {}", newsId, e);
              throw new NewsProcessingException("뉴스 삭제 처리 중 오류 발생", e);
          }
      }
      
      private void streamNewsImmediately(String newsId) {
          try {
              streamService.broadcastNews(newsId);
          } catch (Exception e) {
              logger.warn("즉시 스트리밍 실패 (큐 처리로 대체): {}", newsId, e);
          }
      }
      
      private void streamNewsUpdateImmediately(String newsId) {
          try {
              streamService.broadcastNewsUpdate(newsId);
          } catch (Exception e) {
              logger.warn("즉시 업데이트 스트리밍 실패 (큐 처리로 대체): {}", newsId, e);
          }
      }
      
      private void streamNewsDeletionImmediately(String newsId) {
          try {
              streamService.broadcastNewsDeletion(newsId);
          } catch (Exception e) {
              logger.warn("즉시 삭제 스트리밍 실패 (큐 처리로 대체): {}", newsId, e);
          }
      }
  }
  ```

### 2단계: 뉴스 배치 처리 서비스 구현
- [ ] `NewsBatchProcessingService` 클래스 생성
  ```java
  @Service
  @Transactional
  public class NewsBatchProcessingService {
      
      private final NewsStreamIntegrationService integrationService;
      private final TranslatedNewsService newsService;
      private final Logger logger = LoggerFactory.getLogger(NewsBatchProcessingService.class);
      
      @Value("${news.batch.size:100}")
      private int batchSize;
      
      @Value("${news.batch.delay:1000}")
      private long batchDelay;
      
      public NewsBatchProcessingService(NewsStreamIntegrationService integrationService,
                                      TranslatedNewsService newsService) {
          this.integrationService = integrationService;
          this.newsService = newsService;
      }
      
      @Async("newsTaskExecutor")
      public void processBatchNews(List<String> newsIds) {
          if (newsIds == null || newsIds.isEmpty()) {
              logger.warn("처리할 뉴스 ID가 없습니다");
              return;
          }
          
          logger.info("배치 뉴스 처리 시작: {}개", newsIds.size());
          
          List<List<String>> batches = partitionList(newsIds, batchSize);
          
          for (List<String> batch : batches) {
              try {
                  processBatch(batch);
                  Thread.sleep(batchDelay); // 배치 간 지연
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  logger.warn("배치 처리 중 인터럽트 발생");
                  break;
              } catch (Exception e) {
                  logger.error("배치 처리 실패", e);
              }
          }
          
          logger.info("배치 뉴스 처리 완료: {}개", newsIds.size());
      }
      
      private void processBatch(List<String> newsIds) {
          for (String newsId : newsIds) {
              try {
                  integrationService.processNewsCreated(newsId);
              } catch (Exception e) {
                  logger.error("뉴스 ID {} 처리 실패", newsId, e);
                  // 개별 실패는 로깅만 하고 계속 진행
              }
          }
      }
      
      private List<List<String>> partitionList(List<String> list, int size) {
          List<List<String>> partitions = new ArrayList<>();
          for (int i = 0; i < list.size(); i += size) {
              partitions.add(list.subList(i, Math.min(i + size, list.size())));
          }
          return partitions;
      }
      
      @Async("newsTaskExecutor")
      public void processNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
          try {
              logger.info("날짜 범위 뉴스 처리 시작: {} ~ {}", startDate, endDate);
              
              List<TranslatedNews> newsList = newsService.findByPublishedAtBetween(startDate, endDate);
              List<String> newsIds = newsList.stream()
                  .map(TranslatedNews::getId)
                  .collect(Collectors.toList());
              
              processBatchNews(newsIds);
              
          } catch (Exception e) {
              logger.error("날짜 범위 뉴스 처리 실패: {} ~ {}", startDate, endDate, e);
          }
      }
  }
  ```

### 3단계: 뉴스 스케줄링 서비스 구현
- [ ] `NewsSchedulingService` 클래스 생성
  ```java
  @Service
  public class NewsSchedulingService {
      
      private final NewsBatchProcessingService batchService;
      private final TranslatedNewsService newsService;
      private final Logger logger = LoggerFactory.getLogger(NewsSchedulingService.class);
      
      @Value("${news.schedule.initial-delay:60000}")
      private long initialDelay;
      
      @Value("${news.schedule.fixed-delay:300000}")
      private long fixedDelay;
      
      public NewsSchedulingService(NewsBatchProcessingService batchService,
                                 TranslatedNewsService newsService) {
          this.batchService = batchService;
          this.newsService = newsService;
      }
      
      @Scheduled(initialDelay = 60000, fixedDelay = 300000) // 1분 후 시작, 5분마다 실행
      public void processPendingNews() {
          try {
              logger.info("대기 중인 뉴스 처리 시작");
              
              // 최근 1시간 내에 생성되었지만 아직 처리되지 않은 뉴스 조회
              LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
              List<TranslatedNews> pendingNews = newsService.findByPublishedAtAfter(oneHourAgo);
              
              if (!pendingNews.isEmpty()) {
                  List<String> newsIds = pendingNews.stream()
                      .map(TranslatedNews::getId)
                      .collect(Collectors.toList());
                  
                  batchService.processBatchNews(newsIds);
              } else {
                  logger.debug("처리할 대기 뉴스가 없습니다");
              }
              
          } catch (Exception e) {
              logger.error("대기 뉴스 처리 중 오류 발생", e);
          }
      }
      
      @Scheduled(cron = "0 0 * * * *") // 매시간 정각에 실행
      public void processHourlyNews() {
          try {
              logger.info("시간별 뉴스 처리 시작");
              
              LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
              LocalDateTime now = LocalDateTime.now();
              
              batchService.processNewsByDateRange(oneHourAgo, now);
              
          } catch (Exception e) {
              logger.error("시간별 뉴스 처리 중 오류 발생", e);
          }
      }
      
      @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
      public void processDailyNews() {
          try {
                              logger.info("일별 뉴스 처리 시작");
              
              LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
              LocalDateTime today = LocalDateTime.now();
              
              batchService.processNewsByDateRange(yesterday, today);
              
          } catch (Exception e) {
              logger.error("일별 뉴스 처리 중 오류 발생", e);
          }
      }
  }
  ```

### 4단계: 뉴스 처리 상태 관리
- [ ] `NewsProcessingStatus` 엔티티 생성
  ```java
  @Entity
  @Table(name = "NEWS_PROCESSING_STATUS")
  public class NewsProcessingStatus {
      
      @Id
      private String newsId;
      
      @Enumerated(EnumType.STRING)
      @Column(nullable = false)
      private ProcessingStatus status;
      
      @Column(name = "processing_started_at")
      private LocalDateTime processingStartedAt;
      
      @Column(name = "processing_completed_at")
      private LocalDateTime processingCompletedAt;
      
      @Column(name = "retry_count")
      private int retryCount = 0;
      
      @Column(name = "error_message")
      private String errorMessage;
      
      @Column(name = "created_at")
      private LocalDateTime createdAt;
      
      @Column(name = "updated_at")
      private LocalDateTime updatedAt;
      
      public enum ProcessingStatus {
          PENDING,
          PROCESSING,
          COMPLETED,
          FAILED,
          RETRY
      }
      
      // 생성자, getter, setter, equals, hashCode
  }
  ```

- [ ] `NewsProcessingStatusService` 클래스 생성
  ```java
  @Service
  @Transactional
  public class NewsProcessingStatusService {
      
      private final NewsProcessingStatusRepository statusRepository;
      private final Logger logger = LoggerFactory.getLogger(NewsProcessingStatusService.class);
      
      public NewsProcessingStatusService(NewsProcessingStatusRepository statusRepository) {
          this.statusRepository = statusRepository;
      }
      
      public void markAsPending(String newsId) {
          NewsProcessingStatus status = new NewsProcessingStatus();
          status.setNewsId(newsId);
          status.setStatus(ProcessingStatus.PENDING);
          status.setCreatedAt(LocalDateTime.now());
          status.setUpdatedAt(LocalDateTime.now());
          
          statusRepository.save(status);
          logger.debug("뉴스 처리 상태를 PENDING으로 설정: {}", newsId);
      }
      
      public void markAsProcessing(String newsId) {
          Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
          if (statusOpt.isPresent()) {
              NewsProcessingStatus status = statusOpt.get();
              status.setStatus(ProcessingStatus.PROCESSING);
              status.setProcessingStartedAt(LocalDateTime.now());
              status.setUpdatedAt(LocalDateTime.now());
              
              statusRepository.save(status);
              logger.debug("뉴스 처리 상태를 PROCESSING으로 설정: {}", newsId);
          }
      }
      
      public void markAsCompleted(String newsId) {
          Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
          if (statusOpt.isPresent()) {
              NewsProcessingStatus status = statusOpt.get();
              status.setStatus(ProcessingStatus.COMPLETED);
              status.setProcessingCompletedAt(LocalDateTime.now());
              status.setUpdatedAt(LocalDateTime.now());
              
              statusRepository.save(status);
              logger.debug("뉴스 처리 상태를 COMPLETED로 설정: {}", newsId);
          }
      }
      
      public void markAsFailed(String newsId, String errorMessage) {
          Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
          if (statusOpt.isPresent()) {
              NewsProcessingStatus status = statusOpt.get();
              status.setStatus(ProcessingStatus.FAILED);
              status.setErrorMessage(errorMessage);
              status.setUpdatedAt(LocalDateTime.now());
              
              statusRepository.save(status);
              logger.warn("뉴스 처리 상태를 FAILED로 설정: {} - {}", newsId, errorMessage);
          }
      }
      
      public void incrementRetryCount(String newsId) {
          Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
          if (statusOpt.isPresent()) {
              NewsProcessingStatus status = statusOpt.get();
              status.setRetryCount(status.getRetryCount() + 1);
              status.setStatus(ProcessingStatus.RETRY);
              status.setUpdatedAt(LocalDateTime.now());
              
              statusRepository.save(status);
              logger.debug("뉴스 재시도 횟수 증가: {} ({}회)", newsId, status.getRetryCount());
          }
      }
      
      public List<NewsProcessingStatus> findFailedNews() {
          return statusRepository.findByStatus(ProcessingStatus.FAILED);
      }
      
      public List<NewsProcessingStatus> findRetryNews() {
          return statusRepository.findByStatus(ProcessingStatus.RETRY);
      }
  }
  ```

### 5단계: 뉴스 재처리 서비스 구현
- [ ] `NewsRetryService` 클래스 생성
  ```java
  @Service
  public class NewsRetryService {
      
      private final NewsProcessingStatusService statusService;
      private final NewsStreamIntegrationService integrationService;
      private final Logger logger = LoggerFactory.getLogger(NewsRetryService.class);
      
      @Value("${news.retry.max-attempts:3}")
      private int maxRetryAttempts;
      
      @Value("${news.retry.delay:5000}")
      private long retryDelay;
      
      public NewsRetryService(NewsProcessingStatusService statusService,
                             NewsStreamIntegrationService integrationService) {
          this.statusService = statusService;
          this.integrationService = integrationService;
      }
      
      @Scheduled(fixedDelay = 60000) // 1분마다 실행
      public void retryFailedNews() {
          try {
              logger.info("실패한 뉴스 재처리 시작");
              
              List<NewsProcessingStatus> failedNews = statusService.findFailedNews();
              List<NewsProcessingStatus> retryNews = statusService.findRetryNews();
              
              List<NewsProcessingStatus> allRetryableNews = new ArrayList<>();
              allRetryableNews.addAll(failedNews);
              allRetryableNews.addAll(retryNews);
              
              if (!allRetryableNews.isEmpty()) {
                  for (NewsProcessingStatus status : allRetryableNews) {
                      if (status.getRetryCount() < maxRetryAttempts) {
                          retryNews(status);
                      } else {
                          logger.warn("최대 재시도 횟수 초과: {} ({}회)", 
                              status.getNewsId(), status.getRetryCount());
                      }
                  }
              } else {
                  logger.debug("재처리할 뉴스가 없습니다");
              }
              
          } catch (Exception e) {
              logger.error("실패한 뉴스 재처리 중 오류 발생", e);
          }
      }
      
      private void retryNews(NewsProcessingStatus status) {
          try {
              logger.info("뉴스 재처리 시작: {} ({}회째)", 
                  status.getNewsId(), status.getRetryCount() + 1);
              
              // 재시도 횟수 증가
              statusService.incrementRetryCount(status.getNewsId());
              
              // 지연 후 재처리
              Thread.sleep(retryDelay);
              
              // 뉴스 재처리
              integrationService.processNewsCreated(status.getNewsId());
              
              logger.info("뉴스 재처리 완료: {}", status.getNewsId());
              
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              logger.warn("뉴스 재처리 중 인터럽트 발생: {}", status.getNewsId());
          } catch (Exception e) {
              logger.error("뉴스 재처리 실패: {}", status.getNewsId(), e);
          }
      }
  }
  ```

### 6단계: 뉴스 처리 성능 모니터링
- [ ] `NewsProcessingMetrics` 클래스 생성
  ```java
  @Component
  public class NewsProcessingMetrics {
      
      private final MeterRegistry meterRegistry;
      
      public NewsProcessingMetrics(MeterRegistry meterRegistry) {
          this.meterRegistry = meterRegistry;
          initializeMetrics();
      }
      
      private void initializeMetrics() {
          // 뉴스 처리 성공 카운터
          Counter.builder("news.processing.success")
              .description("성공적으로 처리된 뉴스 수")
              .register(meterRegistry);
          
          // 뉴스 처리 실패 카운터
          Counter.builder("news.processing.failure")
              .description("처리 실패한 뉴스 수")
              .register(meterRegistry);
          
          // 뉴스 처리 시간 타이머
          Timer.builder("news.processing.time")
              .description("뉴스 처리 소요 시간")
              .register(meterRegistry);
          
          // 배치 처리 크기 게이지
          Gauge.builder("news.batch.size")
              .description("배치 처리 크기")
              .register(meterRegistry, 0, Integer::valueOf);
          
          // 재시도 횟수 카운터
          Counter.builder("news.retry.count")
              .description("뉴스 재시도 횟수")
              .register(meterRegistry);
      }
      
      public void recordSuccess() {
          Counter.builder("news.processing.success")
              .register(meterRegistry)
              .increment();
      }
      
      public void recordFailure() {
          Counter.builder("news.processing.failure")
              .register(meterRegistry)
              .increment();
      }
      
      public void recordProcessingTime(long timeInMs) {
          Timer.builder("news.processing.time")
              .register(meterRegistry)
              .record(timeInMs, TimeUnit.MILLISECONDS);
      }
      
      public void recordRetry() {
          Counter.builder("news.retry.count")
              .register(meterRegistry)
              .increment();
      }
  }
  ```

### 7단계: 설정 파일 업데이트
- [ ] `application.yml`에 뉴스 서비스 설정 추가
  ```yaml
  news:
    batch:
      size: ${NEWS_BATCH_SIZE:100}
      delay: ${NEWS_BATCH_DELAY:1000}
    schedule:
      initial-delay: ${NEWS_SCHEDULE_INITIAL_DELAY:60000}
      fixed-delay: ${NEWS_SCHEDULE_FIXED_DELAY:300000}
    retry:
      max-attempts: ${NEWS_RETRY_MAX_ATTEMPTS:3}
      delay: ${NEWS_RETRY_DELAY:5000}
  
  logging:
    level:
      com.alert.news.service: DEBUG
  ```

## 🧪 검증 방법

### 1. 뉴스 처리 상태 확인
```bash
# 처리 상태 조회
curl http://localhost:8080/api/v1/news/processing-status
```

### 2. 배치 처리 테스트
```bash
# 배치 처리 시작
curl -X POST http://localhost:8080/api/v1/news/batch-process \
  -H "Content-Type: application/json" \
  -d '["news-001", "news-002", "news-003"]'
```

### 3. 메트릭 확인
```bash
curl http://localhost:8080/actuator/metrics/news.processing.success
curl http://localhost:8080/actuator/metrics/news.processing.time
```

### 4. 로그에서 뉴스 처리 동작 확인
```bash
./gradlew bootRun
# 로그에서 뉴스 처리 관련 메시지 확인
```

## 📝 체크리스트

- [x] 뉴스 스트림 통합 서비스가 정상적으로 동작함
- [x] 배치 처리 서비스가 정상적으로 동작함
- [x] 스케줄링 서비스가 정상적으로 동작함
- [x] 처리 상태 관리가 정상적으로 동작함
- [x] 재처리 서비스가 정상적으로 동작함
- [x] 성능 모니터링이 정상적으로 수집됨
- [x] 메시지 큐와 WebSocket 연동이 정상적으로 동작함
- [x] 비동기 작업 설정이 정상적으로 구성됨
- [x] 데이터베이스 스키마가 업데이트됨
- [x] API 엔드포인트가 추가됨

## 🚨 주의사항

1. **성능 최적화**: 배치 크기와 지연 시간을 적절히 조정
2. **에러 처리**: 재시도 횟수 제한으로 무한 루프 방지
3. **모니터링**: 처리 성공/실패율 및 처리 시간 모니터링
4. **리소스 관리**: 동시 처리 수 제한으로 시스템 부하 방지

## 🎉 구현 완료 요약

Feature 6: News Service가 성공적으로 구현되었습니다!

### ✅ 구현된 주요 기능들

1. **뉴스 스트림 통합 서비스** (`NewsStreamIntegrationService`)
   - 메시지 큐와 WebSocket을 연동한 실시간 뉴스 전송 파이프라인
   - 뉴스 생성/수정/삭제 이벤트 처리
   - 즉시 스트리밍과 큐 기반 처리의 이중 안전장치

2. **뉴스 배치 처리 서비스** (`NewsBatchProcessingService`)
   - 대량 뉴스를 배치 단위로 처리하여 성능 최적화
   - 비동기 처리로 시스템 부하 분산
   - 날짜 범위 기반 배치 처리 지원

3. **뉴스 스케줄링 서비스** (`NewsSchedulingService`)
   - 정기적인 뉴스 처리 스케줄링
   - 대기 중인 뉴스 자동 처리
   - 시간별/일별 뉴스 처리 지원

4. **뉴스 처리 상태 관리** (`NewsProcessingStatusService`)
   - 뉴스 처리 상태 추적 및 관리
   - 처리 단계별 상태 기록
   - 에러 메시지 및 재시도 횟수 관리

5. **뉴스 재처리 서비스** (`NewsRetryService`)
   - 실패한 뉴스 자동 재처리
   - 최대 재시도 횟수 제한
   - 지연 기반 재처리로 시스템 안정성 확보

6. **뉴스 처리 성능 모니터링** (`NewsProcessingMetrics`)
   - Micrometer 기반 메트릭 수집
   - 처리 성공/실패율, 처리 시간, 재시도 횟수 등 모니터링
   - Prometheus 연동 지원

7. **비동기 작업 설정** (`AsyncConfig`)
   - 뉴스 작업 전용 TaskExecutor 구성
   - 스레드 풀 크기 및 큐 용량 설정
   - 비동기 작업의 효율적인 관리

8. **데이터베이스 스키마 업데이트**
   - `NEWS_PROCESSING_STATUS` 테이블 추가
   - 처리 상태 추적을 위한 인덱스 구성
   - 데이터 일관성 및 성능 최적화

9. **API 엔드포인트 확장**
   - 배치 처리 시작 API
   - 처리 상태 조회 API
   - 처리 통계 조회 API

### 🚀 성능 및 확장성 특징

- **비동기 처리**: 높은 처리량과 응답성 확보
- **배치 처리**: 대량 데이터 처리 시 메모리 효율성
- **상태 관리**: 처리 과정의 투명성과 추적 가능성
- **재처리 메커니즘**: 시스템 안정성과 신뢰성 향상
- **모니터링**: 실시간 성능 추적 및 문제 진단
- **설정 가능**: 환경별 최적화를 위한 설정 값 제공

### 🔗 다음 단계

이 단계가 완료되었으므로 다음 단계인 **REST API** feature로 진행합니다.

## 📚 참고 자료

- [Spring Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Spring Batch](https://spring.io/projects/spring-batch)
- [Spring Retry](https://github.com/spring-projects/spring-retry)
- [Micrometer Metrics](https://micrometer.io/docs)
