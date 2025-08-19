package com.news.stream.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 성능 프로파일링을 위한 어노테이션
 * 이 어노테이션이 적용된 메서드는 실행 시간과 성공/실패 여부를 모니터링합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Profiled {
    /**
     * 프로파일링 식별자 (기본값: 메서드명)
     */
    String value() default "";
    
    /**
     * 데이터베이스 작업 여부 (기본값: false)
     * true인 경우 데이터베이스 성능 메트릭을 별도로 수집합니다.
     */
    boolean database() default false;
}
