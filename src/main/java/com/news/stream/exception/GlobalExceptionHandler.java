package com.news.stream.exception;

import com.news.stream.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
        AuthenticationException e, HttpServletRequest request) {
        
        ApiErrorResponse response = new ApiErrorResponse(
            "AUTHENTICATION_ERROR",
            e.getMessage(),
            "인증에 실패했습니다. 유효한 JWT 토큰을 확인해주세요.",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(MessageDeliveryException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageDeliveryException(
        MessageDeliveryException e, HttpServletRequest request) {
        
        ApiErrorResponse response = new ApiErrorResponse(
            "WEBSOCKET_ERROR",
            e.getMessage(),
            "WebSocket 메시지 전송에 실패했습니다.",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
        MethodArgumentNotValidException e, HttpServletRequest request) {
        
        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ApiErrorResponse response = new ApiErrorResponse(
            "VALIDATION_ERROR",
            "입력값 검증 실패",
            String.join(", ", errors),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
        ConstraintViolationException e, HttpServletRequest request) {
        
        List<String> errors = e.getConstraintViolations()
            .stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());
        
        ApiErrorResponse response = new ApiErrorResponse(
            "VALIDATION_ERROR",
            "제약 조건 위반",
            String.join(", ", errors),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
        BindException e, HttpServletRequest request) {
        
        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ApiErrorResponse response = new ApiErrorResponse(
            "VALIDATION_ERROR",
            "바인딩 오류",
            String.join(", ", errors),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e, HttpServletRequest request) {
        
        ApiErrorResponse response = new ApiErrorResponse(
            "INVALID_REQUEST",
            "잘못된 요청 형식",
            e.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
        Exception e, HttpServletRequest request) {
        
        ApiErrorResponse response = new ApiErrorResponse(
            "INTERNAL_ERROR",
            "내부 서버 오류",
            "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    // 기존 ErrorResponse는 하위 호환성을 위해 유지
    public record ErrorResponse(
        String errorCode,
        String message,
        LocalDateTime timestamp
    ) {}
}
