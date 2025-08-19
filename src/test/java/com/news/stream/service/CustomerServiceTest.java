package com.news.stream.service;

import com.news.stream.TestDataBuilder;
import com.news.stream.model.Customer;
import com.news.stream.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CustomerService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    @BeforeEach
    void setUp() {
        // 테스트 전 초기화 작업
    }
    
    @Test
    @DisplayName("고객사를 생성할 수 있어야 한다")
    void shouldCreateCustomer() {
        // Given
        String customerName = "Test Customer";
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId("customer-001");
            return customer;
        });
        
        // When
        Customer result = customerService.createCustomer(customerName);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(customerName);
        assertThat(result.getToken()).isNotNull();
        assertThat(result.isActive()).isTrue();
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    @DisplayName("토큰으로 고객사를 찾을 수 있어야 한다")
    void shouldFindCustomerByToken() {
        // Given
        String token = "test-token";
        Customer mockCustomer = TestDataBuilder.createCustomer("customer-001");
        mockCustomer.setToken(token); // 특정 토큰 설정
        when(customerRepository.findByToken(token)).thenReturn(Optional.of(mockCustomer));
        
        // When
        Optional<Customer> result = customerService.findByToken(token);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(token);
        verify(customerRepository).findByToken(token);
    }
    
    @Test
    @DisplayName("연결 ID 사용 가능 여부를 확인할 수 있어야 한다")
    void shouldCheckConnectionAvailability() {
        // Given
        String customerId = "customer-001";
        Customer mockCustomer = TestDataBuilder.createCustomer(customerId);
        mockCustomer.setConnectionId(null); // 연결되지 않은 상태
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        
        // When
        boolean result = customerService.isConnectionAvailable(customerId);
        
        // Then
        assertThat(result).isTrue();
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    @DisplayName("고객사 ID로 고객사를 찾을 수 있어야 한다")
    void shouldFindCustomerById() {
        // Given
        String customerId = "customer-001";
        Customer mockCustomer = TestDataBuilder.createCustomer(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        
        // When
        Optional<Customer> result = customerService.findById(customerId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(customerId);
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    @DisplayName("활성 고객사 목록을 조회할 수 있어야 한다")
    void shouldFindActiveCustomers() {
        // Given
        List<Customer> mockCustomers = TestDataBuilder.createCustomerList(3);
        when(customerRepository.findByIsActiveTrue()).thenReturn(mockCustomers);
        
        // When
        List<Customer> result = customerService.findActiveCustomers();
        
        // Then
        assertThat(result).hasSize(3);
        verify(customerRepository).findByIsActiveTrue();
    }
    
    @Test
    @DisplayName("연결 ID를 업데이트할 수 있어야 한다")
    void shouldUpdateConnectionId() {
        // Given
        String customerId = "customer-001";
        String connectionId = "connection-001";
        Customer mockCustomer = TestDataBuilder.createCustomer(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);
        
        // When
        Customer result = customerService.updateConnectionId(customerId, connectionId);
        
        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(mockCustomer);
    }
    
    @Test
    @DisplayName("연결 ID를 제거할 수 있어야 한다")
    void shouldRemoveConnectionId() {
        // Given
        String customerId = "customer-001";
        Customer mockCustomer = TestDataBuilder.createCustomer(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);
        
        // When
        Customer result = customerService.removeConnectionId(customerId);
        
        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(mockCustomer);
    }
    
    @Test
    @DisplayName("연결 ID 사용 가능 여부를 확인할 수 있어야 한다")
    void shouldCheckConnectionIdAvailability() {
        // Given
        String connectionId = "test-connection";
        when(customerRepository.existsByConnectionId(connectionId)).thenReturn(false);
        
        // When
        boolean result = customerService.isConnectionIdAvailable(connectionId);
        
        // Then
        assertThat(result).isTrue();
        verify(customerRepository).existsByConnectionId(connectionId);
    }
}
