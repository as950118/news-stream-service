# Feature 5: WebSocket (실시간 통신)

## 📋 개요
WebSocket을 통한 실시간 뉴스 전송 시스템을 구현하고 고객사별 연결을 관리하는 단계입니다.

## 🎯 목표
- WebSocket 핸들러 구현
- 실시간 뉴스 전송 로직 구현
- 고객사별 연결 관리
- WebSocket 세션 모니터링
- 실시간 통신 성능 최적화

## 📁 작업 순서

### 1단계: WebSocket 설정 구성
- [ ] `WebSocketConfig` 클래스 생성
  ```java
  @Configuration
  @EnableWebSocketMessageBroker
  public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
      
      @Override
      public void configureMessageBroker(MessageBrokerRegistry config) {
          config.enableSimpleBroker("/topic");
          config.setApplicationDestinationPrefixes("/app");
      }
      
      @Override
      public void registerStompEndpoints(StompEndpointRegistry registry) {
          registry.addEndpoint("/ws/news")
              .setAllowedOriginPatterns("*")
              .withSockJS();
      }
      
      @Override
      public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
          registration.setMessageSizeLimit(64 * 1024) // 64KB
              .setSendBufferSizeLimit(512 * 1024)    // 512KB
              .setSendTimeLimit(20000);              // 20초
      }
  }
  ```

### 2단계: WebSocket 이벤트 핸들러 구현
- [ ] `WebSocketEventHandler` 클래스 생성
  ```java
  @Component
  public class WebSocketEventHandler {
      
      private final Logger logger = LoggerFactory.getLogger(WebSocketEventHandler.class);
      private final CustomerService customerService;
      
      public WebSocketEventHandler(CustomerService customerService) {
          this.customerService = customerService;
      }
      
      @EventListener
      public void handleWebSocketConnectListener(SessionConnectedEvent event) {
          StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
          String sessionId = sha.getSessionId();
          logger.info("WebSocket 연결됨: {}", sessionId);
      }
      
      @EventListener
      public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
          StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
          String sessionId = sha.getSessionId();
          logger.info("WebSocket 연결 해제됨: {}", sessionId);
          
          // 연결 해제 시 고객사 연결 정보 정리
          customerService.clearConnection(sessionId);
      }
      
      @EventListener
      public void handleWebSocketErrorListener(SessionSubscribeEvent event) {
          StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
          String sessionId = sha.getSessionId();
          logger.warn("WebSocket 구독 오류: {}", sessionId);
      }
  }
  ```

### 3단계: 뉴스 스트림 서비스 구현
- [ ] `NewsStreamService` 클래스 생성
  ```java
  @Service
  public class NewsStreamService {
      
      private final SimpMessagingTemplate messagingTemplate;
      private final TranslatedNewsService newsService;
      private final CustomerService customerService;
      private final Logger logger = LoggerFactory.getLogger(NewsStreamService.class);
      
      public NewsStreamService(SimpMessagingTemplate messagingTemplate,
                              TranslatedNewsService newsService,
                              CustomerService customerService) {
          this.messagingTemplate = messagingTemplate;
          this.newsService = newsService;
          this.customerService = customerService;
      }
      
      public void broadcastNews(String newsId) {
          try {
              Optional<TranslatedNews> newsOpt = newsService.findById(newsId);
              if (newsOpt.isEmpty()) {
                  logger.warn("뉴스를 찾을 수 없습니다: {}", newsId);
                  return;
              }
              
              TranslatedNews news = newsOpt.get();
              NewsDto newsDto = convertToDto(news);
              
              // 모든 활성 고객사에게 브로드캐스트
              List<Customer> activeCustomers = customerService.findActiveCustomers();
              for (Customer customer : activeCustomers) {
                  if (customer.getConnectionId() != null) {
                      sendToCustomer(customer.getConnectionId(), newsDto);
                  }
              }
              
              logger.info("뉴스가 {}명의 고객사에게 전송되었습니다: {}", 
                  activeCustomers.size(), newsId);
                  
          } catch (Exception e) {
              logger.error("뉴스 브로드캐스트 중 오류 발생: {}", newsId, e);
          }
      }
      
      public void broadcastNewsUpdate(String newsId) {
          try {
              Optional<TranslatedNews> newsOpt = newsService.findById(newsId);
              if (newsOpt.isEmpty()) {
                  logger.warn("업데이트할 뉴스를 찾을 수 없습니다: {}", newsId);
                  return;
              }
              
              TranslatedNews news = newsOpt.get();
              NewsUpdateDto updateDto = new NewsUpdateDto(
                  news.getId(),
                  news.getTitle(),
                  news.getContent(),
                  news.getPublishedAt(),
                  "UPDATED"
              );
              
              // 모든 활성 고객사에게 업데이트 알림
              List<Customer> activeCustomers = customerService.findActiveCustomers();
              for (Customer customer : activeCustomers) {
                  if (customer.getConnectionId() != null) {
                      sendToCustomer(customer.getConnectionId(), updateDto);
                  }
              }
              
              logger.info("뉴스 업데이트가 {}명의 고객사에게 전송되었습니다: {}", 
                  activeCustomers.size(), newsId);
                  
          } catch (Exception e) {
              logger.error("뉴스 업데이트 브로드캐스트 중 오류 발생: {}", newsId, e);
          }
      }
      
      public void broadcastNewsDeletion(String newsId) {
          try {
              NewsDeletionDto deletionDto = new NewsDeletionDto(
                  newsId,
                  "DELETED",
                  LocalDateTime.now()
              );
              
              // 모든 활성 고객사에게 삭제 알림
              List<Customer> activeCustomers = customerService.findActiveCustomers();
              for (Customer customer : activeCustomers) {
                  if (customer.getConnectionId() != null) {
                      sendToCustomer(customer.getConnectionId(), deletionDto);
                  }
              }
              
              logger.info("뉴스 삭제 알림이 {}명의 고객사에게 전송되었습니다: {}", 
                  activeCustomers.size(), newsId);
                  
          } catch (Exception e) {
              logger.error("뉴스 삭제 알림 브로드캐스트 중 오류 발생: {}", newsId, e);
          }
      }
      
      private void sendToCustomer(String connectionId, Object message) {
          try {
              messagingTemplate.convertAndSendToUser(
                  connectionId,
                  "/topic/news",
                  message
              );
          } catch (Exception e) {
              logger.error("고객사 {}에게 메시지 전송 실패", connectionId, e);
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
  ```

### 4단계: WebSocket 메시지 DTO 생성
- [ ] `NewsUpdateDto` 클래스 생성
  ```java
  public record NewsUpdateDto(
      String id,
      String title,
      String content,
      LocalDateTime publishedAt,
      String action
  ) {}
  ```

- [ ] `NewsDeletionDto` 클래스 생성
  ```java
  public record NewsDeletionDto(
      String id,
      String action,
      LocalDateTime deletedAt
  ) {}
  ```

- [ ] `WebSocketMessage` 클래스 생성
  ```java
  public record WebSocketMessage<T>(
      String type,
      T payload,
      LocalDateTime timestamp
  ) {}
  ```

### 5단계: WebSocket 컨트롤러 구현
- [ ] `WebSocketController` 클래스 생성
  ```java
  @Controller
  public class WebSocketController {
      
      private final NewsStreamService newsStreamService;
      private final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
      
      public WebSocketController(NewsStreamService newsStreamService) {
          this.newsStreamService = newsStreamService;
      }
      
      @MessageMapping("/news/request")
      @SendTo("/topic/news")
      public WebSocketMessage<NewsDto> handleNewsRequest(@Payload NewsRequest request) {
          logger.debug("뉴스 요청 수신: {}", request);
          
          // 특정 뉴스 요청 처리
          // 실제 구현에서는 뉴스 ID로 조회하여 반환
          return new WebSocketMessage<>(
              "NEWS_RESPONSE",
              null, // 실제 뉴스 데이터
              LocalDateTime.now()
          );
      }
      
      @MessageMapping("/news/heartbeat")
      @SendTo("/topic/heartbeat")
      public WebSocketMessage<String> handleHeartbeat(@Payload HeartbeatRequest request) {
          logger.debug("하트비트 수신: {}", request);
          
          return new WebSocketMessage<>(
              "HEARTBEAT_RESPONSE",
              "OK",
              LocalDateTime.now()
          );
      }
  }
  ```

### 6단계: WebSocket 연결 관리 서비스
- [ ] `WebSocketConnectionManager` 클래스 생성
  ```java
  @Service
  public class WebSocketConnectionManager {
      
      private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
      private final Map<String, String> customerConnections = new ConcurrentHashMap<>();
      private final Logger logger = LoggerFactory.getLogger(WebSocketConnectionManager.class);
      
      public void addSession(String sessionId, WebSocketSession session) {
          sessions.put(sessionId, session);
          logger.info("WebSocket 세션 추가됨: {}", sessionId);
      }
      
      public void removeSession(String sessionId) {
          sessions.remove(sessionId);
          customerConnections.remove(sessionId);
          logger.info("WebSocket 세션 제거됨: {}", sessionId);
      }
      
      public void associateCustomer(String sessionId, String customerId) {
          customerConnections.put(sessionId, customerId);
          logger.info("고객사 {}가 세션 {}에 연결됨", customerId, sessionId);
      }
      
      public String getCustomerId(String sessionId) {
          return customerConnections.get(sessionId);
      }
      
      public WebSocketSession getSession(String sessionId) {
          return sessions.get(sessionId);
      }
      
      public int getActiveSessionCount() {
          return sessions.size();
      }
      
      public int getActiveCustomerCount() {
          return customerConnections.size();
      }
      
      public List<String> getActiveSessionIds() {
          return new ArrayList<>(sessions.keySet());
      }
      
      public List<String> getActiveCustomerIds() {
          return new ArrayList<>(customerConnections.values());
      }
  }
  ```

### 7단계: WebSocket 상태 모니터링
- [ ] `WebSocketMetrics` 클래스 생성
  ```java
  @Component
  public class WebSocketMetrics {
      
      private final WebSocketConnectionManager connectionManager;
      private final MeterRegistry meterRegistry;
      
      public WebSocketMetrics(WebSocketConnectionManager connectionManager,
                            MeterRegistry meterRegistry) {
          this.connectionManager = connectionManager;
          this.meterRegistry = meterRegistry;
          initializeMetrics();
      }
      
      private void initializeMetrics() {
          // 활성 세션 수 게이지
          Gauge.builder("websocket.sessions.active")
              .description("현재 활성 WebSocket 세션 수")
              .register(meterRegistry, connectionManager, WebSocketConnectionManager::getActiveSessionCount);
          
          // 활성 고객사 수 게이지
          Gauge.builder("websocket.customers.active")
              .description("현재 활성 고객사 수")
              .register(meterRegistry, connectionManager, WebSocketConnectionManager::getActiveCustomerCount);
          
          // 메시지 전송 카운터
          Counter.builder("websocket.messages.sent")
              .description("전송된 WebSocket 메시지 수")
              .register(meterRegistry);
          
          // 메시지 수신 카운터
          Counter.builder("websocket.messages.received")
              .description("수신된 WebSocket 메시지 수")
              .register(meterRegistry);
      }
      
      public void recordMessageSent() {
          Counter.builder("websocket.messages.sent")
              .register(meterRegistry)
              .increment();
      }
      
      public void recordMessageReceived() {
          Counter.builder("websocket.messages.received")
              .register(meterRegistry)
              .increment();
      }
  }
  ```

### 8단계: WebSocket 상태 확인 API
- [ ] `WebSocketStatusController` 클래스 생성
  ```java
  @RestController
  @RequestMapping("/api/v1/websocket")
  public class WebSocketStatusController {
      
      private final WebSocketConnectionManager connectionManager;
      private final WebSocketMetrics webSocketMetrics;
      
      public WebSocketStatusController(WebSocketConnectionManager connectionManager,
                                    WebSocketMetrics webSocketMetrics) {
          this.connectionManager = connectionManager;
          this.webSocketMetrics = webSocketMetrics;
      }
      
      @GetMapping("/status")
      public ResponseEntity<WebSocketStatusResponse> getWebSocketStatus() {
          WebSocketStatusResponse response = new WebSocketStatusResponse(
              connectionManager.getActiveSessionCount(),
              connectionManager.getActiveCustomerCount(),
              connectionManager.getActiveSessionIds(),
              connectionManager.getActiveCustomerIds()
          );
          
          return ResponseEntity.ok(response);
      }
      
      @GetMapping("/sessions")
      public ResponseEntity<List<String>> getActiveSessions() {
          return ResponseEntity.ok(connectionManager.getActiveSessionIds());
      }
      
      @GetMapping("/customers")
      public ResponseEntity<List<String>> getActiveCustomers() {
          return ResponseEntity.ok(connectionManager.getActiveCustomerIds());
      }
  }
  ```

### 9단계: 설정 파일 업데이트
- [ ] `application.yml`에 WebSocket 설정 추가
  ```yaml
  websocket:
    endpoint: /ws/news
    max-connections-per-customer: ${WEBSOCKET_MAX_CONNECTIONS_PER_CUSTOMER:1}
    message-size-limit: ${WEBSOCKET_MESSAGE_SIZE_LIMIT:65536}
    send-buffer-size: ${WEBSOCKET_SEND_BUFFER_SIZE:524288}
    send-time-limit: ${WEBSOCKET_SEND_TIME_LIMIT:20000}
  
  logging:
    level:
      com.alert.news.websocket: DEBUG
      org.springframework.web.socket: DEBUG
  ```

## 🧪 검증 방법

### 1. WebSocket 연결 테스트
```javascript
// 브라우저 콘솔에서 테스트
const socket = new WebSocket('ws://localhost:8080/ws/news');

socket.onopen = function() {
    console.log('WebSocket 연결됨');
};

socket.onmessage = function(event) {
    console.log('메시지 수신:', event.data);
};

socket.onclose = function() {
    console.log('WebSocket 연결 해제됨');
};
```

### 2. WebSocket 상태 확인
```bash
curl http://localhost:8080/api/v1/websocket/status
```

### 3. 메트릭 확인
```bash
curl http://localhost:8080/actuator/metrics/websocket.sessions.active
curl http://localhost:8080/actuator/metrics/websocket.messages.sent
```

### 4. 로그에서 WebSocket 동작 확인
```bash
./gradlew bootRun
# 로그에서 WebSocket 관련 메시지 확인
```

## 📝 체크리스트

- [x] WebSocket 설정이 올바르게 구성됨
- [x] WebSocket 이벤트 핸들러가 정상적으로 동작함
- [x] 뉴스 스트림 서비스가 정상적으로 동작함
- [x] WebSocket 연결 관리가 정상적으로 동작함
- [x] 실시간 뉴스 전송이 정상적으로 동작함
- [x] WebSocket 모니터링 및 메트릭이 정상적으로 수집됨
- [x] WebSocket 상태 확인 API가 정상적으로 동작함

## ✅ 구현 완료

WebSocket 기능이 성공적으로 구현되었습니다. 주요 구현 내용은 다음과 같습니다:

### 구현된 클래스들
1. **WebSocketConfig** - WebSocket 설정 및 STOMP 메시지 브로커 구성
2. **WebSocketEventHandler** - WebSocket 이벤트 처리 (연결, 해제, 구독)
3. **NewsStreamService** - 실시간 뉴스 브로드캐스트 서비스
4. **WebSocketConnectionManager** - WebSocket 연결 및 세션 관리
5. **WebSocketMetrics** - WebSocket 메트릭 수집 및 모니터링
6. **WebSocketAuthenticationInterceptor** - JWT 토큰 기반 인증
7. **WebSocketStatusController** - WebSocket 상태 확인 API
8. **WebSocketController** - WebSocket 메시지 처리 컨트롤러

### 주요 기능
- **실시간 뉴스 전송**: WebSocket을 통한 번역된 뉴스 실시간 전송
- **고객사별 연결 관리**: JWT 토큰 기반 인증 및 연결 제한
- **이벤트 처리**: 연결, 해제, 구독 이벤트 자동 처리
- **메트릭 수집**: 활성 세션 수, 메시지 전송/수신 카운터 등
- **상태 모니터링**: WebSocket 연결 상태 실시간 확인
- **메시지 처리**: 뉴스 요청, 하트비트, 구독 요청 처리

### WebSocket 엔드포인트
- `/ws` - WebSocket 연결 엔드포인트
- `/topic/news` - 뉴스 브로드캐스트 토픽
- `/topic/heartbeat` - 하트비트 응답 토픽
- `/topic/subscription` - 구독 확인 토픽

### API 엔드포인트
- `GET /api/v1/websocket/status` - WebSocket 전체 상태
- `GET /api/v1/websocket/sessions` - 활성 세션 목록
- `GET /api/v1/websocket/customers` - 활성 고객사 목록
- `GET /api/v1/websocket/stats` - WebSocket 통계 정보

## 🚨 주의사항

1. **연결 관리**: 고객사별 연결 수 제한 및 세션 정리
2. **메모리 관리**: WebSocket 세션 수 제한으로 메모리 오버플로우 방지
3. **에러 처리**: 연결 실패 및 메시지 전송 실패 시 적절한 로깅
4. **성능 최적화**: 메시지 크기 제한 및 전송 버퍼 크기 조정

## 🔗 다음 단계

이 단계가 완료되었으므로 다음 단계인 **News Service** feature로 진행합니다.

## 📚 참고 자료

- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [STOMP Protocol](https://stomp.github.io/stomp-specification-1.2.html)
- [Spring Messaging](https://docs.spring.io/spring-framework/reference/integration/messaging.html)
