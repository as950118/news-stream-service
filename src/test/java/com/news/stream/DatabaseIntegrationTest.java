package com.news.stream;

import com.news.stream.model.Customer;
import com.news.stream.model.TranslatedNews;
import com.news.stream.repository.CustomerRepository;
import com.news.stream.repository.TranslatedNewsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 데이터베이스 통합 테스트
 * JPA 리포지토리와 엔티티 매핑을 테스트합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
class DatabaseIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TranslatedNewsRepository translatedNewsRepository;

    @Test
    void 고객사_엔티티를_저장하고_조회할_수_있어야_한다() {
        // Given
        Customer customer = new Customer();
        customer.setId("test-customer");
        customer.setName("테스트 고객사");
        customer.setConnectionId("test-connection");

        // When
        Customer savedCustomer = customerRepository.save(customer);
        Customer foundCustomer = customerRepository.findById("test-customer").orElse(null);

        // Then
        assertThat(savedCustomer).isNotNull();
        assertThat(foundCustomer).isNotNull();
        assertThat(foundCustomer.getName()).isEqualTo("테스트 고객사");
        assertThat(foundCustomer.getConnectionId()).isEqualTo("test-connection");
    }

    @Test
    void 번역된_뉴스_엔티티를_저장하고_조회할_수_있어야_한다() {
        // Given
        TranslatedNews news = new TranslatedNews();
        news.setId("test-news");
        news.setTitle("테스트 뉴스");
        news.setContent("테스트 내용");
        news.setPublishedAt(LocalDateTime.now());
        news.setCreatedAt(LocalDateTime.now());

        // When
        TranslatedNews savedNews = translatedNewsRepository.save(news);
        TranslatedNews foundNews = translatedNewsRepository.findById("test-news").orElse(null);

        // Then
        assertThat(savedNews).isNotNull();
        assertThat(foundNews).isNotNull();
        assertThat(foundNews.getTitle()).isEqualTo("테스트 뉴스");
        assertThat(foundNews.getContent()).isEqualTo("테스트 내용");
    }

    @Test
    void 모든_리포지토리가_정상적으로_주입되어야_한다() {
        // 리포지토리 빈들이 정상적으로 주입되는지 확인
        assertThat(customerRepository).isNotNull();
        assertThat(translatedNewsRepository).isNotNull();
    }
}
