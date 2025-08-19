-- PostgreSQL 초기화 스크립트
-- 데이터베이스 스키마 생성

-- 기존 테이블이 있다면 삭제 (개발 환경용)
DROP TABLE IF EXISTS NEWS_PROCESSING_STATUS CASCADE;
DROP TABLE IF EXISTS CUSTOMERS CASCADE;
DROP TABLE IF EXISTS TRANSLATED_NEWS CASCADE;

-- TRANSLATED_NEWS 테이블
CREATE TABLE IF NOT EXISTS TRANSLATED_NEWS (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CUSTOMERS 테이블
CREATE TABLE IF NOT EXISTS CUSTOMERS (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    token VARCHAR(500) UNIQUE NOT NULL,
    connection_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- NEWS_PROCESSING_STATUS 테이블
CREATE TABLE IF NOT EXISTS NEWS_PROCESSING_STATUS (
    news_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    failure_reason VARCHAR(100),
    affected_customers TEXT,
    failed_customer_count INTEGER DEFAULT 0,
    total_customer_count INTEGER DEFAULT 0,
    last_failure_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_translated_news_published_at ON TRANSLATED_NEWS(published_at);
CREATE INDEX IF NOT EXISTS idx_translated_news_created_at ON TRANSLATED_NEWS(created_at);
CREATE INDEX IF NOT EXISTS idx_customers_token ON CUSTOMERS(token);
CREATE INDEX IF NOT EXISTS idx_customers_connection_id ON CUSTOMERS(connection_id);
CREATE INDEX IF NOT EXISTS idx_news_processing_status ON NEWS_PROCESSING_STATUS(status);
CREATE INDEX IF NOT EXISTS idx_news_processing_created_at ON NEWS_PROCESSING_STATUS(created_at);
CREATE INDEX IF NOT EXISTS idx_news_processing_updated_at ON NEWS_PROCESSING_STATUS(updated_at);

-- 스키마 생성 완료 로그
DO $$
BEGIN
    RAISE NOTICE 'Database schema initialization completed successfully';
END $$;
