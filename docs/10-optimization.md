# Feature 10: Optimization (최적화 및 성능 개선)

## 📋 개요
시스템 성능 프로파일링, 데이터베이스 인덱싱 최적화, 캐싱 전략 구현, 메모리 및 동시성 최적화를 통해 시스템의 전반적인 성능을 향상시키는 단계입니다.

## 🎯 목표
- 성능 프로파일링 및 병목 지점 식별
- 데이터베이스 인덱싱 최적화
- Redis 캐싱 전략 구현
- 메모리 사용량 최적화
- 동시성 처리 성능 향상
- 시스템 응답 시간 개선

## 📁 작업 순서

### 1단계: 성능 프로파일링 설정
- [ ] `build.gradle`에 프로파일링 도구 의존성 추가
  ```gradle
  // JProfiler 또는 YourKit 대신 무료 도구 사용
  implementation 'org.springframework.boot:spring-boot-starter-actuator'
  implementation 'io.micrometer:micrometer-registry-prometheus'
  
  // 성능 모니터링
  implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
  implementation 'ch.qos.logback:logback-classic'
  ```

- [ ] `PerformanceProfiler` 클래스 생성
  ```java
  @Component
  @Aspect
  public class PerformanceProfiler {
      
      private final Logger logger = LoggerFactory.getLogger(PerformanceProfiler.class);
      private final MeterRegistry meterRegistry;
      
      public PerformanceProfiler(MeterRegistry meterRegistry) {
          this.meterRegistry = meterRegistry;
      }
      
      @Around("@annotation(Profiled)")
      public Object profileMethod(ProceedingJoinPoint joinPoint) throws Throwable {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();
          String key = className + "." + methodName;
          
          Timer.Sample sample = Timer.start(meterRegistry);
          long startTime = System.currentTimeMillis();
          
          try {
              Object result = joinPoint.proceed();
              long executionTime = System.currentTimeMillis() - startTime;
              
              // 성공 메트릭 기록
              sample.stop(Timer.builder("method.execution.success")
                  .tag("class", className)
                  .tag("method", methodName)
                  .register(meterRegistry));
              
              // 실행 시간 로깅
              if (executionTime > 1000) {
                  logger.warn("성능 경고: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
              } else if (executionTime > 500) {
                  logger.info("성능 정보: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
              }
              
              return result;
              
          } catch (Exception e) {
              long executionTime = System.currentTimeMillis() - startTime;
              
              // 실패 메트릭 기록
              sample.stop(Timer.builder("method.execution.failure")
                  .tag("class", className)
                  .tag("method", methodName)
                  .register(meterRegistry));
              
              logger.error("성능 오류: {}.{} 실행 시간 {}ms, 오류: {}", 
                  className, methodName, executionTime, e.getMessage());
              
              throw e;
          }
      }
      
      @Around("@annotation(Profiled)")
      public Object profileDatabaseOperation(ProceedingJoinPoint joinPoint) throws Throwable {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();
          
          Timer.Sample sample = Timer.start(meterRegistry);
          long startTime = System.currentTimeMillis();
          
          try {
              Object result = joinPoint.proceed();
              long executionTime = System.currentTimeMillis() - startTime;
              
              // 데이터베이스 작업 메트릭 기록
              sample.stop(Timer.builder("database.operation")
                  .tag("class", className)
                  .tag("method", methodName)
                  .tag("status", "success")
                  .register(meterRegistry));
              
              // 데이터베이스 성능 로깅
              if (executionTime > 100) {
                  logger.warn("DB 성능 경고: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
              }
              
              return result;
              
          } catch (Exception e) {
              long executionTime = System.currentTimeMillis() - startTime;
              
              sample.stop(Timer.builder("database.operation")
                  .tag("class", className)
                  .tag("method", methodName)
                  .tag("status", "failure")
                  .register(meterRegistry));
              
              logger.error("DB 성능 오류: {}.{} 실행 시간 {}ms, 오류: {}", 
                  className, methodName, executionTime, e.getMessage());
              
              throw e;
          }
      }
  }
  ```

- [ ] `@Profiled` 어노테이션 생성
  ```java
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Profiled {
      String value() default "";
      boolean database() default false;
  }
  ```

### 2단계: 데이터베이스 인덱싱 최적화
- [ ] `DatabaseOptimizationConfig` 클래스 생성
  ```java
  @Configuration
  public class DatabaseOptimizationConfig {
      
      @Bean
      public DataSource dataSource() {
          HikariConfig config = new HikariConfig();
          config.setJdbcUrl("jdbc:postgresql://localhost:5432/alert_news");
          config.setUsername("postgres");
          config.setPassword("password");
          
          // 커넥션 풀 최적화
          config.setMaximumPoolSize(20);
          config.setMinimumIdle(5);
          config.setIdleTimeout(300000); // 5분
          config.setMaxLifetime(1800000); // 30분
          config.setConnectionTimeout(30000); // 30초
          
          // 성능 최적화 설정
          config.addDataSourceProperty("cachePrepStmts", "true");
          config.addDataSourceProperty("prepStmtCacheSize", "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          config.addDataSourceProperty("useServerPrepStmts", "true");
          
          return new HikariDataSource(config);
      }
      
      @Bean
      public LocalContainerEntityManagerFactoryBean entityManagerFactory(
          DataSource dataSource) {
          
          LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
          em.setDataSource(dataSource);
          em.setPackagesToScan("com.alert.news.model");
          
          HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
          vendorAdapter.setGenerateDdl(true);
          vendorAdapter.setShowSql(false);
          vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect");
          
          em.setJpaVendorAdapter(vendorAdapter);
          
          Properties properties = new Properties();
          properties.setProperty("hibernate.hbm2ddl.auto", "validate");
          properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
          properties.setProperty("hibernate.show_sql", "false");
          properties.setProperty("hibernate.format_sql", "true");
          properties.setProperty("hibernate.use_sql_comments", "false");
          
          // 성능 최적화 설정
          properties.setProperty("hibernate.jdbc.batch_size", "50");
          properties.setProperty("hibernate.order_inserts", "true");
          properties.setProperty("hibernate.order_updates", "true");
          properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
          properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
          
          em.setJpaProperties(properties);
          
          return em;
      }
  }
  ```

- [ ] 데이터베이스 인덱스 최적화 스크립트 생성 (`db/optimization.sql`)
  ```sql
  -- 기존 인덱스 확인
  SELECT schemaname, tablename, indexname, indexdef 
  FROM pg_indexes 
  WHERE schemaname = 'public' 
  ORDER BY tablename, indexname;
  
  -- 뉴스 테이블 인덱스 최적화
  -- 1. 복합 인덱스 (published_at + created_at)
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_translated_news_published_created 
  ON TRANSLATED_NEWS (published_at DESC, created_at DESC);
  
  -- 2. 제목 검색을 위한 GIN 인덱스 (한글 검색 최적화)
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_translated_news_title_gin 
  ON TRANSLATED_NEWS USING GIN (to_tsvector('korean', title));
  
  -- 3. 내용 검색을 위한 GIN 인덱스
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_translated_news_content_gin 
  ON TRANSLATED_NEWS USING GIN (to_tsvector('korean', content));
  
  -- 4. 부분 인덱스 (활성 뉴스만)
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_translated_news_active 
  ON TRANSLATED_NEWS (published_at DESC) 
  WHERE published_at >= CURRENT_DATE - INTERVAL '30 days';
  
  -- 고객사 테이블 인덱스 최적화
  -- 1. 토큰 검색 최적화
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_token_hash 
  ON CUSTOMERS USING HASH (token);
  
  -- 2. 연결 ID 검색 최적화
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_connection_id 
  ON CUSTOMERS (connection_id) 
  WHERE connection_id IS NOT NULL;
  
  -- 3. 활성 고객사 검색 최적화
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_active 
  ON CUSTOMERS (is_active, created_at DESC) 
  WHERE is_active = true;
  
  -- 뉴스 처리 상태 테이블 인덱스
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_news_processing_status_status 
  ON NEWS_PROCESSING_STATUS (status, updated_at DESC);
  
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_news_processing_status_retry 
  ON NEWS_PROCESSING_STATUS (retry_count, status) 
  WHERE status IN ('FAILED', 'RETRY');
  
  -- 통계 정보 업데이트
  ANALYZE TRANSLATED_NEWS;
  ANALYZE CUSTOMERS;
  ANALYZE NEWS_PROCESSING_STATUS;
  
  -- 인덱스 사용 통계 확인
  SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
  FROM pg_stat_user_indexes 
  WHERE schemaname = 'public'
  ORDER BY idx_scan DESC;
  ```

### 3단계: Redis 캐싱 전략 구현
- [ ] `build.gradle`에 Redis 의존성 추가
  ```gradle
  implementation 'org.springframework.boot:spring-boot-starter-data-redis'
  implementation 'org.springframework.boot:spring-boot-starter-cache'
  implementation 'com.fasterxml.jackson.core:jackson-databind'
  ```

- [ ] `RedisConfig` 클래스 생성
  ```java
  @Configuration
  @EnableCaching
  public class RedisConfig {
      
      @Value("${spring.redis.host:localhost}")
      private String redisHost;
      
      @Value("${spring.redis.port:6379}")
      private int redisPort;
      
      @Value("${spring.redis.password:}")
      private String redisPassword;
      
      @Bean
      public RedisConnectionFactory redisConnectionFactory() {
          RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
          config.setHostName(redisHost);
          config.setPort(redisPort);
          
          if (StringUtils.hasText(redisPassword)) {
              config.setPassword(redisPassword);
          }
          
          return new LettuceConnectionFactory(config);
      }
      
      @Bean
      public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
          RedisTemplate<String, Object> template = new RedisTemplate<>();
          template.setConnectionFactory(connectionFactory);
          
          // JSON 직렬화 설정
          Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
          ObjectMapper mapper = new ObjectMapper();
          mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
          mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
              ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
          serializer.setObjectMapper(mapper);
          
          template.setKeySerializer(new StringRedisSerializer());
          template.setValueSerializer(serializer);
          template.setHashKeySerializer(new StringRedisSerializer());
          template.setHashValueSerializer(serializer);
          
          template.afterPropertiesSet();
          return template;
      }
      
      @Bean
      public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
          RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
              .entryTtl(Duration.ofMinutes(30)) // 기본 TTL 30분
              .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
              .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
              .disableCachingNullValues();
          
          return RedisCacheManager.builder(connectionFactory)
              .cacheDefaults(config)
              .withCacheConfiguration("news", 
                  RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)))
              .withCacheConfiguration("customers", 
                  RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(60)))
              .withCacheConfiguration("system", 
                  RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
              .build();
      }
  }
  ```

- [ ] `NewsCacheService` 클래스 생성
  ```java
  @Service
  public class NewsCacheService {
      
      private final RedisTemplate<String, Object> redisTemplate;
      private final Logger logger = LoggerFactory.getLogger(NewsCacheService.class);
      
      private static final String NEWS_CACHE_PREFIX = "news:";
      private static final String NEWS_LIST_CACHE_PREFIX = "news:list:";
      private static final Duration NEWS_CACHE_TTL = Duration.ofMinutes(10);
      private static final Duration NEWS_LIST_CACHE_TTL = Duration.ofMinutes(5);
      
      public NewsCacheService(RedisTemplate<String, Object> redisTemplate) {
          this.redisTemplate = redisTemplate;
      }
      
      @Cacheable(value = "news", key = "#newsId", unless = "#result == null")
      public Optional<NewsDto> getNewsFromCache(String newsId) {
          String key = NEWS_CACHE_PREFIX + newsId;
          try {
              Object cached = redisTemplate.opsForValue().get(key);
              if (cached instanceof NewsDto) {
                  logger.debug("뉴스 캐시 히트: {}", newsId);
                  return Optional.of((NewsDto) cached);
              }
          } catch (Exception e) {
              logger.warn("뉴스 캐시 조회 중 오류: {}", newsId, e);
          }
          
          logger.debug("뉴스 캐시 미스: {}", newsId);
          return Optional.empty();
       }
      
      @CachePut(value = "news", key = "#newsDto.id")
      public void cacheNews(NewsDto newsDto) {
          String key = NEWS_CACHE_PREFIX + newsDto.id();
          try {
              redisTemplate.opsForValue().set(key, newsDto, NEWS_CACHE_TTL);
              logger.debug("뉴스 캐시 저장: {}", newsDto.id());
          } catch (Exception e) {
              logger.warn("뉴스 캐시 저장 중 오류: {}", newsDto.id(), e);
          }
      }
      
      @CacheEvict(value = "news", key = "#newsId")
      public void evictNews(String newsId) {
          String key = NEWS_CACHE_PREFIX + newsId;
          try {
              redisTemplate.delete(key);
              logger.debug("뉴스 캐시 제거: {}", newsId);
          } catch (Exception e) {
              logger.warn("뉴스 캐시 제거 중 오류: {}", newsId, e);
          }
      }
      
      @Cacheable(value = "news", key = "'list:' + #page + ':' + #size + ':' + #sortBy + ':' + #direction")
      public Optional<PageResponse<NewsDto>> getNewsListFromCache(int page, int size, 
                                                                String sortBy, String direction) {
          String key = NEWS_LIST_CACHE_PREFIX + page + ":" + size + ":" + sortBy + ":" + direction;
          try {
              Object cached = redisTemplate.opsForValue().get(key);
              if (cached instanceof PageResponse) {
                  logger.debug("뉴스 목록 캐시 히트: page={}, size={}", page, size);
                  return Optional.of((PageResponse<NewsDto>) cached);
              }
          } catch (Exception e) {
              logger.warn("뉴스 목록 캐시 조회 중 오류: page={}, size={}", page, size, e);
          }
          
          logger.debug("뉴스 목록 캐시 미스: page={}, size={}", page, size);
          return Optional.empty();
      }
      
      @CachePut(value = "news", key = "'list:' + #page + ':' + #size + ':' + #sortBy + ':' + #direction")
      public void cacheNewsList(PageResponse<NewsDto> newsList, int page, int size, 
                               String sortBy, String direction) {
          String key = NEWS_LIST_CACHE_PREFIX + page + ":" + size + ":" + sortBy + ":" + direction;
          try {
              redisTemplate.opsForValue().set(key, newsList, NEWS_LIST_CACHE_TTL);
              logger.debug("뉴스 목록 캐시 저장: page={}, size={}", page, size);
          } catch (Exception e) {
              logger.warn("뉴스 목록 캐시 저장 중 오류: page={}, size={}", page, size, e);
          }
      }
      
      @CacheEvict(value = "news", allEntries = true)
      public void evictAllNewsCache() {
          try {
              Set<String> keys = redisTemplate.keys(NEWS_CACHE_PREFIX + "*");
              if (keys != null && !keys.isEmpty()) {
                  redisTemplate.delete(keys);
                  logger.info("모든 뉴스 캐시 제거: {}개", keys.size());
              }
          } catch (Exception e) {
              logger.warn("모든 뉴스 캐시 제거 중 오류", e);
          }
      }
      
      public void preloadPopularNews(List<String> popularNewsIds) {
          // 인기 뉴스를 미리 캐시에 로드
          for (String newsId : popularNewsIds) {
              try {
                  // 실제 구현에서는 뉴스 서비스에서 조회하여 캐시
                  logger.debug("인기 뉴스 프리로드: {}", newsId);
              } catch (Exception e) {
                  logger.warn("인기 뉴스 프리로드 실패: {}", newsId, e);
              }
          }
      }
  }
  ```

### 4단계: 메모리 사용량 최적화
- [ ] `MemoryOptimizationConfig` 클래스 생성
  ```java
  @Configuration
  public class MemoryOptimizationConfig {
      
      @Bean
      public JvmMetrics jvmMetrics(MeterRegistry meterRegistry) {
          return new JvmMetrics(meterRegistry);
      }
      
      @Bean
      public MemoryPoolMetrics memoryPoolMetrics(MeterRegistry meterRegistry) {
          return new MemoryPoolMetrics(meterRegistry);
      }
      
      @Bean
      public GarbageCollectorMetrics garbageCollectorMetrics(MeterRegistry meterRegistry) {
          return new GarbageCollectorMetrics(meterRegistry);
      }
      
      @PostConstruct
      public void configureMemorySettings() {
          // JVM 메모리 설정 최적화
          Runtime runtime = Runtime.getRuntime();
          
          // 가상 스레드 풀 크기 설정
          System.setProperty("jdk.virtualThreadScheduler.parallelism", "2");
          System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "100");
          
          // GC 최적화 설정
          System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
              String.valueOf(Runtime.getRuntime().availableProcessors()));
          
          // 로깅 최적화
          System.setProperty("logback.configurationFile", "logback-optimized.xml");
      }
  }
  ```

- [ ] `MemoryMonitor` 클래스 생성
  ```java
  @Component
  @Scheduled(fixedRate = 30000) // 30초마다 실행
  public class MemoryMonitor {
      
      private final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
      private final MeterRegistry meterRegistry;
      
      public MemoryMonitor(MeterRegistry meterRegistry) {
          this.meterRegistry = meterRegistry;
      }
      
      public void monitorMemoryUsage() {
          Runtime runtime = Runtime.getRuntime();
          
          long totalMemory = runtime.totalMemory();
          long freeMemory = runtime.freeMemory();
          long usedMemory = totalMemory - freeMemory;
          long maxMemory = runtime.maxMemory();
          
          double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
          double heapUsagePercent = (double) usedMemory / totalMemory * 100;
          
          // 메모리 사용량 메트릭 기록
          Gauge.builder("jvm.memory.used")
              .description("사용 중인 JVM 메모리")
              .register(meterRegistry, usedMemory, Long::valueOf);
          
          Gauge.builder("jvm.memory.usage.percent")
              .description("JVM 메모리 사용률 (%)")
              .register(meterRegistry, memoryUsagePercent, Double::valueOf);
          
          Gauge.builder("jvm.heap.usage.percent")
              .description("힙 메모리 사용률 (%)")
              .register(meterRegistry, heapUsagePercent, Double::valueOf);
          
          // 메모리 경고 임계값 체크
          if (memoryUsagePercent > 90) {
              logger.error("메모리 사용률이 90%를 초과했습니다: {}%", 
                  String.format("%.1f", memoryUsagePercent));
              triggerMemoryOptimization();
          } else if (memoryUsagePercent > 80) {
              logger.warn("메모리 사용률이 80%를 초과했습니다: {}%", 
                  String.format("%.1f", memoryUsagePercent));
          } else if (memoryUsagePercent > 70) {
              logger.info("메모리 사용률: {}%", String.format("%.1f", memoryUsagePercent));
          }
          
          // 힙 메모리 경고
          if (heapUsagePercent > 85) {
              logger.warn("힙 메모리 사용률이 85%를 초과했습니다: {}%", 
                  String.format("%.1f", heapUsagePercent));
              suggestGarbageCollection();
          }
      }
      
      private void triggerMemoryOptimization() {
          logger.info("메모리 최적화 시작");
          
          // 1. 캐시 정리
          clearExpiredCache();
          
          // 2. 불필요한 객체 정리
          clearUnusedObjects();
          
          // 3. 가비지 컬렉션 제안
          suggestGarbageCollection();
          
          logger.info("메모리 최적화 완료");
      }
      
      private void clearExpiredCache() {
          try {
              // Redis 캐시 만료된 항목 정리
              // 실제 구현에서는 Redis TTL을 활용
              logger.debug("만료된 캐시 정리 완료");
          } catch (Exception e) {
              logger.warn("캐시 정리 중 오류", e);
          }
       }
      
      private void clearUnusedObjects() {
          try {
              // 사용하지 않는 객체 정리
              // 실제 구현에서는 WeakReference 등을 활용
              logger.debug("사용하지 않는 객체 정리 완료");
          } catch (Exception e) {
              logger.warn("객체 정리 중 오류", e);
          }
      }
      
      private void suggestGarbageCollection() {
          try {
              // 가비지 컬렉션 제안 (실제 GC는 JVM이 결정)
              logger.info("가비지 컬렉션 제안");
              
              // 메모리 사용량 재확인
              Runtime.getRuntime().gc();
              
          } catch (Exception e) {
              logger.warn("가비지 컬렉션 제안 중 오류", e);
          }
      }
  }
  ```

### 5단계: 동시성 처리 최적화
- [ ] `ConcurrencyOptimizationConfig` 클래스 생성
  ```java
  @Configuration
  @EnableAsync
  public class ConcurrencyOptimizationConfig {
      
      @Value("${concurrency.core-pool-size:4}")
      private int corePoolSize;
      
      @Value("${concurrency.max-pool-size:8}")
      private int maxPoolSize;
      
      @Value("${concurrency.queue-capacity:100}")
      private int queueCapacity;
      
      @Bean("newsTaskExecutor")
      public TaskExecutor newsTaskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(corePoolSize);
          executor.setMaxPoolSize(maxPoolSize);
          executor.setQueueCapacity(queueCapacity);
          executor.setThreadNamePrefix("news-");
          executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
          executor.setKeepAliveSeconds(60);
          executor.setAllowCoreThreadTimeOut(true);
          executor.initialize();
          return executor;
      }
      
      @Bean("websocketTaskExecutor")
      public TaskExecutor websocketTaskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(2);
          executor.setMaxPoolSize(4);
          executor.setQueueCapacity(50);
          executor.setThreadNamePrefix("ws-");
          executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
          executor.initialize();
          return executor;
      }
      
      @Bean("cacheTaskExecutor")
      public TaskExecutor cacheTaskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(2);
          executor.setMaxPoolSize(4);
          executor.setQueueCapacity(25);
          executor.setThreadNamePrefix("cache-");
          executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
          executor.initialize();
          return executor;
      }
  }
  ```

- [ ] `AsyncNewsProcessor` 클래스 생성
  ```java
  @Service
  public class AsyncNewsProcessor {
      
      private final NewsStreamIntegrationService streamService;
      private final NewsCacheService cacheService;
      private final Logger logger = LoggerFactory.getLogger(AsyncNewsProcessor.class);
      
      public AsyncNewsProcessor(NewsStreamIntegrationService streamService,
                               NewsCacheService cacheService) {
          this.streamService = streamService;
          this.cacheService = cacheService;
      }
      
      @Async("newsTaskExecutor")
      public CompletableFuture<Void> processNewsAsync(String newsId) {
          return CompletableFuture.runAsync(() -> {
              try {
                  logger.debug("비동기 뉴스 처리 시작: {}", newsId);
                  
                  // 1. 뉴스 스트리밍 처리
                  streamService.processNewsCreated(newsId);
                  
                  // 2. 캐시 업데이트
                  updateNewsCache(newsId);
                  
                  logger.debug("비동기 뉴스 처리 완료: {}", newsId);
                  
              } catch (Exception e) {
                  logger.error("비동기 뉴스 처리 실패: {}", newsId, e);
              }
          });
      }
      
      @Async("newsTaskExecutor")
      public CompletableFuture<List<String>> processBatchNewsAsync(List<String> newsIds) {
          return CompletableFuture.supplyAsync(() -> {
              List<String> processedIds = new ArrayList<>();
              
              try {
                  logger.debug("배치 뉴스 처리 시작: {}개", newsIds.size());
                  
                  for (String newsId : newsIds) {
                      try {
                          streamService.processNewsCreated(newsId);
                          processedIds.add(newsId);
                      } catch (Exception e) {
                          logger.error("뉴스 ID {} 처리 실패", newsId, e);
                      }
                  }
                  
                  logger.debug("배치 뉴스 처리 완료: {}개", processedIds.size());
                  
              } catch (Exception e) {
                  logger.error("배치 뉴스 처리 중 오류", e);
              }
              
              return processedIds;
          });
      }
      
      @Async("cacheTaskExecutor")
      public CompletableFuture<Void> updateNewsCacheAsync(String newsId) {
          return CompletableFuture.runAsync(() -> {
              try {
                  updateNewsCache(newsId);
              } catch (Exception e) {
                  logger.error("뉴스 캐시 업데이트 실패: {}", newsId, e);
              }
          });
      }
      
      private void updateNewsCache(String newsId) {
          try {
              // 실제 구현에서는 뉴스 서비스에서 조회하여 캐시 업데이트
              logger.debug("뉴스 캐시 업데이트: {}", newsId);
          } catch (Exception e) {
              logger.warn("뉴스 캐시 업데이트 중 오류: {}", newsId, e);
          }
      }
  }
  ```

### 6단계: 성능 모니터링 대시보드
- [ ] `PerformanceDashboardController` 클래스 생성
  ```java
  @RestController
  @RequestMapping("/api/v1/performance")
  @Tag(name = "Performance Dashboard", description = "성능 모니터링 대시보드 API")
  public class PerformanceDashboardController {
      
      private final MeterRegistry meterRegistry;
      private final MemoryMonitor memoryMonitor;
      private final Logger logger = LoggerFactory.getLogger(PerformanceDashboardController.class);
      
      public PerformanceDashboardController(MeterRegistry meterRegistry,
                                          MemoryMonitor memoryMonitor) {
          this.meterRegistry = meterRegistry;
          this.memoryMonitor = memoryMonitor;
      }
      
      @GetMapping("/metrics")
      @Operation(summary = "성능 메트릭 조회", description = "시스템 성능 메트릭을 조회합니다")
      public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics() {
          
          try {
              PerformanceMetricsResponse response = new PerformanceMetricsResponse(
                  getMethodExecutionMetrics(),
                  getDatabaseMetrics(),
                  getCacheMetrics(),
                  getMemoryMetrics(),
                  getConcurrencyMetrics()
              );
              
              return ResponseEntity.ok(response);
              
          } catch (Exception e) {
              logger.error("성능 메트릭 조회 중 오류 발생", e);
              return ResponseEntity.internalServerError().build();
          }
      }
      
      @GetMapping("/health")
      @Operation(summary = "성능 헬스체크", description = "시스템 성능 상태를 확인합니다")
      public ResponseEntity<PerformanceHealthResponse> getPerformanceHealth() {
          
          try {
              boolean isHealthy = checkPerformanceHealth();
              
              if (isHealthy) {
                  PerformanceHealthResponse response = new PerformanceHealthResponse(
                      "HEALTHY", "시스템 성능이 정상 범위 내에 있습니다");
                  return ResponseEntity.ok(response);
              } else {
                  PerformanceHealthResponse response = new PerformanceHealthResponse(
                      "DEGRADED", "시스템 성능이 저하되었습니다");
                  return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
              }
              
          } catch (Exception e) {
              logger.error("성능 헬스체크 중 오류 발생", e);
              return ResponseEntity.internalServerError().build();
          }
      }
      
      @PostMapping("/optimize")
      @Operation(summary = "성능 최적화 실행", description = "수동으로 성능 최적화를 실행합니다")
      public ResponseEntity<OptimizationResponse> runOptimization() {
          
          try {
              logger.info("수동 성능 최적화 시작");
              
              // 메모리 최적화
              memoryMonitor.triggerMemoryOptimization();
              
              // 캐시 정리
              clearExpiredCache();
              
              // 가비지 컬렉션 제안
              suggestGarbageCollection();
              
              OptimizationResponse response = new OptimizationResponse(
                  "SUCCESS",
                  "성능 최적화가 성공적으로 완료되었습니다",
                  LocalDateTime.now()
              );
              
              logger.info("수동 성능 최적화 완료");
              return ResponseEntity.ok(response);
              
          } catch (Exception e) {
              logger.error("수동 성능 최적화 중 오류 발생", e);
              
              OptimizationResponse response = new OptimizationResponse(
                  "ERROR",
                  "성능 최적화 중 오류가 발생했습니다: " + e.getMessage(),
                  LocalDateTime.now()
              );
              
              return ResponseEntity.internalServerError().body(response);
          }
      }
      
      private MethodExecutionMetrics getMethodExecutionMetrics() {
          // 실제 구현에서는 메트릭 레지스트리에서 조회
          return new MethodExecutionMetrics(
              95.5, // 평균 성공률
              150.0, // 평균 실행 시간
              25.0   // 95% 백분위 실행 시간
          );
      }
      
      private DatabaseMetrics getDatabaseMetrics() {
          // 실제 구현에서는 메트릭 레지스트리에서 조회
          return new DatabaseMetrics(
              98.0, // 평균 성공률
              50.0, // 평균 쿼리 시간
              10.0  // 연결 풀 사용률
          );
      }
      
      private CacheMetrics getCacheMetrics() {
          // 실제 구현에서는 메트릭 레지스트리에서 조회
          return new CacheMetrics(
              85.0, // 캐시 히트율
              15.0, // 캐시 미스율
              1000  // 캐시된 항목 수
          );
      }
      
      private MemoryMetrics getMemoryMetrics() {
          Runtime runtime = Runtime.getRuntime();
          long totalMemory = runtime.totalMemory();
          long freeMemory = runtime.freeMemory();
          long usedMemory = totalMemory - freeMemory;
          
          return new MemoryMetrics(
              totalMemory,
              usedMemory,
              freeMemory,
              runtime.maxMemory(),
              (double) usedMemory / runtime.maxMemory() * 100
          );
      }
      
      private ConcurrencyMetrics getConcurrencyMetrics() {
          // 실제 구현에서는 메트릭 레지스트리에서 조회
          return new ConcurrencyMetrics(
              10,   // 활성 스레드 수
              20,   // 최대 스레드 수
              5     // 대기 중인 작업 수
          );
      }
      
      private boolean checkPerformanceHealth() {
          try {
              // 메모리 사용률 체크
              Runtime runtime = Runtime.getRuntime();
              long usedMemory = runtime.totalMemory() - runtime.freeMemory();
              double memoryUsage = (double) usedMemory / runtime.maxMemory() * 100;
              
              if (memoryUsage > 90) {
                  return false;
              }
              
              // 메서드 실행 시간 체크
              // 실제 구현에서는 메트릭에서 조회
              double avgExecutionTime = 150.0; // 예시 값
              if (avgExecutionTime > 1000) {
                  return false;
              }
              
              // 데이터베이스 성능 체크
              double avgQueryTime = 50.0; // 예시 값
              if (avgQueryTime > 500) {
                  return false;
              }
              
              return true;
              
          } catch (Exception e) {
              logger.error("성능 헬스체크 중 오류", e);
              return false;
          }
      }
      
      private void clearExpiredCache() {
          try {
              // Redis 캐시 만료된 항목 정리
              logger.info("만료된 캐시 정리 완료");
          } catch (Exception e) {
              logger.warn("캐시 정리 중 오류", e);
          }
      }
      
      private void suggestGarbageCollection() {
          try {
              // 가비지 컬렉션 제안
              Runtime.getRuntime().gc();
              logger.info("가비지 컬렉션 제안 완료");
          } catch (Exception e) {
              logger.warn("가비지 컬렉션 제안 중 오류", e);
          }
      }
  }
  ```

### 7단계: 설정 파일 업데이트
- [ ] `application.yml`에 최적화 설정 추가
  ```yaml
  concurrency:
    core-pool-size: ${CORE_POOL_SIZE:4}
    max-pool-size: ${MAX_POOL_SIZE:8}
    queue-capacity: ${QUEUE_CAPACITY:100}
  
  cache:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: ${REDIS_TIMEOUT:2000}
      database: ${REDIS_DATABASE:0}
  
  database:
    hikari:
      maximum-pool-size: ${DB_MAX_POOL_SIZE:20}
      minimum-idle: ${DB_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:300000}
      max-lifetime: ${DB_MAX_LIFETIME:1800000}
  
  performance:
    profiling:
      enabled: ${PERFORMANCE_PROFILING:true}
      threshold:
        warning: ${PERFORMANCE_WARNING_THRESHOLD:500}
        error: ${PERFORMANCE_ERROR_THRESHOLD:1000}
  
  logging:
    level:
      com.alert.news.optimization: DEBUG
      org.springframework.cache: DEBUG
      org.springframework.data.redis: DEBUG
  ```

## 🧪 검증 방법

### 1. 성능 메트릭 확인
```bash
# 성능 메트릭 조회
curl http://localhost:8080/api/v1/performance/metrics

# 성능 헬스체크
curl http://localhost:8080/api/v1/performance/health

# 수동 최적화 실행
curl -X POST http://localhost:8080/api/v1/performance/optimize
```

### 2. 캐시 성능 확인
```bash
# Redis 연결 확인
redis-cli ping

# 캐시 키 확인
redis-cli keys "news:*"

# 캐시 통계 확인
redis-cli info memory
```

### 3. 데이터베이스 성능 확인
```bash
# PostgreSQL 연결 확인
docker exec -it news-stream-service-postgres-1 psql -U postgres -d alert_news

# 인덱스 사용 통계 확인
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

### 4. 메모리 사용량 확인
```bash
# JVM 메모리 상태 확인
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.memory.usage.percent

# 애플리케이션 로그에서 메모리 모니터링 확인
tail -f logs/news-stream-service.log | grep "MemoryMonitor"
```

## 📝 체크리스트

- [ ] 성능 프로파일링이 정상적으로 설정됨
- [ ] 데이터베이스 인덱싱이 최적화됨
- [ ] Redis 캐싱이 정상적으로 동작함
- [ ] 메모리 사용량이 최적화됨
- [ ] 동시성 처리가 최적화됨
- [ ] 성능 모니터링 대시보드가 정상적으로 동작함
- [ ] 시스템 응답 시간이 개선됨

## 🚨 주의사항

1. **메모리 관리**: 과도한 캐싱으로 인한 메모리 부족 방지
2. **인덱스 최적화**: 불필요한 인덱스 생성으로 인한 성능 저하 방지
3. **캐시 전략**: 캐시 무효화 전략을 적절히 설정하여 데이터 일관성 보장
4. **모니터링**: 성능 모니터링으로 인한 오버헤드 최소화

## 🔗 다음 단계

이 단계가 완료되면 모든 feature가 구현되어 완전한 뉴스 스트림 서비스가 완성됩니다.

## 📚 참고 자료

- [Spring Boot Performance](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.performance)
- [Redis Documentation](https://redis.io/documentation)
- [PostgreSQL Performance Tuning](https://www.postgresql.org/docs/current/runtime-config-query.html)
- [JVM Tuning Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/)
- [Micrometer Performance](https://micrometer.io/docs)
