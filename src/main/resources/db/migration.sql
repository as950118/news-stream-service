-- NEWS_PROCESSING_STATUS 테이블 마이그레이션
-- 기존 테이블에 새로운 컬럼들을 추가

-- failure_reason 컬럼 추가
ALTER TABLE NEWS_PROCESSING_STATUS 
ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(100);

-- affected_customers 컬럼 추가 (JSON 형태로 고객사별 실패 정보 저장)
ALTER TABLE NEWS_PROCESSING_STATUS 
ADD COLUMN IF NOT EXISTS affected_customers TEXT;

-- failed_customer_count 컬럼 추가
ALTER TABLE NEWS_PROCESSING_STATUS 
ADD COLUMN IF NOT EXISTS failed_customer_count INTEGER DEFAULT 0;

-- total_customer_count 컬럼 추가
ALTER TABLE NEWS_PROCESSING_STATUS 
ADD COLUMN IF NOT EXISTS total_customer_count INTEGER DEFAULT 0;

-- last_failure_at 컬럼 추가
ALTER TABLE NEWS_PROCESSING_STATUS 
ADD COLUMN IF NOT EXISTS last_failure_at TIMESTAMP;

-- 새로운 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_news_processing_failure_reason ON NEWS_PROCESSING_STATUS(failure_reason);
CREATE INDEX IF NOT EXISTS idx_news_processing_last_failure ON NEWS_PROCESSING_STATUS(last_failure_at);
CREATE INDEX IF NOT EXISTS idx_news_processing_failed_customers ON NEWS_PROCESSING_STATUS(failed_customer_count);

-- 기존 데이터에 대한 기본값 설정
UPDATE NEWS_PROCESSING_STATUS 
SET failure_reason = 'UNKNOWN_ERROR' 
WHERE failure_reason IS NULL;

UPDATE NEWS_PROCESSING_STATUS 
SET failed_customer_count = 0 
WHERE failed_customer_count IS NULL;

UPDATE NEWS_PROCESSING_STATUS 
SET total_customer_count = 0 
WHERE total_customer_count IS NULL;
