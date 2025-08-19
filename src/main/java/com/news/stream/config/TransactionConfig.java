package com.news.stream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 트랜잭션 관리를 위한 설정 클래스
 * autoCommit 문제를 해결하고 트랜잭션을 안전하게 관리합니다.
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    /**
     * JPA 트랜잭션 매니저 설정
     * autoCommit을 명시적으로 비활성화합니다.
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        
        // 트랜잭션 타임아웃 설정
        transactionManager.setDefaultTimeout(30);
        
        // 롤백 설정
        transactionManager.setRollbackOnCommitFailure(true);
        
        return transactionManager;
    }
}
