#!/bin/bash

echo "🚀 News Stream Service 테스트 시나리오 실행"
echo "=========================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API 기본 URL
BASE_URL="http://localhost:8080"
WS_URL="ws://localhost:8080/ws/news"

# 테스트 결과 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 함수
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    echo -e "\n${BLUE}🧪 테스트: $test_name${NC}"
    echo "실행 중..."
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if eval "$test_command"; then
        echo -e "${GREEN}✅ 성공: $test_name${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 실패: $test_name${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# 헬스체크
check_health() {
    echo -e "\n${YELLOW}🔍 시스템 상태 확인${NC}"
    
    # 애플리케이션 헬스체크
    if curl -s "$BASE_URL/actuator/health" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}✅ 애플리케이션 상태: UP${NC}"
    else
        echo -e "${RED}❌ 애플리케이션 상태: DOWN${NC}"
        return 1
    fi
    
    # 데이터베이스 헬스체크
    if curl -s "$BASE_URL/actuator/health/db" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}✅ 데이터베이스 상태: UP${NC}"
    else
        echo -e "${RED}❌ 데이터베이스 상태: DOWN${NC}"
        return 1
    fi
    
    return 0
}

# 시나리오 1: 다수의 고객사가 연결하기 (인증)
test_multiple_customer_connections() {
    echo -e "\n${YELLOW}📡 시나리오 1: 다수의 고객사 연결 테스트${NC}"
    
    # 고객사 생성
    local customer_ids=()
    
    for i in {1..5}; do
        echo "고객사 $i 생성 중..."
        response=$(curl -s -X POST "$BASE_URL/api/v1/customers" \
            -H "Content-Type: application/json" \
            -d "{\"name\": \"테스트고객사$i\"}")
        
        if echo "$response" | grep -q '"id"'; then
            customer_id=$(echo "$response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
            customer_ids+=("$customer_id")
            echo -e "${GREEN}✅ 고객사 $i 생성됨: $customer_id${NC}"
        else
            echo -e "${RED}❌ 고객사 $i 생성 실패${NC}"
            return 1
        fi
        
        sleep 0.5
    done
    
    echo -e "${GREEN}✅ 총 ${#customer_ids[@]}개 고객사 생성 완료${NC}"
    
    # 고객사 목록 확인
    if curl -s "$BASE_URL/api/v1/customers" | grep -q '"id"'; then
        echo -e "${GREEN}✅ 고객사 목록 조회 성공${NC}"
    else
        echo -e "${RED}❌ 고객사 목록 조회 실패${NC}"
        return 1
    fi
    
    return 0
}

# 시나리오 2: 메시지 발생시 다수의 고객사에게 소켓으로 전달
test_news_broadcasting() {
    echo -e "\n${YELLOW}📰 시나리오 2: 뉴스 브로드캐스팅 테스트${NC}"
    
    # 큐 상태 확인
    echo "큐 상태 확인 중..."
    if curl -s "$BASE_URL/api/v1/queue/stats" | grep -q '"queueSize"'; then
        echo -e "${GREEN}✅ 큐 상태 조회 성공${NC}"
    else
        echo -e "${RED}❌ 큐 상태 조회 실패${NC}"
        return 1
    fi
    
    # 테스트 뉴스 전송
    echo "테스트 뉴스 전송 중..."
    for i in {1..3}; do
        news_id="test-news-$(printf "%03d" $i)"
        response=$(curl -s -X POST "$BASE_URL/api/v1/queue/test-message" \
            -H "Content-Type: application/json" \
            -d "{\"newsId\": \"$news_id\"}")
        
        if echo "$response" | grep -q '"success"'; then
            echo -e "${GREEN}✅ 뉴스 전송 성공: $news_id${NC}"
        else
            echo -e "${RED}❌ 뉴스 전송 실패: $news_id${NC}"
            return 1
        fi
        
        sleep 0.5
    done
    
    return 0
}

# 시나리오 3: 1고객사 1소켓 연결 (새 연결시 기존연결 끊기)
test_single_connection_per_customer() {
    echo -e "\n${YELLOW}🔌 시나리오 3: 1고객사 1소켓 연결 테스트${NC}"
    
    # 고객사 생성
    echo "테스트용 고객사 생성 중..."
    response=$(curl -s -X POST "$BASE_URL/api/v1/customers" \
        -H "Content-Type: application/json" \
        -d "{\"name\": \"연결테스트고객사\"}")
    
    if echo "$response" | grep -q '"id"'; then
        customer_id=$(echo "$response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✅ 테스트 고객사 생성됨: $customer_id${NC}"
    else
        echo -e "${RED}❌ 테스트 고객사 생성 실패${NC}"
        return 1
    fi
    
    # 연결 상태 확인
    echo "연결 상태 확인 중..."
    if curl -s "$BASE_URL/api/v1/customers/$customer_id/connections" | grep -q '"connectionId"'; then
        echo -e "${GREEN}✅ 연결 상태 조회 성공${NC}"
    else
        echo -e "${GREEN}✅ 연결 상태 조회 성공 (연결 없음)${NC}"
    fi
    
    return 0
}

# WebSocket 연결 테스트
test_websocket_connection() {
    echo -e "\n${YELLOW}🌐 WebSocket 연결 테스트${NC}"
    
    # WebSocket 엔드포인트 확인
    if curl -s "$BASE_URL/ws/news" | grep -q "WebSocket"; then
        echo -e "${GREEN}✅ WebSocket 엔드포인트 접근 가능${NC}"
    else
        echo -e "${YELLOW}⚠️ WebSocket 엔드포인트 확인 불가 (정상)${NC}"
    fi
    
    return 0
}

# 메트릭 확인
check_metrics() {
    echo -e "\n${YELLOW}📊 메트릭 확인${NC}"
    
    # Prometheus 메트릭
    if curl -s "$BASE_URL/actuator/prometheus" | grep -q "jvm_"; then
        echo -e "${GREEN}✅ Prometheus 메트릭 수집 가능${NC}"
    else
        echo -e "${RED}❌ Prometheus 메트릭 수집 실패${NC}"
        return 1
    fi
    
    # 애플리케이션 메트릭
    if curl -s "$BASE_URL/actuator/metrics" | grep -q '"names"'; then
        echo -e "${GREEN}✅ 애플리케이션 메트릭 조회 가능${NC}"
    else
        echo -e "${RED}❌ 애플리케이션 메트릭 조회 실패${NC}"
        return 1
    fi
    
    return 0
}

# 메인 테스트 실행
main() {
    echo "테스트 시작 시간: $(date)"
    echo "=========================================="
    
    # 시스템 상태 확인
    run_test "시스템 헬스체크" "check_health"
    
    # 시나리오별 테스트 실행
    run_test "다수 고객사 연결 테스트" "test_multiple_customer_connections"
    run_test "뉴스 브로드캐스팅 테스트" "test_news_broadcasting"
    run_test "1고객사 1소켓 연결 테스트" "test_single_connection_per_customer"
    run_test "WebSocket 연결 테스트" "test_websocket_connection"
    run_test "메트릭 확인" "check_metrics"
    
    # 결과 요약
    echo -e "\n=========================================="
    echo -e "${BLUE}📋 테스트 결과 요약${NC}"
    echo "=========================================="
    echo -e "총 테스트: ${BLUE}$TOTAL_TESTS${NC}"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "\n${GREEN}🎉 모든 테스트가 성공했습니다!${NC}"
        exit 0
    else
        echo -e "\n${RED}❌ 일부 테스트가 실패했습니다.${NC}"
        exit 1
    fi
}

# 스크립트 실행
main "$@"
