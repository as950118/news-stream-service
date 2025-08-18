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

### Infrastructure
- **Real-time Communication**: WebSocket
  - `/ws/news` 엔드포인트로 클라이언트와 실시간 통신
  - JSON 형태의 뉴스 메시지 전송
  - 고객사별 개별 연결 관리
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
│   │   │       ├── config/          # 설정 클래스
│   │   │       ├── controller/      # REST API 컨트롤러
│   │   │       ├── websocket/       # WebSocket 핸들러 및 메시지 처리
│   │   │       ├── service/         # 비즈니스 로직
│   │   │       ├── repository/      # 데이터 접근 계층
│   │   │       ├── model/           # 도메인 모델 (뉴스, 고객사)
│   │   │       ├── dto/             # 데이터 전송 객체
│   │   │       ├── exception/       # 예외 처리
│   │   │       ├── queue/           # 내부 큐 처리 (LinkedBlockingQueue)
│   │   │       └── util/            # 유틸리티 클래스
│   │   └── resources/
│   │       ├── application.yml      # 애플리케이션 설정
│   │       └── db/                  # 데이터베이스 스크립트
│   └── test/                        # 테스트 코드
├── docker-compose.yml               # Docker 환경 설정
├── Dockerfile                       # 애플리케이션 Docker 이미지
├── build.gradle                     # Gradle 의존성 관리
└── settings.gradle                  # Gradle 프로젝트 설정
```

## 🔌 API 엔드포인트

### WebSocket 엔드포인트
- `/ws/news` - 뉴스 실시간 전송을 위한 WebSocket 연결
- **연결 시**: 고객사 토큰을 통해 인증 및 연결 제한
- **메시지 형식**: JSON 형태의 뉴스 데이터
```json
{
  "id": "a1b2c3",
  "title": "news-stream-service",
  "body": "news-stream-service는...",
  "publishedAt": "2025-06-05T10:00:00"
}
```

### 뉴스 관련 API
- `GET /api/v1/news/{id}` - 특정 뉴스 조회
- `GET /api/v1/news` - 뉴스 목록 조회 (페이징)

### 고객사 관련 API
- `POST /api/v1/customers/auth` - 고객사 인증 및 토큰 발급
- `GET /api/v1/customers/{id}/connections` - 고객사 연결 상태 확인

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

## 🔄 메시지 처리 흐름

1. **뉴스 ID 수신**: 내부 큐(LinkedBlockingQueue)에서 뉴스 ID 수신
2. **뉴스 조회**: 데이터베이스에서 해당 ID의 번역된 뉴스 조회
3. **고객사 확인**: 구독 중인 고객사 목록 확인
4. **실시간 전송**: WebSocket을 통해 각 고객사에게 뉴스 전송
5. **예외 처리**: 각 단계별 예외 상황 로깅 및 처리

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
- **Queue Metrics**: `http://localhost:8080/actuator/metrics/queue.size`

## 🔧 설정

### 환경 변수
```bash
# 데이터베이스 설정
DB_HOST=localhost
DB_PORT=5432
DB_NAME=alert_news
DB_USERNAME=postgres
DB_PASSWORD=password

# WebSocket 설정
WEBSOCKET_ENDPOINT=/ws/news
WEBSOCKET_MAX_CONNECTIONS_PER_CUSTOMER=1

# 큐 설정
QUEUE_CAPACITY=1000
QUEUE_POLL_TIMEOUT=1000

# 고객사 인증 설정
CUSTOMER_TOKEN_EXPIRY_HOURS=24
```

### 로그 레벨 설정
```yaml
logging:
  level:
    com.alert.news: DEBUG
    org.springframework.web.socket: DEBUG
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

### 🚀 확장 아이디어
- **실시간 채팅 기능**: 뉴스에 대한 실시간 댓글 및 토론
- **뉴스 추천 시스템**: 사용자별 맞춤 뉴스 추천 알고리즘
- **실시간 통계 대시보드**: 뉴스 조회수, 인기도 실시간 집계
- **모바일 푸시 알림**: WebSocket + FCM 연동으로 모바일 알림
- **AI 감정 분석**: 뉴스 내용의 감정 분석 및 카테고리 자동 분류

### 🛠️ 도전 과제
- **부하 테스트**: JMeter나 Gatling으로 대용량 동시 접속 테스트
- **장애 상황 시뮬레이션**: 네트워크 끊김, DB 장애 등 다양한 장애 상황 대응
- **성능 최적화**: Redis 캐싱, DB 인덱싱, Connection Pooling 등
- **모니터링 시스템**: Prometheus + Grafana로 실시간 시스템 모니터링

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

**Note**: 이 프로젝트는 실시간 뉴스 전송 시스템을 학습하고 구현해보는 토이프로젝트입니다. WebSocket, 메시지 큐, 실시간 통신 등 다양한 기술을 직접 구현해보며 백엔드 개발 실력을 키워봅시다! 🚀 
