package com.news.stream.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 설정 클래스
 * Swagger UI 및 API 문서 생성을 위한 설정
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("News Stream Service API")
                .version("1.0.0")
                .description("실시간 뉴스 전송 서비스 API 문서")
                .contact(new Contact()
                    .name("Development Team")
                    .email("dev@example.com")
                )
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")
                )
            )
            .addServersItem(new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 환경")
            )
            .addServersItem(new Server()
                .url("https://api.example.com")
                .description("운영 환경")
            )
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 입력하세요")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
