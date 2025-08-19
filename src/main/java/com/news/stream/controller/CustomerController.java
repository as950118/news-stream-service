package com.news.stream.controller;

import com.news.stream.dto.CustomerDto;
import com.news.stream.dto.CreateCustomerRequest;
import com.news.stream.model.Customer;
import com.news.stream.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request.name());
        CustomerDto dto = new CustomerDto(
            customer.getId(),
            customer.getName(),
            customer.getToken(),
            customer.isActive()
        );
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable String id) {
        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            CustomerDto dto = new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getToken(),
                customer.isActive()
            );
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllActiveCustomers() {
        List<Customer> customers = customerService.findActiveCustomers();
        List<CustomerDto> dtos = customers.stream()
            .map(customer -> new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getToken(),
                customer.isActive()
            ))
            .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/auth/{token}")
    public ResponseEntity<CustomerDto> authenticateCustomer(@PathVariable String token) {
        Optional<Customer> customerOpt = customerService.findByToken(token);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            CustomerDto dto = new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getToken(),
                customer.isActive()
            );
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }
}
