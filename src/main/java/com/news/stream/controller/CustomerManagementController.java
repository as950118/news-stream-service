package com.news.stream.controller;

import com.news.stream.dto.CustomerDto;
import com.news.stream.dto.CreateCustomerRequest;
import com.news.stream.dto.UpdateCustomerRequest;
import com.news.stream.model.Customer;
import com.news.stream.service.CustomerService;
import com.news.stream.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/customers")
@Validated
@Tag(name = "Customer Management", description = "고객사 관리 API")
public class CustomerManagementController {
    
    private final CustomerService customerService;
    private final Logger logger = LoggerFactory.getLogger(CustomerManagementController.class);
    
    public CustomerManagementController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    @PostMapping
    @Operation(summary = "고객사 생성", description = "새로운 고객사를 생성합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "고객사 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<CustomerDto> createCustomer(
        @Parameter(description = "고객사 생성 요청", required = true)
        @Valid @RequestBody CreateCustomerRequest request) {
        
        try {
            Customer customer = customerService.createCustomer(request.name());
            CustomerDto customerDto = convertToDto(customer);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(customerDto);
            
        } catch (Exception e) {
            logger.error("고객사 생성 중 오류 발생", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "고객사 정보 조회", description = "고객사 ID로 고객사 정보를 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고객사 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음")
    })
    public ResponseEntity<CustomerDto> getCustomerById(
        @Parameter(description = "고객사 ID", required = true)
        @PathVariable String id) {
        
        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Customer customer = customerOpt.get();
        CustomerDto customerDto = convertToDto(customer);
        
        return ResponseEntity.ok(customerDto);
    }
    
    @GetMapping
    @Operation(summary = "고객사 목록 조회", description = "모든 고객사 목록을 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고객사 목록 조회 성공")
    })
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        
        try {
            List<Customer> customers = customerService.findActiveCustomers();
            List<CustomerDto> customerDtos = customers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(customerDtos);
        } catch (Exception e) {
            logger.error("고객사 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "고객사 정보 수정", description = "고객사 정보를 수정합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고객사 정보 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<CustomerDto> updateCustomer(
        @Parameter(description = "고객사 ID", required = true)
        @PathVariable String id,
        
        @Parameter(description = "고객사 수정 요청", required = true)
        @Valid @RequestBody UpdateCustomerRequest request) {
        
        try {
            Optional<Customer> customerOpt = customerService.findById(id);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Customer customer = customerOpt.get();
            customer.setName(request.name());
            customer.setActive(request.isActive());
            customer.setUpdatedAt(LocalDateTime.now());
            
            Customer updatedCustomer = customerService.save(customer);
            CustomerDto customerDto = convertToDto(updatedCustomer);
            
            return ResponseEntity.ok(customerDto);
            
        } catch (Exception e) {
            logger.error("고객사 정보 수정 중 오류 발생: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "고객사 삭제", description = "고객사를 삭제합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "고객사 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "고객사를 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteCustomer(
        @Parameter(description = "고객사 ID", required = true)
        @PathVariable String id) {
        
        try {
            Optional<Customer> customerOpt = customerService.findById(id);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            customerService.deleteById(id);
            
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("고객사 삭제 중 오류 발생: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private CustomerDto convertToDto(Customer customer) {
        return new CustomerDto(
            customer.getId(),
            customer.getName(),
            customer.getToken(),
            customer.isActive()
        );
    }
}
