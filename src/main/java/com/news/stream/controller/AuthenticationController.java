package com.news.stream.controller;

import com.news.stream.dto.AuthRequest;
import com.news.stream.dto.ConnectionStatusResponse;
import com.news.stream.model.Customer;
import com.news.stream.service.AuthenticationService;
import com.news.stream.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    private final CustomerService customerService;
    
    public AuthenticationController(AuthenticationService authenticationService,
                                   CustomerService customerService) {
        this.authenticationService = authenticationService;
        this.customerService = customerService;
    }
    
    @PostMapping("/customers")
    @Operation(summary = "고객사 인증", description = "고객사 인증을 수행하고 토큰을 발급합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<AuthenticationService.AuthResponse> authenticateCustomer(
        @Parameter(description = "인증 요청 정보", required = true)
        @Valid @RequestBody AuthRequest request) {
        
        log.info("고객사 인증 요청: {}", request.name());
        
        try {
            AuthenticationService.AuthResponse response = authenticationService.authenticateCustomer(
                request.name(), request.password());
            
            log.info("고객사 인증 성공: {} (ID: {})", request.name(), response.customerId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("고객사 인증 실패: {}", request.name(), e);
            throw e;
        }
    }
    
    @GetMapping("/customers/{id}/connections")
    @Operation(summary = "연결 상태 조회", description = "고객사의 WebSocket 연결 상태를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "연결 상태 조회 성공"),
        @ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음")
    })
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus(
        @Parameter(description = "고객사 ID", required = true)
        @PathVariable String id) {
        
        log.debug("연결 상태 조회 요청: {}", id);
        
        Optional<Customer> customer = customerService.findById(id);
        if (customer.isEmpty()) {
            log.warn("연결 상태 조회 실패: 고객사를 찾을 수 없음 - {}", id);
            return ResponseEntity.notFound().build();
        }
        
        Customer cust = customer.get();
        ConnectionStatusResponse response = new ConnectionStatusResponse(
            cust.getId(),
            cust.getName(),
            cust.getConnectionId(),
            cust.getConnectionId() != null,
            cust.getUpdatedAt()
        );
        
        log.debug("연결 상태 조회 완료: {} - 연결됨: {}", id, response.isConnected());
        return ResponseEntity.ok(response);
    }
}
