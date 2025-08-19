#!/bin/bash

echo "=== News Stream Service 테스트 실행 ==="

# 1. 단위 테스트 실행
echo "1. 단위 테스트 실행"
./gradlew test

# 2. 통합 테스트 실행
echo -e "\n2. 통합 테스트 실행"
./gradlew integrationTest

# 3. 테스트 커버리지 리포트 생성
echo -e "\n3. 테스트 커버리지 리포트 생성"
./gradlew jacocoTestReport

# 4. 테스트 커버리지 검증
echo -e "\n4. 테스트 커버리지 검증"
./gradlew jacocoTestCoverageVerification

# 5. 테스트 결과 요약
echo -e "\n=== 테스트 결과 요약 ==="
echo "테스트 커버리지 리포트: build/reports/jacoco/test/html/index.html"
echo "테스트 리포트: build/reports/tests/test/index.html"

echo -e "\n=== 테스트 실행 완료 ==="
