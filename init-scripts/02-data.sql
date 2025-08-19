-- 테스트용 고객사 데이터
INSERT INTO CUSTOMERS (id, name, token, is_active, created_at, updated_at) 
VALUES 
    ('customer-001', 'Test Customer 1', 'test-token-001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('customer-002', 'Test Customer 2', 'test-token-002', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 테스트용 뉴스 데이터
INSERT INTO TRANSLATED_NEWS (id, title, content, published_at, created_at, updated_at)
VALUES 
    ('news-001', '테스트 뉴스 제목 1', '테스트 뉴스 내용 1입니다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('news-002', '테스트 뉴스 제목 2', '테스트 뉴스 내용 2입니다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
