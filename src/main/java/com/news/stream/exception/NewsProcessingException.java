package com.news.stream.exception;

/**
 * 뉴스 처리 중 발생하는 예외
 */
public class NewsProcessingException extends RuntimeException {
    
    public NewsProcessingException(String message) {
        super(message);
    }
    
    public NewsProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
