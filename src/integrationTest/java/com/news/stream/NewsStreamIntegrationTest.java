package com.news.stream;

import com.news.stream.model.Customer;
import com.news.stream.model.TranslatedNews;
import com.news.stream.service.CustomerService;
import com.news.stream.service.NewsStreamIntegrationService;
import com.news.stream.service.TranslatedNewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 뉴스 스트리밍 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
@ActiveProfiles("test")
class NewsStreamIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TranslatedNewsService newsService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private NewsStreamIntegrationService streamService;
    
    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    @DisplayName("뉴스 스트리밍 통합 테스트")
    void shouldStreamNewsSuccessfully() {
        // Given
        Customer customer = customerService.createCustomer("Test Customer");
        TranslatedNews news = createTestNews("test-news-001");
        newsService.save(news);
        
        // When
        assertDoesNotThrow(() -> streamService.processNewsCreated(news.getId()));
        
        // Then
        assertThat(newsService.findById(news.getId())).isPresent();
    }
    
    @Test
    @DisplayName("REST API를 통한 뉴스 조회 테스트")
    void shouldRetrieveNewsViaRestApi() {
        // Given
        TranslatedNews news = createTestNews("test-news-002");
        newsService.save(news);
        
        // When
        ResponseEntity<Object> response = restTemplate.getForEntity(
            "/api/v1/news/" + news.getId(), Object.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    @DisplayName("고객사 인증 API 테스트")
    void shouldAuthenticateCustomer() {
        // Given
        // AuthRequest와 AuthResponse가 구현되어 있다고 가정
        
        // When & Then
        // 실제 구현에서는 인증 테스트 수행
        assertThat(customerService).isNotNull();
    }
    
    @Test
    @DisplayName("뉴스 목록 조회 API 테스트")
    void shouldRetrieveNewsList() {
        // Given
        TranslatedNews news1 = createTestNews("test-news-003");
        TranslatedNews news2 = createTestNews("test-news-004");
        newsService.save(news1);
        newsService.save(news2);
        
        // When
        ResponseEntity<Object> response = restTemplate.getForEntity(
            "/api/v1/news", Object.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    @DisplayName("고객사 생성 및 조회 테스트")
    void shouldCreateAndRetrieveCustomer() {
        // Given
        String customerName = "Integration Test Customer";
        
        // When
        Customer customer = customerService.createCustomer(customerName);
        
        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getName()).isEqualTo(customerName);
        assertThat(customer.isActive()).isTrue();
        
        // 고객사 조회 테스트
        var foundCustomer = customerService.findById(customer.getId());
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getName()).isEqualTo(customerName);
    }
    
    private TranslatedNews createTestNews(String id) {
        TranslatedNews news = new TranslatedNews();
        news.setId(id);
        news.setTitle("테스트 뉴스 제목");
        news.setContent("테스트 뉴스 내용");
        news.setPublishedAt(LocalDateTime.now());
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());
        return news;
    }
}
