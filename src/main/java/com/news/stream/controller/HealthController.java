package com.news.stream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 컨트롤러
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "헬스체크 API")
public class HealthController {

    @GetMapping
    @Operation(summary = "헬스체크", description = "서비스 상태를 확인합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작")
    })
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("헬스체크 요청");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "News Stream Service");
        response.put("version", "1.0.0");
        
        log.debug("헬스체크 응답: {}", response);
        return ResponseEntity.ok(response);
    }
}
