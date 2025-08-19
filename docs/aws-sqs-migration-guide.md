# AWS SQS 마이그레이션 가이드

## 📋 개요

이 문서는 현재 로컬 메시지 큐 시스템을 AWS SQS로 마이그레이션하는 방법을 설명합니다.

## 🎯 마이그레이션 전략

### 1. **현재 아키텍처 분석**

현재 시스템은 DDD(도메인 주도 설계) 원칙을 잘 따르고 있어 마이그레이션이 수월합니다:

- ✅ **인터페이스 분리**: `MessageQueue<T>` 인터페이스로 추상화
- ✅ **의존성 역전**: 구체 구현체가 아닌 인터페이스에 의존
- ✅ **단일 책임**: Producer, Consumer, Queue가 명확히 분리
- ✅ **설정 분리**: `QueueConfig`로 큐 관련 설정 중앙화

### 2. **마이그레이션 단계**

#### **Phase 1: AWS SQS 구현체 추가** ✅
- `AwsSqsMessageQueue` 클래스 생성
- `MessageQueue<NewsMessage>` 인터페이스 구현
- AWS SDK SQS 의존성 추가

#### **Phase 2: 설정 기반 전환** ✅
- 프로파일 기반 설정 (`local`, `aws-sqs`)
- 환경별 설정 파일 분리
- AWS 자격 증명 및 SQS 설정

#### **Phase 3: 점진적 전환**
- 개발/테스트 환경: AWS SQS
- 프로덕션: 기존 로컬 큐 → AWS SQS

## 🚀 구현 세부사항

### 1. **AWS SQS 구현체**

```java
@Component
@Profile("aws-sqs")
public class AwsSqsMessageQueue implements MessageQueue<NewsMessage> {
    // AWS SQS 구현
}
```

**주요 특징:**
- JSON 직렬화/역직렬화
- 메시지 속성 지원
- 에러 처리 및 재시도
- 메트릭 수집

### 2. **설정 파일**

```yaml
# application-aws-sqs.yml
queue:
  type: aws-sqs
  aws:
    sqs:
      endpoint: ${AWS_SQS_ENDPOINT:}
      region: ${AWS_REGION:ap-northeast-2}
      queue-url: ${AWS_SQS_QUEUE_URL:}
```

### 3. **프로파일 기반 전환**

```java
@Configuration
@Profile("aws-sqs")
public class AwsSqsConfig {
    @Bean
    public SqsClient sqsClient() {
        // SQS 클라이언트 설정
    }
}
```

## 🔧 환경 설정

### 1. **로컬 개발 환경**

```bash
# LocalStack 사용 (로컬 SQS 에뮬레이션)
docker run -d --name localstack \
  -p 4566:4566 \
  -e SERVICES=sqs \
  localstack/localstack

# 환경 변수 설정
export AWS_SQS_ENDPOINT=http://localhost:4566
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
```

### 2. **AWS 프로덕션 환경**

```bash
# AWS 자격 증명 설정
export AWS_REGION=ap-northeast-2
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_SQS_QUEUE_URL=https://sqs.ap-northeast-2.amazonaws.com/...
```

## 📊 모니터링 및 메트릭

### 1. **SQS 전용 메트릭**

```java
public void recordSqsMetrics(String operation, long timeInMillis, boolean success) {
    // SQS 작업별 성능 메트릭 수집
}
```

**수집되는 메트릭:**
- `queue.sqs.send.time`: 메시지 전송 시간
- `queue.sqs.receive.time`: 메시지 수신 시간
- `queue.sqs.delete.time`: 메시지 삭제 시간
- `queue.messages.enqueued`: 큐잉된 메시지 수
- `queue.messages.dequeued`: 디큐잉된 메시지 수

### 2. **CloudWatch 연동**

```yaml
# CloudWatch 메트릭 내보내기
management:
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: news-stream-service
```

## 🔄 마이그레이션 체크리스트

### **사전 준비**
- [ ] AWS 계정 및 IAM 권한 설정
- [ ] SQS 큐 생성 및 설정
- [ ] 환경 변수 및 설정 파일 준비
- [ ] 로컬 테스트 환경 구축 (LocalStack)

### **코드 변경**
- [ ] AWS SDK SQS 의존성 추가
- [ ] `AwsSqsMessageQueue` 구현체 생성
- [ ] `AwsSqsConfig` 설정 클래스 생성
- [ ] 프로파일 기반 설정 분리

### **테스트**
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 실행
- [ ] 성능 테스트 수행
- [ ] 장애 복구 테스트

### **배포**
- [ ] 개발 환경 배포 및 검증
- [ ] 스테이징 환경 배포 및 검증
- [ ] 프로덕션 환경 점진적 전환
- [ ] 모니터링 및 알림 설정

## 🚨 주의사항

### 1. **메시지 순서**
- SQS Standard Queue는 메시지 순서를 보장하지 않음
- 순서가 중요한 경우 SQS FIFO Queue 사용 고려

### 2. **메시지 중복**
- SQS는 최소 1회 전달을 보장
- 멱등성(Idempotency) 고려 필요

### 3. **비용 최적화**
- 메시지 크기 제한 (256KB)
- 배치 처리 활용
- Dead Letter Queue 설정

### 4. **보안**
- IAM 역할 및 정책 최소 권한 원칙
- VPC 엔드포인트 사용 고려
- 암호화 전송 및 저장

## 📈 성능 최적화

### 1. **배치 처리**

```java
// 배치로 메시지 전송
SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
    .queueUrl(queueUrl)
    .entries(entries)
    .build();
```

### 2. **Long Polling**

```java
// 긴 폴링으로 대기 시간 단축
ReceiveMessageRequest request = ReceiveMessageRequest.builder()
    .waitTimeSeconds(20) // 최대 20초 대기
    .build();
```

### 3. **가시성 타임아웃**

```java
// 메시지 처리 시간에 맞춘 가시성 타임아웃
ChangeMessageVisibilityRequest request = ChangeMessageVisibilityRequest.builder()
    .visibilityTimeout(60) // 60초
    .build();
```

## 🔍 문제 해결

### 1. **일반적인 오류**

| 오류 코드 | 원인 | 해결 방법 |
|-----------|------|-----------|
| `AccessDenied` | IAM 권한 부족 | IAM 정책 확인 및 수정 |
| `InvalidParameterValue` | 잘못된 파라미터 | 요청 파라미터 검증 |
| `QueueDoesNotExist` | 큐 URL 오류 | 큐 URL 확인 |
| `ThrottlingException` | 요청 제한 초과 | 지수 백오프 적용 |

### 2. **디버깅 팁**

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

## 📚 추가 리소스

- [AWS SQS 개발자 가이드](https://docs.aws.amazon.com/sqs/)
- [Spring Cloud AWS 문서](https://spring.io/projects/spring-cloud-aws)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [LocalStack 문서](https://docs.localstack.cloud/)

## 🎉 결론

DDD 기반으로 잘 설계된 현재 시스템은 AWS SQS 마이그레이션에 매우 적합합니다. 인터페이스 분리와 의존성 역전 원칙 덕분에:

1. **최소한의 코드 변경**으로 마이그레이션 가능
2. **프로파일 기반**으로 점진적 전환
3. **기존 비즈니스 로직**에 영향 없음
4. **확장성과 안정성** 향상

체계적인 접근과 충분한 테스트를 통해 안전하고 효율적인 마이그레이션을 진행할 수 있습니다.
