# Feature 2: Domain & Data (도메인 모델 및 데이터 계층)

## 📋 개요
뉴스 스트림 서비스의 핵심 도메인 모델을 정의하고 데이터 접근 계층을 구현하는 단계입니다.

## 🎯 목표
- `TRANSLATED_NEWS` 엔티티 및 테이블 생성
- `CUSTOMERS` 엔티티 및 테이블 생성
- JPA Repository 구현
- 데이터베이스 마이그레이션 스크립트
- 기본 CRUD 서비스 구현

## 📁 작업 순서

### 1단계: 도메인 모델 설계
- [ ] `TranslatedNews` 엔티티 클래스 생성
  ```java
  @Entity
  @Table(name = "TRANSLATED_NEWS")
  public class TranslatedNews {
      @Id
      private String id;
      
      @Column(nullable = false)
      private String title;
      
      @Column(columnDefinition = "TEXT")
      private String content;
      
      @Column(name = "published_at")
      private LocalDateTime publishedAt;
      
      @Column(name = "created_at")
      private LocalDateTime createdAt;
      
      @Column(name = "updated_at")
      private LocalDateTime updatedAt;
      
      // 생성자, getter, setter, equals, hashCode
  }
  ```

- [ ] `Customer` 엔티티 클래스 생성
  ```java
  @Entity
  @Table(name = "CUSTOMERS")
  public class Customer {
      @Id
      private String id;
      
      @Column(nullable = false)
      private String name;
      
      @Column(unique = true, nullable = false)
      private String token;
      
      @Column(name = "connection_id")
      private String connectionId;
      
      @Column(name = "is_active")
      private boolean isActive = true;
      
      @Column(name = "created_at")
      private LocalDateTime createdAt;
      
      @Column(name = "updated_at")
      private LocalDateTime updatedAt;
      
      // 생성자, getter, setter, equals, hashCode
  }
  ```

### 2단계: JPA Repository 구현
- [ ] `TranslatedNewsRepository` 인터페이스 생성
  ```java
  @Repository
  public interface TranslatedNewsRepository extends JpaRepository<TranslatedNews, String> {
      List<TranslatedNews> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);
      List<TranslatedNews> findByPublishedAtAfter(LocalDateTime publishedAt);
      Page<TranslatedNews> findAllByOrderByPublishedAtDesc(Pageable pageable);
  }
  ```

- [ ] `CustomerRepository` 인터페이스 생성
  ```java
  @Repository
  public interface CustomerRepository extends JpaRepository<Customer, String> {
      Optional<Customer> findByToken(String token);
      Optional<Customer> findByConnectionId(String connectionId);
      List<Customer> findByIsActiveTrue();
      boolean existsByConnectionId(String connectionId);
  }
  ```

### 3단계: 데이터베이스 스키마 생성
- [ ] `schema.sql` 파일 생성
  ```sql
  -- TRANSLATED_NEWS 테이블
  CREATE TABLE IF NOT EXISTS TRANSLATED_NEWS (
      id VARCHAR(255) PRIMARY KEY,
      title VARCHAR(500) NOT NULL,
      content TEXT,
      published_at TIMESTAMP,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  
  -- CUSTOMERS 테이블
  CREATE TABLE IF NOT EXISTS CUSTOMERS (
      id VARCHAR(255) PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      token VARCHAR(500) UNIQUE NOT NULL,
      connection_id VARCHAR(255),
      is_active BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  
  -- 인덱스 생성
  CREATE INDEX IF NOT EXISTS idx_translated_news_published_at ON TRANSLATED_NEWS(published_at);
  CREATE INDEX IF NOT EXISTS idx_translated_news_created_at ON TRANSLATED_NEWS(created_at);
  CREATE INDEX IF NOT EXISTS idx_customers_token ON CUSTOMERS(token);
  CREATE INDEX IF NOT EXISTS idx_customers_connection_id ON CUSTOMERS(connection_id);
  ```

- [ ] `data.sql` 파일 생성 (초기 테스트 데이터)
  ```sql
  -- 테스트용 고객사 데이터
  INSERT INTO CUSTOMERS (id, name, token, is_active, created_at, updated_at) 
  VALUES 
      ('customer-001', 'Test Customer 1', 'test-token-001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('customer-002', 'Test Customer 2', 'test-token-002', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO NOTHING;
  
  -- 테스트용 뉴스 데이터
  INSERT INTO TRANSLATED_NEWS (id, title, content, published_at, created_at, updated_at)
  VALUES 
      ('news-001', '테스트 뉴스 제목 1', '테스트 뉴스 내용 1입니다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('news-002', '테스트 뉴스 제목 2', '테스트 뉴스 내용 2입니다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO NOTHING;
  ```

### 4단계: DTO 클래스 생성
- [ ] `NewsDto` 클래스 생성
  ```java
  public record NewsDto(
      String id,
      String title,
      String content,
      LocalDateTime publishedAt
  ) {}
  ```

- [ ] `CustomerDto` 클래스 생성
  ```java
  public record CustomerDto(
      String id,
      String name,
      String token,
      boolean isActive
  ) {}
  ```

- [ ] `CreateCustomerRequest` 클래스 생성
  ```java
  public record CreateCustomerRequest(
      String name
  ) {}
  ```

### 5단계: 기본 서비스 구현
- [ ] `TranslatedNewsService` 클래스 생성
  ```java
  @Service
  @Transactional
  public class TranslatedNewsService {
      private final TranslatedNewsRepository newsRepository;
      
      public TranslatedNewsService(TranslatedNewsRepository newsRepository) {
          this.newsRepository = newsRepository;
      }
      
      public Optional<TranslatedNews> findById(String id) {
          return newsRepository.findById(id);
      }
      
      public List<TranslatedNews> findRecentNews(int limit) {
          Pageable pageable = PageRequest.of(0, limit, Sort.by("publishedAt").descending());
          return newsRepository.findAllByOrderByPublishedAtDesc(pageable).getContent();
      }
      
      public TranslatedNews save(TranslatedNews news) {
          if (news.getCreatedAt() == null) {
              news.setCreatedAt(LocalDateTime.now());
          }
          news.setUpdatedAt(LocalDateTime.now());
          return newsRepository.save(news);
      }
  }
  ```

- [ ] `CustomerService` 클래스 생성
  ```java
  @Service
  @Transactional
  public class CustomerService {
      private final CustomerRepository customerRepository;
      private final PasswordEncoder passwordEncoder;
      
      public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
          this.customerRepository = customerRepository;
          this.passwordEncoder = passwordEncoder;
      }
      
      public Customer createCustomer(String name) {
          String token = generateToken();
          Customer customer = new Customer();
          customer.setId(UUID.randomUUID().toString());
          customer.setName(name);
          customer.setToken(token);
          customer.setActive(true);
          customer.setCreatedAt(LocalDateTime.now());
          customer.setUpdatedAt(LocalDateTime.now());
          
          return customerRepository.save(customer);
      }
      
      public Optional<Customer> findByToken(String token) {
          return customerRepository.findByToken(token);
      }
      
      public boolean isConnectionAvailable(String connectionId) {
          return !customerRepository.existsByConnectionId(connectionId);
      }
      
      private String generateToken() {
          return UUID.randomUUID().toString();
      }
  }
  ```

### 6단계: 데이터베이스 설정
- [ ] `application.yml`에 데이터베이스 설정 추가
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/alert_news
      username: postgres
      password: password
      driver-class-name: org.postgresql.Driver
    jpa:
      hibernate:
        ddl-auto: validate
      show-sql: true
      properties:
        hibernate:
          format_sql: true
          dialect: org.hibernate.dialect.PostgreSQLDialect
    sql:
      init:
        mode: always
        schema-locations: classpath:db/schema.sql
        data-locations: classpath:db/data.sql
  ```

## 🧪 검증 방법

### 1. 데이터베이스 연결 확인
```bash
# PostgreSQL 연결 테스트
docker exec -it news-stream-service-postgres-1 psql -U postgres -d alert_news
```

### 2. 테이블 생성 확인
```sql
-- PostgreSQL에서 실행
\dt
SELECT * FROM TRANSLATED_NEWS;
SELECT * FROM CUSTOMERS;
```

### 3. 애플리케이션 시작 확인
```bash
./gradlew bootRun
```

### 4. 로그에서 SQL 실행 확인
- Hibernate SQL 로그 확인
- 테이블 생성 로그 확인

## 📝 체크리스트

- [x] `TranslatedNews` 엔티티가 올바르게 생성됨
- [x] `Customer` 엔티티가 올바르게 생성됨
- [x] JPA Repository가 정상적으로 동작함
- [x] 데이터베이스 스키마가 올바르게 생성됨
- [x] 초기 테스트 데이터가 정상적으로 삽입됨
- [x] 기본 CRUD 서비스가 정상적으로 동작함
- [x] 데이터베이스 연결이 안정적으로 유지됨

## 🚨 주의사항

1. **엔티티 설계**: JPA 어노테이션을 올바르게 사용하여 데이터베이스 스키마 생성
2. **인덱스**: 자주 조회되는 컬럼에 인덱스 생성으로 성능 최적화
3. **트랜잭션**: 서비스 레이어에 적절한 트랜잭션 설정
4. **데이터 무결성**: 외래키 제약조건 및 유니크 제약조건 설정

## ✅ 완료된 작업

### 1. 도메인 모델 구현
- `TranslatedNews` 엔티티 클래스 생성 완료
- `Customer` 엔티티 클래스 생성 완료
- JPA 어노테이션을 사용한 데이터베이스 매핑 구현

### 2. 데이터 접근 계층 구현
- `TranslatedNewsRepository` 인터페이스 생성 완료
- `CustomerRepository` 인터페이스 생성 완료
- Spring Data JPA를 활용한 쿼리 메서드 구현

### 3. 서비스 계층 구현
- `TranslatedNewsService` 클래스 생성 완료
- `CustomerService` 클래스 생성 완료
- 트랜잭션 관리 및 비즈니스 로직 구현

### 4. DTO 클래스 구현
- `NewsDto` record 클래스 생성 완료
- `CustomerDto` record 클래스 생성 완료
- `CreateCustomerRequest` record 클래스 생성 완료

### 5. 컨트롤러 구현
- `NewsController` 생성 완료 (GET, POST 메서드)
- `CustomerController` 생성 완료 (GET, POST 메서드)
- RESTful API 엔드포인트 구현

### 6. 데이터베이스 설정
- PostgreSQL 데이터베이스 연결 설정 완료
- `schema.sql` 및 `data.sql` 파일 생성 완료
- Docker Compose 환경 구성 완료

### 7. 테스트 및 검증
- 애플리케이션 정상 실행 확인
- API 엔드포인트 동작 확인
- 데이터베이스 CRUD 작업 검증

## 🔗 다음 단계

이 단계가 완료되었으므로 다음 단계인 **Authentication** feature로 진행합니다.

## 📚 참고 자료

- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Hibernate User Guide](https://hibernate.org/orm/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JPA Best Practices](https://www.baeldung.com/jpa-hibernate-persistence-context)
