package com.news.stream.service;

import com.news.stream.model.Customer;
import com.news.stream.util.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {
    
    private final CustomerService customerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    
    public AuthenticationService(CustomerService customerService, 
                                JwtTokenProvider jwtTokenProvider,
                                PasswordEncoder passwordEncoder) {
        this.customerService = customerService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }
    
    public AuthResponse authenticateCustomer(String name, String password) {
        // 실제 구현에서는 고객사별 비밀번호 검증 로직 필요
        // 현재는 간단하게 고객사 생성 후 토큰 발급
        Customer customer = customerService.createCustomer(name);
        String token = jwtTokenProvider.generateToken(customer.getId());
        
        return new AuthResponse(customer.getId(), customer.getName(), token);
    }
    
    public Optional<Customer> validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return Optional.empty();
        }
        
        String customerId = jwtTokenProvider.getCustomerIdFromToken(token);
        return customerService.findById(customerId);
    }
    
    public boolean isConnectionAvailable(String customerId) {
        Optional<Customer> customer = customerService.findById(customerId);
        if (customer.isEmpty()) {
            return false;
        }
        
        // 고객사가 이미 연결되어 있으면 false
        return customer.get().getConnectionId() == null;
    }
    
    // 내부 클래스로 AuthResponse 정의
    public record AuthResponse(
        String customerId,
        String customerName,
        String token
    ) {}
}
