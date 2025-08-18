package com.news.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * News Stream Service 메인 애플리케이션 클래스
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class NewsStreamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsStreamServiceApplication.class, args);
    }
}
