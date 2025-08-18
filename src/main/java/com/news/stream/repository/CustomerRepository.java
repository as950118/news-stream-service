package com.news.stream.repository;

import com.news.stream.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    Optional<Customer> findByToken(String token);
    
    Optional<Customer> findByConnectionId(String connectionId);
    
    List<Customer> findByIsActiveTrue();
    
    boolean existsByConnectionId(String connectionId);
}
