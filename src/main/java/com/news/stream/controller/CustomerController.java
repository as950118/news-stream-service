package com.news.stream.controller;

import com.news.stream.dto.CustomerDto;
import com.news.stream.dto.CreateCustomerRequest;
import com.news.stream.model.Customer;
import com.news.stream.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer", description = "고객사 관리 API")
public class CustomerController {
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    @PostMapping
    @Operation(summary = "고객사 생성", description = "새로운 고객사를 생성합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고객사 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<CustomerDto> createCustomer(
        @Parameter(description = "고객사 생성 요청", required = true)
        @RequestBody CreateCustomerRequest request) {
        
        log.info("고객사 생성 요청: {}", request.name());
        
        Customer customer = customerService.createCustomer(request.name());
        CustomerDto dto = convertToDto(customer);
        
        log.info("고객사 생성 완료: {} (ID: {})", request.name(), customer.getId());
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "고객사 조회", description = "ID로 특정 고객사를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고객사 조회 성공"),
        @ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음")
    })
    public ResponseEntity<CustomerDto> getCustomerById(
        @Parameter(description = "고객사 ID", required = true)
        @PathVariable String id) {
        
        log.debug("고객사 조회 요청: {}", id);
        
        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isEmpty()) {
            log.warn("고객사 조회 실패: 고객사를 찾을 수 없음 - {}", id);
            return ResponseEntity.notFound().build();
        }
        
        CustomerDto dto = convertToDto(customerOpt.get());
        log.debug("고객사 조회 완료: {}", id);
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping
    @Operation(summary = "활성 고객사 목록 조회", description = "활성 상태인 모든 고객사를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고객사 목록 조회 성공")
    })
    public ResponseEntity<List<CustomerDto>> getAllActiveCustomers() {
        log.debug("활성 고객사 목록 조회 요청");
        
        List<Customer> customers = customerService.findActiveCustomers();
        List<CustomerDto> dtos = customers.stream()
            .map(this::convertToDto)
            .toList();
        
        log.debug("활성 고객사 목록 조회 완료: {}명", dtos.size());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/auth/{token}")
    @Operation(summary = "토큰으로 고객사 인증", description = "토큰을 사용하여 고객사를 인증합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증 성공"),
        @ApiResponse(responseCode = "404", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<CustomerDto> authenticateCustomer(
        @Parameter(description = "인증 토큰", required = true)
        @PathVariable String token) {
        
        log.debug("토큰 인증 요청: {}", token);
        
        Optional<Customer> customerOpt = customerService.findByToken(token);
        if (customerOpt.isEmpty()) {
            log.warn("토큰 인증 실패: 유효하지 않은 토큰 - {}", token);
            return ResponseEntity.notFound().build();
        }
        
        CustomerDto dto = convertToDto(customerOpt.get());
        log.debug("토큰 인증 성공: {}", dto.id());
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Customer 엔티티를 CustomerDto로 변환하는 공통 메서드
     */
    private CustomerDto convertToDto(Customer customer) {
        return new CustomerDto(
            customer.getId(),
            customer.getName(),
            customer.getToken(),
            customer.isActive()
        );
    }
}
