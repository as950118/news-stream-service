package com.news.stream.controller;

import com.news.stream.dto.AuthRequest;
import com.news.stream.dto.ConnectionStatusResponse;
import com.news.stream.model.Customer;
import com.news.stream.service.AuthenticationService;
import com.news.stream.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    private final CustomerService customerService;
    
    public AuthenticationController(AuthenticationService authenticationService,
                                   CustomerService customerService) {
        this.authenticationService = authenticationService;
        this.customerService = customerService;
    }
    
    @PostMapping("/customers")
    public ResponseEntity<AuthenticationService.AuthResponse> authenticateCustomer(
        @Valid @RequestBody AuthRequest request) {
        
        AuthenticationService.AuthResponse response = authenticationService.authenticateCustomer(
            request.name(), request.password());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/customers/{id}/connections")
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus(
        @PathVariable String id) {
        
        Optional<Customer> customer = customerService.findById(id);
        if (customer.isEmpty()) {
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
        
        return ResponseEntity.ok(response);
    }
}
