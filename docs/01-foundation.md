# Feature 1: Foundation (기본 인프라 및 프로젝트 구조)

## 📋 개요
프로젝트의 기본 인프라와 구조를 설정하는 단계입니다. 이 단계에서는 개발 환경의 토대를 마련합니다.

## 🎯 목표
- Spring Boot 3.4+ 프로젝트 생성
- Gradle 빌드 시스템 설정
- Docker 환경 구성
- 기본 패키지 구조 생성
- 개발 환경 설정

## 📁 작업 순서

### 1단계: Spring Boot 프로젝트 생성
- [x] Spring Initializr를 통한 프로젝트 생성
  - Java 21 선택
  - Spring Boot 3.3+ 선택
  - Gradle 선택
  - 필요한 의존성 추가:
    - Spring Web
    - Spring Data JPA
    - Spring WebSocket
    - Spring Boot Actuator
    - PostgreSQL Driver
    - Spring Boot DevTools (개발용)

### 2단계: Gradle 설정
- [x] `build.gradle` 파일 구성
  - Java 21 설정
  - Spring Boot 버전 설정
  - 의존성 버전 관리
  - JVM 옵션 설정 (Virtual Threads 지원)
  - 테스트 의존성 추가 (JUnit 5, Testcontainers)

### 3단계: 프로젝트 구조 생성
- [x] 기본 패키지 구조 생성
  ```
  src/main/java/com/alert/news/
  ├── config/          # 설정 클래스
  ├── controller/      # REST API 컨트롤러
  ├── websocket/       # WebSocket 핸들러
  ├── service/         # 비즈니스 로직
  ├── repository/      # 데이터 접근 계층
  ├── model/           # 도메인 모델
  ├── dto/             # 데이터 전송 객체
  ├── exception/       # 예외 처리
  ├── queue/           # 메시지 큐 처리
  └── util/            # 유틸리티 클래스
  ```

### 4단계: Docker 환경 구성
- [x] `Dockerfile` 생성
  - Java 21 베이스 이미지 사용
  - 멀티스테이지 빌드 구성
  - 최적화된 이미지 크기
- [x] `docker-compose.yml` 생성
  - PostgreSQL 서비스 설정
  - Redis 서비스 설정 (향후 확장 고려)
  - 네트워크 설정
  - 볼륨 마운트 설정

### 5단계: 애플리케이션 설정
- [x] `application.yml` 생성
  - 기본 서버 설정 (포트, 컨텍스트 패스)
  - 데이터베이스 연결 설정
  - 로깅 설정
  - 프로파일별 설정 분리
- [x] `application-dev.yml` 생성 (개발 환경)
- [x] `application-prod.yml` 생성 (운영 환경)
- [x] `application-test.yml` 생성 (테스트 환경)

### 6단계: 개발 도구 설정
- [x] `.gitignore` 파일 구성
- [x] IDE 설정 파일 생성 (`.idea/`, `.vscode/`)
- [x] 코드 포맷팅 설정 (Spotless 제거, 기본 Gradle 설정)
- [ ] Git hooks 설정 (pre-commit, pre-push)

### 7단계: 기본 클래스 생성
- [x] 메인 애플리케이션 클래스 생성
- [x] WebSocket 설정 클래스 생성
- [x] 헬스체크 컨트롤러 생성
- [x] 기본 테스트 클래스 생성

## 🧪 검증 방법

### 1. 프로젝트 빌드 확인 ✅
```bash
gradle clean build -x test
```
**결과**: BUILD SUCCESSFUL

### 2. Docker 환경 실행 확인 ✅
```bash
docker-compose up -d postgres redis
docker-compose ps
```
**결과**: PostgreSQL과 Redis 컨테이너가 정상적으로 실행됨

### 3. 애플리케이션 실행 확인 ✅
```bash
gradle bootRun
```
**결과**: 애플리케이션이 정상적으로 시작됨

### 4. 기본 엔드포인트 확인 ✅
- `http://localhost:8080/api/health` - 커스텀 헬스체크 ✅
- `http://localhost:8080/actuator/health` - Spring Boot Actuator 헬스체크 ✅

## 📝 체크리스트

- [x] Spring Boot 프로젝트가 정상적으로 생성됨
- [x] Gradle 빌드가 성공적으로 완료됨
- [x] Docker 환경이 정상적으로 실행됨
- [x] PostgreSQL 데이터베이스에 연결 가능
- [x] 기본 패키지 구조가 올바르게 생성됨
- [x] 애플리케이션이 정상적으로 시작됨
- [x] 헬스체크 엔드포인트가 정상 동작함

## 🚨 주의사항

1. **Java 버전**: 반드시 Java 21을 사용하여 Virtual Threads 지원
2. **Spring Boot 버전**: 3.3+ 버전을 사용하여 최신 기능 활용
3. **Docker 이미지**: 프로덕션 환경을 고려한 보안 설정
4. **의존성 관리**: 불필요한 의존성은 제거하여 이미지 크기 최적화
5. **Gradle 호환성**: Gradle 9.0.0과의 호환성을 위해 Spring Boot 3.3+ 사용

## 🔗 다음 단계

이 단계가 완료되었습니다! 다음 단계인 **Domain & Data** feature로 진행할 수 있습니다.

## 📚 참고 자료

- [Spring Boot 3.3 Reference Documentation](https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/)
- [Java 21 Virtual Threads Guide](https://openjdk.org/jeps/444)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)

## 🎉 완료 요약

Foundation 단계가 성공적으로 완료되었습니다:

- ✅ Spring Boot 3.3+ 프로젝트 생성 및 구성
- ✅ Java 21 기반 개발 환경 설정
- ✅ Gradle 빌드 시스템 구성
- ✅ Docker 환경 구성 (PostgreSQL, Redis)
- ✅ 기본 패키지 구조 생성
- ✅ 애플리케이션 설정 파일 구성
- ✅ 기본 클래스 및 컨트롤러 생성
- ✅ 프로젝트 빌드 및 실행 검증
- ✅ 데이터베이스 연결 및 헬스체크 검증

모든 기본 인프라가 정상적으로 작동하며, 다음 단계로 진행할 준비가 완료되었습니다.
