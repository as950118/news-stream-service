package com.news.stream.service;

import com.news.stream.model.Customer;
import com.news.stream.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
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
        
        Customer savedCustomer = customerRepository.save(customer);
        log.info("새 고객사가 생성되었습니다: {} (ID: {})", name, savedCustomer.getId());
        return savedCustomer;
    }
    
    public Optional<Customer> findById(String id) {
        return customerRepository.findById(id);
    }
    
    public Optional<Customer> findByToken(String token) {
        return customerRepository.findByToken(token);
    }
    
    public Optional<Customer> findByConnectionId(String connectionId) {
        return customerRepository.findByConnectionId(connectionId);
    }
    
    public List<Customer> findActiveCustomers() {
        return customerRepository.findByIsActiveTrue();
    }
    
    public boolean isConnectionAvailable(String customerId) {
        Optional<Customer> customer = findById(customerId);
        return customer.isPresent() && customer.get().getConnectionId() == null;
    }
    
    public boolean isConnectionIdAvailable(String connectionId) {
        return !customerRepository.existsByConnectionId(connectionId);
    }
    
    public Customer updateConnectionId(String customerId, String connectionId) {
        Customer customer = findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));
        
        customer.setConnectionId(connectionId);
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer updatedCustomer = customerRepository.save(customer);
        log.info("고객사 {}의 연결 ID가 업데이트되었습니다: {}", customerId, connectionId);
        return updatedCustomer;
    }
    
    public Customer removeConnectionId(String customerId) {
        Customer customer = findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));
        
        customer.setConnectionId(null);
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer updatedCustomer = customerRepository.save(customer);
        log.info("고객사 {}의 연결 ID가 제거되었습니다", customerId);
        return updatedCustomer;
    }
    
    /**
     * 세션 ID로 고객사의 연결 정보를 제거합니다.
     * 
     * @param sessionId WebSocket 세션 ID
     */
    public void removeConnectionIdBySessionId(String sessionId) {
        findByConnectionId(sessionId).ifPresent(customer -> {
            customer.setConnectionId(null);
            customer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(customer);
            log.info("고객사 {}의 연결 정보가 제거되었습니다: {}", customer.getId(), sessionId);
        });
    }
    
    public Customer save(Customer customer) {
        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(LocalDateTime.now());
        }
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }
    
    public void deleteById(String id) {
        customerRepository.deleteById(id);
        log.info("고객사가 삭제되었습니다: {}", id);
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}
