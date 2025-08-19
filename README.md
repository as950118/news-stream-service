# News Stream Service

데이터베이스에 적재된 데이터를 실시간으로 여러 엔드포인트에 전송하는 백엔드 서비스입니다.

## 📋 프로젝트 개요

실시간 뉴스 전송 시스템을 직접 구현해보는 토이프로젝트입니다

WebSocket, 메시지 큐, 실시간 통신 등 백엔드 개발의 핵심 기술들을 학습하고 구현해보며, 실제 서비스처럼 동작하는 시스템을 만들어봅니다. 

**시나리오**: 번역한 뉴스가 DB에 저장되면, 메시지 큐를 통해 뉴스 ID를 받아서 WebSocket으로 실시간 전송하는 시스템입니다.

### 주요 기능
- **실시간 뉴스 전송**: 웹소켓을 통한 번역된 뉴스 실시간 전송
- **큐 기반 메시지 처리**: 내부 큐를 통한 뉴스 ID 처리
- **고객사 인증**: 토큰 기반 고객사 인증 및 연결 제한(1고객사 1연결)
- **예외 처리 및 로깅**: 다양한 예외 상황에 대한 처리 및 로깅

## 🏗️ 기술 스택

### Backend
- **Language**: Java 21+ (Virtual Threads 지원)
  - Virtual Threads로 높은 동시성 처리 및 메모리 효율성 확보
  - LTS 버전으로 안정성과 장기 지원 보장
- **Framework**: Spring Boot 3.2+
  - Java 21 및 Virtual Threads 완벽 지원
  - Spring WebSocket으로 실시간 통신 구현
  - Spring Data JPA로 데이터 접근 계층 표준화
  - **Spring Security**로 JWT 기반 인증 시스템 구현
- **Database**: 
  - **Primary: PostgreSQL**
    - 번역된 뉴스 데이터 저장
    - 트랜잭션 ACID 보장으로 데이터 일관성 확보
    - 인덱싱으로 뉴스 조회 성능 최적화
- **Message Queue**: 
  - **현재**: LinkedBlockingQueue (내부 큐)
    - 임시 메시지 처리 큐 구현
    - 향후 AWS SQS 전환을 고려한 설계
  - **향후**: AWS SQS
    - 확장성 있는 메시지 큐 시스템
    - AWS 인프라와의 완벽한 통합
- **Security**: 
  - **JWT (JSON Web Token)** 기반 인증
  - BCrypt 비밀번호 암호화
  - WebSocket 연결 시 토큰 검증

### Infrastructure
- **Real-time Communication**: WebSocket
  - `/ws/news` 엔드포인트로 클라이언트와 실시간 통신
  - JSON 형태의 뉴스 메시지 전송
  - **JWT 토큰 기반 고객사별 개별 연결 관리**
- **Container**: Docker
  - 일관된 개발/운영 환경 제공
  - 빠른 배포 및 스케일링
- **Orchestration**: Docker Compose
  - 로컬 개발 환경의 간편한 구성
  - PostgreSQL, Redis 등 의존성 서비스 관리
- **API Documentation**: Swagger/OpenAPI 3.0
  - 자동 API 문서 생성 및 실시간 테스트
  - 개발자 경험 향상
- **Testing**: JUnit 5, Testcontainers
  - JUnit 5의 최신 기능 활용
  - Testcontainers로 실제 데이터베이스 환경에서의 통합 테스트

## 시작하기

### Prerequisites
- Java 21+ (Virtual Threads 지원)
- Docker & Docker Compose
- gradle 9+

### 설치 및 실행

1. **저장소 클론**
```bash
git clone https://github.com/your-username/news-stream-service.git
cd news-stream-service
```

2. **Docker 환경 실행**
```bash
docker-compose up -d
```

3. **애플리케이션 실행**
```bash
./gradlew bootRun
```

4. **빌드**
```bash
./gradlew build
```

5. **테스트 실행**
```bash
./gradlew test
```

6. **API 문서 확인**
```
http://localhost:8080/swagger-ui.html
```

## 📁 프로젝트 구조

```
news-stream-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/news/stream/
│   │   │       ├── config/          # 설정 클래스 (Security, WebSocket)
│   │   │       ├── controller/      # REST API 컨트롤러 (인증, 뉴스, 고객사)
│   │   │       ├── websocket/       # WebSocket 핸들러 및 인증 인터셉터
│   │   │       ├── service/         # 비즈니스 로직 (인증, 고객사, 뉴스)
│   │   │       ├── repository/      # 데이터 접근 계층
│   │   │       ├── model/           # 도메인 모델 (뉴스, 고객사)
│   │   │       ├── dto/             # 데이터 전송 객체 (인증 요청/응답)
│   │   │       ├── exception/       # 예외 처리 (인증, 전역)
│   │   │       ├── queue/           # 내부 큐 처리 (LinkedBlockingQueue)
│   │   │       └── util/            # 유틸리티 클래스 (JWT 토큰)
│   │   └── resources/
│   │       ├── application.yml      # 애플리케이션 설정 (JWT, 고객사 설정 포함)
│   │       └── db/                  # 데이터베이스 스크립트
│   └── test/                        # 테스트 코드
├── docker-compose.yml               # Docker 환경 설정
├── Dockerfile                       # 애플리케이션 Docker 이미지
├── build.gradle                     # Gradle 의존성 관리 (Spring Security, JWT 포함)
└── settings.gradle                  # Gradle 프로젝트 설정
```

## 🔌 API 엔드포인트

### WebSocket 엔드포인트
- `/ws/news` - 뉴스 실시간 전송을 위한 WebSocket 연결
- **연결 시**: JWT 토큰을 통해 인증 및 연결 제한
- **메시지 형식**: JSON 형태의 뉴스 데이터

### 큐 관련 API
- `GET /api/v1/queue/status` - 큐 상태 확인
- `GET /api/v1/queue/stats` - 큐 통계 정보
- `POST /api/v1/queue/test-message` - 테스트 메시지 전송
- `POST /api/v1/queue/clear` - 큐 비우기
```json
{
  "id": "a1b2c3",
  "title": "news-stream-service",
  "body": "news-stream-service는...",
  "publishedAt": "2025-06-05T10:00:00"
}
```

### 인증 관련 API
- `POST /api/v1/customers/auth` - 고객사 인증 및 JWT 토큰 발급
- `GET /api/v1/customers/{id}/connections` - 고객사 연결 상태 확인

### 뉴스 관련 API
- `GET /api/v1/news/{id}` - 특정 뉴스 조회
- `GET /api/v1/news` - 뉴스 목록 조회 (페이징)

## 🗄️ 데이터베이스 스키마

### TRANSLATED_NEWS 테이블
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | String | 뉴스 ID (SQS에서 전송받을 ID) |
| title | String | 번역된 뉴스 제목 |
| content | String | 번역된 뉴스 본문 |
| published_at | datetime | 번역된 뉴스 시각 |

### CUSTOMERS 테이블
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | String | 고객사 ID |
| name | String | 고객사명 |
| token | String | 인증 토큰 |
| connection_id | String | 현재 연결된 WebSocket 세션 ID |
| is_active | boolean | 활성 상태 |
| created_at | datetime | 생성 시각 |
| updated_at | datetime | 수정 시각 |

## 🔄 메시지 처리 흐름

1. **뉴스 ID 수신**: 내부 큐(LinkedBlockingQueue)에서 뉴스 ID 수신
2. **메시지 처리**: Producer-Consumer 패턴으로 뉴스 메시지 처리
3. **뉴스 조회**: 데이터베이스에서 해당 ID의 번역된 뉴스 조회
4. **고객사 확인**: 구독 중인 고객사 목록 확인
5. **실시간 전송**: WebSocket을 통해 각 고객사에게 뉴스 전송
6. **예외 처리**: 각 단계별 예외 상황 로깅 및 처리
7. **모니터링**: 큐 상태 및 메트릭 실시간 수집

## 🔐 인증 시스템

### JWT 토큰 기반 인증
- **토큰 발급**: 고객사 인증 시 JWT 토큰 발급
- **토큰 검증**: WebSocket 연결 시 JWT 토큰 검증
- **연결 제한**: 고객사별 1개 연결만 허용

### 보안 설정
- Spring Security를 통한 엔드포인트 보호
- CSRF 비활성화 (WebSocket 사용)
- Stateless 세션 관리
- JWT 시크릿 키 환경 변수 관리

## 🧪 테스트

### 단위 테스트 실행
```bash
./gradlew test
```

### 통합 테스트 실행
```bash
./gradlew integrationTest
```

### 테스트 커버리지 확인
```bash
./gradlew jacocoTestReport
```

## 📊 모니터링

### Health Check
- **Application Health**: `http://localhost:8080/actuator/health`
- **Database Health**: `http://localhost:8080/actuator/health/db`

### Metrics
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`
- **Application Metrics**: `http://localhost:8080/actuator/metrics`
- **WebSocket Metrics**: `http://localhost:8080/actuator/metrics/websocket.sessions`
- **Queue Metrics**: 
  - `http://localhost:8080/actuator/metrics/queue.size`
  - `http://localhost:8080/actuator/metrics/queue.capacity`
  - `http://localhost:8080/actuator/metrics/queue.utilization`
  - `http://localhost:8080/actuator/metrics/queue.messages.processed`

## 🔧 설정

### 환경 변수
```bash
# 데이터베이스 설정
DB_HOST=localhost
DB_PORT=5432
DB_NAME=alert_news
DB_USERNAME=postgres
DB_PASSWORD=password

# JWT 설정
JWT_SECRET=your-256-bit-secret-key-here-make-it-long-enough-for-security
JWT_EXPIRATION_HOURS=24

# WebSocket 설정
WEBSOCKET_ENDPOINT=/ws/news
WEBSOCKET_MAX_CONNECTIONS_PER_CUSTOMER=1

# 큐 설정
QUEUE_CAPACITY=1000
QUEUE_CONSUMER_THREADS=2
QUEUE_POLL_TIMEOUT=1000

# 고객사 인증 설정
CUSTOMER_TOKEN_EXPIRY_HOURS=24
```

### 로그 레벨 설정
```yaml
logging:
  level:
    com.news.stream: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.springframework.messaging: DEBUG
```

## 🚀 배포

### Docker 이미지 빌드
```bash
docker build -t news-stream-service:latest .
```

### Docker 이미지 실행
```bash
docker run -p 8080:8080 news-stream-service:latest
```

### Docker Compose로 전체 환경 실행
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🔮 학습 목표 및 확장 계획

### 🎯 주요 학습 포인트
- **WebSocket 구현**: 실시간 양방향 통신의 원리와 구현 방법
- **메시지 큐 패턴**: Producer-Consumer 패턴과 비동기 처리
- **동시성 처리**: Java 21 Virtual Threads를 활용한 고성능 처리
- **실시간 시스템 설계**: 대용량 트래픽 처리와 확장성 고려
- **보안 시스템**: JWT 기반 인증 및 WebSocket 보안

### 🚀 확장 아이디어
- **실시간 채팅 기능**: 뉴스에 대한 실시간 댓글 및 토론
- **뉴스 추천 시스템**: 사용자별 맞춤 뉴스 추천 알고리즘
- **실시간 통계 대시보드**: 뉴스 조회수, 인기도 실시간 집계
- **모바일 푸시 알림**: WebSocket + FCM 연동으로 모바일 알림
- **AI 감정 분석**: 뉴스 내용의 감정 분석 및 카테고리 자동 분류
- **AWS SQS 연동**: 내부 큐를 AWS SQS로 전환하여 확장성 향상
- **Redis Streams**: 고성능 메시지 스트리밍으로 처리량 증가

### 🛠️ 도전 과제
- **부하 테스트**: JMeter나 Gatling으로 대용량 동시 접속 테스트
- **장애 상황 시뮬레이션**: 네트워크 끊김, DB 장애 등 다양한 장애 상황 대응
- **성능 최적화**: Redis 캐싱, DB 인덱싱, Connection Pooling 등
- **모니터링 시스템**: Prometheus + Grafana로 실시간 시스템 모니터링
- **큐 성능 테스트**: 대용량 메시지 처리 시 큐 성능 및 메모리 사용량 측정
- **메시지 순서 보장**: 동시성 환경에서 메시지 처리 순서 보장 전략

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.

---

**Note**: 이 프로젝트는 실시간 뉴스 전송 시스템을 학습하고 구현해보는 토이프로젝트입니다. WebSocket, 메시지 큐, 실시간 통신, JWT 인증 등 다양한 기술을 직접 구현해보며 백엔드 개발 실력을 키워봅시다! 🚀 

---

## 🔐 JWT 인증 시스템 구현 가이드

현재 프로젝트에서는 WebSocket 연결 시에만 JWT 토큰 검증이 구현되어 있습니다. REST API에서도 JWT 검증을 구현하려면 다음과 같은 추가 작업이 필요합니다.

### 현재 구현 상태
- ✅ **WebSocket JWT 검증**: `WebSocketAuthenticationInterceptor`에서 구현됨
- ❌ **REST API JWT 검증**: 미구현 (모든 API가 `permitAll()`로 설정됨)

### REST API JWT 검증 구현 방법

#### 1. JWT 인증 필터 생성

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationService authenticationService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractTokenFromRequest(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Optional<Customer> customer = authenticationService.validateToken(token);
            if (customer.isPresent()) {
                // Spring Security 컨텍스트에 인증 정보 설정
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        customer.get(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### 2. SecurityConfig 수정

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/customers/auth").permitAll() // 로그인만 허용
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated() // 나머지는 인증 필요
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
}
```

#### 3. 컨트롤러에서 인증 정보 사용

```java
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {
    
    @GetMapping
    public ResponseEntity<PageResponse<NewsDto>> getNews(
            @AuthenticationPrincipal Customer customer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // customer 객체를 통해 인증된 고객사 정보 접근
        log.info("인증된 고객사: {}", customer.getName());
        
        // 뉴스 조회 로직...
    }
}
```

#### 4. 메서드 레벨 보안 설정

```java
@Service
public class NewsService {
    
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<NewsDto> getNewsForCustomer(String customerId) {
        // 고객사별 뉴스 조회 로직
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteNews(String newsId) {
        // 관리자만 뉴스 삭제 가능
    }
}
```

### 구현 시 고려사항

1. **토큰 만료 처리**: JWT 토큰 만료 시 자동 갱신 또는 재로그인 요구
2. **에러 응답**: 인증 실패 시 적절한 HTTP 상태 코드와 에러 메시지 반환
3. **로깅**: 인증 성공/실패 로그 기록으로 보안 모니터링
4. **테스트**: JWT 인증이 적용된 API에 대한 테스트 코드 작성

### 보안 강화 방안

1. **토큰 블랙리스트**: 로그아웃된 토큰 관리
2. **Rate Limiting**: API 호출 횟수 제한
3. **CORS 설정**: 허용된 도메인에서만 API 호출 가능
4. **HTTPS 강제**: 프로덕션 환경에서 HTTPS 사용

이 가이드를 따라 구현하면 REST API에서도 JWT 기반 인증을 완벽하게 적용할 수 있습니다.

## 🚀 AWS SQS 마이그레이션 가이드

### 📋 개요

현재 시스템은 로컬 메시지 큐(LinkedBlockingQueue)를 사용하고 있으며, AWS SQS로 마이그레이션할 수 있도록 설계되어 있습니다. DDD(도메인 주도 설계) 원칙을 잘 따르고 있어 마이그레이션이 상당히 수월합니다.

### 🎯 마이그레이션 전략

#### **현재 아키텍처의 장점**
- ✅ **인터페이스 분리**: `MessageQueue<T>` 인터페이스로 완벽하게 추상화
- ✅ **의존성 역전**: 구체 구현체가 아닌 인터페이스에 의존
- ✅ **단일 책임**: Producer, Consumer, Queue가 명확히 분리
- ✅ **설정 분리**: `QueueConfig`로 큐 관련 설정 중앙화

#### **마이그레이션 단계**
1. **Phase 1**: AWS SQS 구현체 추가 ✅
2. **Phase 2**: 설정 기반 전환 ✅
3. **Phase 3**: 점진적 전환 (개발→스테이징→프로덕션)

### 🔧 구현된 구성요소

#### **1. AwsSqsMessageQueue**
```java
@Component
@Profile("aws-sqs")
public class AwsSqsMessageQueue implements MessageQueue<NewsMessage> {
    // AWS SQS 기반 메시지 큐 구현체
    // JSON 직렬화/역직렬화, 메시지 속성 지원, 에러 처리
}
```

#### **2. AwsSqsConfig**
```java
@Configuration
@Profile("aws-sqs")
public class AwsSqsConfig {
    @Bean
    public AmazonSQS sqsClient() {
        // SQS 클라이언트 설정
        // 로컬 테스트를 위한 endpoint 설정 지원
    }
}
```

#### **3. 프로파일 기반 설정**
- `local`: 기존 로컬 큐 사용
- `aws-sqs`: AWS SQS 사용

### 🚀 사용 방법

#### **로컬 개발 환경 (LocalStack)**

1. **LocalStack 실행**
```bash
docker run -d --name localstack \
  -p 4566:4566 \
  -e SERVICES=sqs \
  localstack/localstack
```

2. **SQS 큐 생성**
```bash
# AWS CLI 설치 (로컬 테스트용)
pip install awscli-local

# 큐 생성
awslocal sqs create-queue --queue-name news-queue

# 큐 URL 확인
awslocal sqs get-queue-url --queue-name news-queue
```

3. **환경 변수 설정**
```bash
export AWS_SQS_ENDPOINT=http://localhost:4566
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_SQS_QUEUE_URL=http://localhost:4566/000000000000/news-queue
```

4. **애플리케이션 실행**
```bash
./gradlew bootRun --args='--spring.profiles.active=aws-sqs'
```

#### **AWS 프로덕션 환경**

1. **AWS 자격 증명 설정**
```bash
export AWS_REGION=ap-northeast-2
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_SQS_QUEUE_URL=https://sqs.ap-northeast-2.amazonaws.com/...
```

2. **애플리케이션 실행**
```bash
./gradlew bootRun --args='--spring.profiles.active=aws-sqs'
```

### 📊 모니터링 및 메트릭

#### **SQS 전용 메트릭**
- `queue.sqs.send.time`: 메시지 전송 시간
- `queue.sqs.receive.time`: 메시지 수신 시간
- `queue.sqs.delete.time`: 메시지 삭제 시간
- `queue.messages.enqueued`: 큐잉된 메시지 수
- `queue.messages.dequeued`: 디큐잉된 메시지 수

#### **CloudWatch 연동**
```yaml
# CloudWatch 메트릭 내보내기
management:
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: news-stream-service
```

### 🔄 마이그레이션 체크리스트

#### **사전 준비**
- [ ] AWS 계정 및 IAM 권한 설정
- [ ] SQS 큐 생성 및 설정
- [ ] 환경 변수 및 설정 파일 준비
- [ ] 로컬 테스트 환경 구축 (LocalStack)

#### **코드 변경**
- [x] AWS SDK SQS 의존성 추가
- [x] `AwsSqsMessageQueue` 구현체 생성
- [x] `AwsSqsConfig` 설정 클래스 생성
- [x] 프로파일 기반 설정 분리

#### **테스트**
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 실행
- [ ] 성능 테스트 수행
- [ ] 장애 복구 테스트

#### **배포**
- [ ] 개발 환경 배포 및 검증
- [ ] 스테이징 환경 배포 및 검증
- [ ] 프로덕션 환경 점진적 전환
- [ ] 모니터링 및 알림 설정

### 🚨 주의사항

#### **1. 메시지 순서**
- SQS Standard Queue는 메시지 순서를 보장하지 않음
- 순서가 중요한 경우 SQS FIFO Queue 사용 고려

#### **2. 메시지 중복**
- SQS는 최소 1회 전달을 보장
- 멱등성(Idempotency) 고려 필요

#### **3. 비용 최적화**
- 메시지 크기 제한 (256KB)
- 배치 처리 활용
- Dead Letter Queue 설정

#### **4. 보안**
- IAM 역할 및 정책 최소 권한 원칙
- VPC 엔드포인트 사용 고려
- 암호화 전송 및 저장

### 📈 성능 최적화

#### **1. 배치 처리**
```java
// 배치로 메시지 전송
SendMessageBatchRequest batchRequest = new SendMessageBatchRequest()
    .withQueueUrl(queueUrl)
    .withEntries(entries);
```

#### **2. Long Polling**
```java
// 긴 폴링으로 대기 시간 단축
ReceiveMessageRequest request = new ReceiveMessageRequest()
    .withWaitTimeSeconds(20); // 최대 20초 대기
```

#### **3. 가시성 타임아웃**
```java
// 메시지 처리 시간에 맞춘 가시성 타임아웃
ChangeMessageVisibilityRequest request = new ChangeMessageVisibilityRequest()
    .withVisibilityTimeout(60); // 60초
```

### 🔍 문제 해결

#### **일반적인 오류**

| 오류 코드 | 원인 | 해결 방법 |
|-----------|------|-----------|
| `AccessDenied` | IAM 권한 부족 | IAM 정책 확인 및 수정 |
| `InvalidParameterValue` | 잘못된 파라미터 | 요청 파라미터 검증 |
| `QueueDoesNotExist` | 큐 URL 오류 | 큐 URL 확인 |
| `ThrottlingException` | 요청 제한 초과 | 지수 백오프 적용 |

#### **디버깅 팁**
```bash
# AWS CLI로 큐 상태 확인
aws sqs get-queue-attributes \
  --queue-url $QUEUE_URL \
  --attribute-names All

# CloudWatch 로그 확인
aws logs filter-log-events \
  --log-group-name /aws/lambda/your-function \
  --start-time $(date -d '1 hour ago' +%s)000
```

### 📚 추가 리소스

- [AWS SQS 개발자 가이드](https://docs.aws.amazon.com/sqs/)
- [Spring Cloud AWS 문서](https://spring.io/projects/spring-cloud-aws)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [LocalStack 문서](https://docs.localstack.cloud/)

### 🎉 결론

DDD 기반으로 잘 설계된 현재 시스템은 AWS SQS 마이그레이션에 매우 적합합니다. 인터페이스 분리와 의존성 역전 원칙 덕분에:

1. **최소한의 코드 변경**으로 마이그레이션 가능
2. **프로파일 기반**으로 점진적 전환
3. **기존 비즈니스 로직**에 영향 없음
4. **확장성과 안정성** 향상

체계적인 접근과 충분한 테스트를 통해 안전하고 효율적인 마이그레이션을 진행할 수 있습니다.

---
