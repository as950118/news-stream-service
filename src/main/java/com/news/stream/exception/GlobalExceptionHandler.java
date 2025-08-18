package com.news.stream.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException e) {
        
        ErrorResponse response = new ErrorResponse(
            "AUTHENTICATION_ERROR",
            e.getMessage(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(MessageDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleMessageDeliveryException(
        MessageDeliveryException e) {
        
        ErrorResponse response = new ErrorResponse(
            "WEBSOCKET_ERROR",
            e.getMessage(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        ErrorResponse response = new ErrorResponse(
            "INTERNAL_ERROR",
            "내부 서버 오류가 발생했습니다",
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    public record ErrorResponse(
        String errorCode,
        String message,
        LocalDateTime timestamp
    ) {}
}
