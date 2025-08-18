package com.news.stream.service;

import com.news.stream.model.Customer;
import com.news.stream.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
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
        
        return customerRepository.save(customer);
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
        if (customer.isEmpty()) {
            return false;
        }
        
        // 고객사가 이미 연결되어 있으면 false
        return customer.get().getConnectionId() == null;
    }
    
    public boolean isConnectionIdAvailable(String connectionId) {
        return !customerRepository.existsByConnectionId(connectionId);
    }
    
    public Customer updateConnectionId(String customerId, String connectionId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setConnectionId(connectionId);
            customer.setUpdatedAt(LocalDateTime.now());
            return customerRepository.save(customer);
        }
        throw new RuntimeException("Customer not found with id: " + customerId);
    }
    
    public Customer removeConnectionId(String customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setConnectionId(null);
            customer.setUpdatedAt(LocalDateTime.now());
            return customerRepository.save(customer);
        }
        throw new RuntimeException("Customer not found with id: " + customerId);
    }
    
    /**
     * 세션 ID로 고객사의 연결 정보를 제거합니다.
     * 
     * @param sessionId WebSocket 세션 ID
     */
    public void removeConnectionIdBySessionId(String sessionId) {
        Optional<Customer> customerOpt = findByConnectionId(sessionId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setConnectionId(null);
            customer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(customer);
            logger.info("고객사 {}의 연결 정보가 제거되었습니다: {}", customer.getId(), sessionId);
        }
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
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}
