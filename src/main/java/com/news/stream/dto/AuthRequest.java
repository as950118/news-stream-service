package com.news.stream.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "고객사명은 필수입니다")
    String name,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {}
