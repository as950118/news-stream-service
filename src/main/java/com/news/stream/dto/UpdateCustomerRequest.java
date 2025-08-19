package com.news.stream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 고객사 수정 요청을 위한 DTO 클래스
 */
public record UpdateCustomerRequest(
    @NotBlank(message = "고객사명은 필수입니다")
    String name,
    
    @NotNull(message = "활성 상태는 필수입니다")
    Boolean isActive
) {}
