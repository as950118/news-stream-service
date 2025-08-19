-- 데이터베이스 인덱스 최적화 스크립트
-- 이 스크립트는 뉴스 스트림 서비스의 성능을 향상시키기 위한 인덱스를 생성합니다.

-- 기존 인덱스 확인
SELECT schemaname, tablename, indexname, indexdef 
FROM pg_indexes 
WHERE schemaname = 'public' 
ORDER BY tablename, indexname;

-- 뉴스 테이블 인덱스 최적화
-- 1. 복합 인덱스 (published_at + created_at)
CREATE INDEX IF NOT EXISTS idx_translated_news_published_created 
ON TRANSLATED_NEWS (published_at DESC, created_at DESC);

-- 2. 제목 검색을 위한 GIN 인덱스 (한글 검색 최적화 - 한국어 설정이 없으면 영어 사용)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'korean') THEN
        CREATE INDEX IF NOT EXISTS idx_translated_news_title_gin 
        ON TRANSLATED_NEWS USING GIN (to_tsvector('korean', title));
    ELSE
        CREATE INDEX IF NOT EXISTS idx_translated_news_title_gin 
        ON TRANSLATED_NEWS USING GIN (to_tsvector('english', title));
    END IF;
END $$;

-- 3. 내용 검색을 위한 GIN 인덱스
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'korean') THEN
        CREATE INDEX IF NOT EXISTS idx_translated_news_content_gin 
        ON TRANSLATED_NEWS USING GIN (to_tsvector('korean', content));
    ELSE
        CREATE INDEX IF NOT EXISTS idx_translated_news_content_gin 
        ON TRANSLATED_NEWS USING GIN (to_tsvector('english', content));
    END IF;
END $$;

-- 4. 부분 인덱스 (활성 뉴스만) - CURRENT_DATE 대신 고정 날짜 사용
CREATE INDEX IF NOT EXISTS idx_translated_news_active 
ON TRANSLATED_NEWS (published_at DESC) 
WHERE published_at >= '2024-01-01'::date;

-- 고객사 테이블 인덱스 최적화
-- 1. 토큰 검색 최적화
CREATE INDEX IF NOT EXISTS idx_customers_token_hash 
ON CUSTOMERS USING HASH (token);

-- 2. 연결 ID 검색 최적화
CREATE INDEX IF NOT EXISTS idx_customers_connection_id 
ON CUSTOMERS (connection_id) 
WHERE connection_id IS NOT NULL;

-- 3. 활성 고객사 검색 최적화
CREATE INDEX IF NOT EXISTS idx_customers_active 
ON CUSTOMERS (is_active, created_at DESC) 
WHERE is_active = true;

-- 뉴스 처리 상태 테이블 인덱스
CREATE INDEX IF NOT EXISTS idx_news_processing_status_status 
ON NEWS_PROCESSING_STATUS (status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_news_processing_status_retry 
ON NEWS_PROCESSING_STATUS (retry_count, status) 
WHERE status IN ('FAILED', 'RETRY');

-- 통계 정보 업데이트
ANALYZE TRANSLATED_NEWS;
ANALYZE CUSTOMERS;
ANALYZE NEWS_PROCESSING_STATUS;

-- 인덱스 사용 통계 확인 (PostgreSQL 15 호환)
SELECT schemaname, relname as tablename, indexrelname as indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
