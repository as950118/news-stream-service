package com.news.stream.service;

import com.news.stream.model.Customer;
import com.news.stream.util.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
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
        log.debug("고객사 인증 요청: {}", name);
        
        try {
            // 실제 구현에서는 고객사별 비밀번호 검증 로직 필요
            // 현재는 간단하게 고객사 생성 후 토큰 발급
            Customer customer = customerService.createCustomer(name);
            String token = jwtTokenProvider.generateToken(customer.getId());
            
            log.info("고객사 인증 성공: {} (ID: {})", name, customer.getId());
            return new AuthResponse(customer.getId(), customer.getName(), token);
            
        } catch (Exception e) {
            log.error("고객사 인증 실패: {}", name, e);
            throw e;
        }
    }
    
    public Optional<Customer> validateToken(String token) {
        log.debug("토큰 검증 요청");
        
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("유효하지 않은 토큰");
            return Optional.empty();
        }
        
        String customerId = jwtTokenProvider.getCustomerIdFromToken(token);
        Optional<Customer> customer = customerService.findById(customerId);
        
        if (customer.isPresent()) {
            log.debug("토큰 검증 성공: {}", customerId);
        } else {
            log.warn("토큰에 해당하는 고객사를 찾을 수 없음: {}", customerId);
        }
        
        return customer;
    }
    
    public boolean isConnectionAvailable(String customerId) {
        log.debug("연결 가능 여부 확인: {}", customerId);
        
        Optional<Customer> customer = customerService.findById(customerId);
        if (customer.isEmpty()) {
            log.warn("고객사를 찾을 수 없음: {}", customerId);
            return false;
        }
        
        boolean isAvailable = customer.get().getConnectionId() == null;
        log.debug("고객사 {} 연결 가능 여부: {}", customerId, isAvailable);
        
        return isAvailable;
    }
    
    // 내부 클래스로 AuthResponse 정의
    public record AuthResponse(
        String customerId,
        String customerName,
        String token
    ) {}
}
