# Feature 4: Message Queue (메시지 큐 시스템)

## 📋 개요
LinkedBlockingQueue 기반의 내부 메시지 큐 시스템을 구현하고 Producer-Consumer 패턴을 통해 뉴스 ID를 처리하는 단계입니다.

## 🎯 목표
- LinkedBlockingQueue 기반 내부 큐 구현
- Producer-Consumer 패턴 구현
- 큐 모니터링 및 메트릭 수집
- 향후 AWS SQS 전환을 고려한 인터페이스 설계
- 큐 처리 성능 최적화

## 📁 작업 순서

### 1단계: 큐 인터페이스 설계
- [ ] `MessageQueue` 인터페이스 생성
  ```java
  public interface MessageQueue<T> {
      void enqueue(T message) throws InterruptedException;
      T dequeue() throws InterruptedException;
      T dequeue(long timeout, TimeUnit unit) throws InterruptedException;
      int size();
      boolean isEmpty();
      void clear();
  }
  ```

- [ ] `NewsMessage` 클래스 생성
  ```java
  public record NewsMessage(
      String newsId,
      LocalDateTime timestamp,
      MessageType type
  ) {
      public enum MessageType {
          NEWS_CREATED,
          NEWS_UPDATED,
          NEWS_DELETED
      }
  }
  ```

### 2단계: LinkedBlockingQueue 구현체 생성
- [ ] `LinkedBlockingMessageQueue` 클래스 생성
  ```java
  @Component
  public class LinkedBlockingMessageQueue implements MessageQueue<NewsMessage> {
      
      private final LinkedBlockingQueue<NewsMessage> queue;
      private final int capacity;
      
      public LinkedBlockingMessageQueue(
          @Value("${queue.capacity:1000}") int capacity) {
          this.capacity = capacity;
          this.queue = new LinkedBlockingQueue<>(capacity);
      }
      
      @Override
      public void enqueue(NewsMessage message) throws InterruptedException {
          if (message == null) {
              throw new IllegalArgumentException("메시지는 null일 수 없습니다");
          }
          queue.put(message);
      }
      
      @Override
      public NewsMessage dequeue() throws InterruptedException {
          return queue.take();
      }
      
      @Override
      public NewsMessage dequeue(long timeout, TimeUnit unit) throws InterruptedException {
          return queue.poll(timeout, unit);
      }
      
      @Override
      public int size() {
          return queue.size();
      }
      
      @Override
      public boolean isEmpty() {
          return queue.isEmpty();
      }
      
      @Override
      public void clear() {
          queue.clear();
      }
      
      public int getCapacity() {
          return capacity;
      }
      
      public int getRemainingCapacity() {
          return queue.remainingCapacity();
      }
  }
  ```

### 3단계: 큐 설정 및 구성
- [ ] `QueueConfig` 클래스 생성
  ```java
  @Configuration
  @EnableAsync
  public class QueueConfig {
      
      @Value("${queue.capacity:1000}")
      private int queueCapacity;
      
      @Value("${queue.consumer-threads:2}")
      private int consumerThreads;
      
      @Value("${queue.poll-timeout:1000}")
      private long pollTimeout;
      
      @Bean
      public MessageQueue<NewsMessage> messageQueue() {
          return new LinkedBlockingMessageQueue(queueCapacity);
      }
      
      @Bean
      public TaskExecutor queueTaskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(consumerThreads);
          executor.setMaxPoolSize(consumerThreads * 2);
          executor.setQueueCapacity(100);
          executor.setThreadNamePrefix("queue-consumer-");
          executor.initialize();
          return executor;
      }
  }
  ```

### 4단계: 메시지 프로듀서 구현
- [ ] `NewsMessageProducer` 클래스 생성
  ```java
  @Component
  public class NewsMessageProducer {
      
      private final MessageQueue<NewsMessage> messageQueue;
      private final Logger logger = LoggerFactory.getLogger(NewsMessageProducer.class);
      
      public NewsMessageProducer(MessageQueue<NewsMessage> messageQueue) {
          this.messageQueue = messageQueue;
      }
      
      public void publishNewsCreated(String newsId) {
          try {
              NewsMessage message = new NewsMessage(
                  newsId, 
                  LocalDateTime.now(), 
                  NewsMessage.MessageType.NEWS_CREATED
              );
              messageQueue.enqueue(message);
              logger.info("뉴스 생성 메시지가 큐에 추가되었습니다: {}", newsId);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              logger.error("뉴스 생성 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
          }
      }
      
      public void publishNewsUpdated(String newsId) {
          try {
              NewsMessage message = new NewsMessage(
                  newsId, 
                  LocalDateTime.now(), 
                  NewsMessage.MessageType.NEWS_UPDATED
              );
              messageQueue.enqueue(message);
              logger.info("뉴스 수정 메시지가 큐에 추가되었습니다: {}", newsId);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              logger.error("뉴스 수정 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
          }
      }
      
      public void publishNewsDeleted(String newsId) {
          try {
              NewsMessage message = new NewsMessage(
                  newsId, 
                  LocalDateTime.now(), 
                  NewsMessage.MessageType.NEWS_DELETED
              );
              messageQueue.enqueue(message);
              logger.info("뉴스 삭제 메시지가 큐에 추가되었습니다: {}", newsId);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              logger.error("뉴스 삭제 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
          }
      }
  }
  ```

### 5단계: 메시지 컨슈머 구현
- [ ] `NewsMessageConsumer` 클래스 생성
  ```java
  @Component
  public class NewsMessageConsumer {
      
      private final MessageQueue<NewsMessage> messageQueue;
      private final NewsStreamService newsStreamService;
      private final Logger logger = LoggerFactory.getLogger(NewsMessageConsumer.class);
      
      @Value("${queue.poll-timeout:1000}")
      private long pollTimeout;
      
      public NewsMessageConsumer(MessageQueue<NewsMessage> messageQueue,
                                NewsStreamService newsStreamService) {
          this.messageQueue = messageQueue;
          this.newsStreamService = newsStreamService;
      }
      
      @Async("queueTaskExecutor")
      public void startConsuming() {
          logger.info("뉴스 메시지 컨슈머가 시작되었습니다");
          
          while (!Thread.currentThread().isInterrupted()) {
              try {
                  NewsMessage message = messageQueue.dequeue(pollTimeout, TimeUnit.MILLISECONDS);
                  if (message != null) {
                      processMessage(message);
                  }
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  logger.info("뉴스 메시지 컨슈머가 인터럽트되었습니다");
                  break;
              } catch (Exception e) {
                  logger.error("메시지 처리 중 오류 발생", e);
              }
          }
      }
      
      private void processMessage(NewsMessage message) {
          try {
              logger.debug("메시지 처리 시작: {}", message);
              
              switch (message.type()) {
                  case NEWS_CREATED:
                      newsStreamService.broadcastNews(message.newsId());
                      break;
                  case NEWS_UPDATED:
                      newsStreamService.broadcastNewsUpdate(message.newsId());
                      break;
                  case NEWS_DELETED:
                      newsStreamService.broadcastNewsDeletion(message.newsId());
                      break;
                  default:
                      logger.warn("알 수 없는 메시지 타입: {}", message.type());
              }
              
              logger.debug("메시지 처리 완료: {}", message.newsId());
          } catch (Exception e) {
              logger.error("메시지 처리 실패: {}", message, e);
          }
      }
      
      @PostConstruct
      public void init() {
          startConsuming();
      }
  }
  ```

### 6단계: 큐 모니터링 및 메트릭
- [ ] `QueueMetrics` 클래스 생성
  ```java
  @Component
  public class QueueMetrics {
      
      private final MessageQueue<NewsMessage> messageQueue;
      private final MeterRegistry meterRegistry;
      
      public QueueMetrics(MessageQueue<NewsMessage> messageQueue, 
                         MeterRegistry meterRegistry) {
          this.messageQueue = messageQueue;
          this.meterRegistry = meterRegistry;
          initializeMetrics();
      }
      
      private void initializeMetrics() {
          // 큐 크기 게이지
          Gauge.builder("queue.size", messageQueue, MessageQueue::size)
              .description("현재 큐에 있는 메시지 수")
              .register(meterRegistry);
          
          // 큐 처리율 카운터
          Counter.builder("queue.messages.processed")
              .description("처리된 메시지 수")
              .register(meterRegistry);
          
          // 큐 처리 시간 타이머
          Timer.builder("queue.processing.time")
              .description("메시지 처리 시간")
              .register(meterRegistry);
      }
      
      public void recordMessageProcessed() {
          Counter.builder("queue.messages.processed")
              .register(meterRegistry)
              .increment();
      }
      
      public void recordProcessingTime(long timeInMs) {
          Timer.builder("queue.processing.time")
              .register(meterRegistry)
              .record(timeInMs, TimeUnit.MILLISECONDS);
      }
  }
  ```

### 7단계: 큐 상태 모니터링 API
- [ ] `QueueController` 클래스 생성
  ```java
  @RestController
  @RequestMapping("/api/v1/queue")
  public class QueueController {
      
      private final MessageQueue<NewsMessage> messageQueue;
      private final QueueMetrics queueMetrics;
      
      public QueueController(MessageQueue<NewsMessage> messageQueue,
                           QueueMetrics queueMetrics) {
          this.messageQueue = messageQueue;
          this.queueMetrics = queueMetrics;
      }
      
      @GetMapping("/status")
      public ResponseEntity<QueueStatusResponse> getQueueStatus() {
          QueueStatusResponse response = new QueueStatusResponse(
              messageQueue.size(),
              messageQueue.isEmpty(),
              messageQueue.getCapacity(),
              messageQueue.getRemainingCapacity()
          );
          
          return ResponseEntity.ok(response);
      }
      
      @PostMapping("/clear")
      public ResponseEntity<Void> clearQueue() {
          messageQueue.clear();
          return ResponseEntity.ok().build();
      }
      
      @PostMapping("/test-message")
      public ResponseEntity<Void> sendTestMessage() {
          NewsMessage testMessage = new NewsMessage(
              "test-" + System.currentTimeMillis(),
              LocalDateTime.now(),
              NewsMessage.MessageType.NEWS_CREATED
          );
          
          try {
              messageQueue.enqueue(testMessage);
              return ResponseEntity.ok().build();
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
          }
      }
  }
  ```

### 8단계: 설정 파일 업데이트
- [ ] `application.yml`에 큐 설정 추가
  ```yaml
  queue:
    capacity: ${QUEUE_CAPACITY:1000}
    consumer-threads: ${QUEUE_CONSUMER_THREADS:2}
    poll-timeout: ${QUEUE_POLL_TIMEOUT:1000}
  
  logging:
    level:
      com.alert.news.queue: DEBUG
  ```

## 🧪 검증 방법

### 1. 큐 상태 확인
```bash
curl http://localhost:8080/api/v1/queue/status
```

### 2. 테스트 메시지 전송
```bash
curl -X POST http://localhost:8080/api/v1/queue/test-message
```

### 3. 메트릭 확인
```bash
curl http://localhost:8080/actuator/metrics/queue.size
curl http://localhost:8080/actuator/metrics/queue.messages.processed
```

### 4. 로그에서 큐 동작 확인
```bash
./gradlew bootRun
# 로그에서 큐 관련 메시지 확인
```

## 📝 체크리스트

- [x] `MessageQueue` 인터페이스가 올바르게 설계됨
- [x] `LinkedBlockingMessageQueue`가 정상적으로 동작함
- [x] `NewsMessageProducer`가 메시지를 정상적으로 큐에 추가함
- [x] `NewsMessageConsumer`가 메시지를 정상적으로 처리함
- [x] 큐 모니터링 및 메트릭이 정상적으로 수집됨
- [x] 큐 상태 확인 API가 정상적으로 동작함
- [x] 큐 처리 성능이 요구사항을 만족함

## ✅ 구현 완료

메시지 큐 시스템이 성공적으로 구현되었습니다:

- **MessageQueue 인터페이스**: 향후 AWS SQS 등 외부 메시지 큐로 전환할 수 있도록 설계
- **LinkedBlockingMessageQueue**: 내부 큐 구현체로 LinkedBlockingQueue 기반
- **NewsMessageProducer**: 뉴스 생성/수정/삭제 메시지를 큐에 발행
- **NewsMessageConsumer**: 큐에서 메시지를 소비하고 처리 (비동기)
- **QueueMetrics**: Micrometer를 사용한 큐 메트릭 수집
- **QueueController**: 큐 상태 모니터링 및 제어 API
- **QueueConfig**: 큐 관련 설정 및 TaskExecutor 구성

## 🚨 주의사항

1. **동시성 처리**: 큐 접근 시 thread-safe 보장
2. **메모리 관리**: 큐 크기 제한으로 메모리 오버플로우 방지
3. **에러 처리**: 메시지 처리 실패 시 적절한 로깅 및 복구
4. **성능 최적화**: 컨슈머 스레드 수 조정으로 처리량 최적화

## 🔗 다음 단계

이 단계가 완료되었습니다! 다음 단계인 **WebSocket** feature로 진행할 수 있습니다.

## 📊 구현된 API 엔드포인트

### 큐 상태 확인
- `GET /api/v1/queue/status` - 큐의 현재 상태 조회
- `GET /api/v1/queue/stats` - 큐의 상세 통계 정보

### 큐 제어
- `POST /api/v1/queue/clear` - 큐의 모든 메시지 제거
- `POST /api/v1/queue/test-message` - 테스트 메시지 전송

### 뉴스 메시지 테스트
- `POST /api/v1/queue/test-message/news-created/{newsId}` - 뉴스 생성 테스트
- `POST /api/v1/queue/test-message/news-updated/{newsId}` - 뉴스 수정 테스트
- `POST /api/v1/queue/test-message/news-deleted/{newsId}` - 뉴스 삭제 테스트

## 📈 메트릭

다음 메트릭들이 자동으로 수집됩니다:
- `queue.size` - 현재 큐에 있는 메시지 수
- `queue.capacity` - 큐의 전체 용량
- `queue.remaining.capacity` - 큐의 남은 용량
- `queue.utilization` - 큐 사용률 (0.0 ~ 1.0)
- `queue.messages.processed` - 처리된 메시지 수
- `queue.processing.time` - 메시지 처리 시간

## 📚 참고 자료

- [Java Concurrency in Practice](https://jcip.net/)
- [LinkedBlockingQueue Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/LinkedBlockingQueue.html)
- [Spring Async Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Micrometer Metrics](https://micrometer.io/docs)
