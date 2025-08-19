package com.news.stream.service;

import com.news.stream.util.Profiled;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 메서드 성능 프로파일링을 위한 AOP 컴포넌트
 * @Profiled 어노테이션이 적용된 메서드의 실행 시간과 성공/실패 여부를 모니터링합니다.
 */
@Component
@Aspect
public class PerformanceProfiler {
    
    private final Logger logger = LoggerFactory.getLogger(PerformanceProfiler.class);
    private final MeterRegistry meterRegistry;
    
    public PerformanceProfiler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * 일반 메서드 성능 프로파일링
     */
    @Around("@annotation(profiled)")
    public Object profileMethod(ProceedingJoinPoint joinPoint, Profiled profiled) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String key = className + "." + methodName;
        
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 성공 메트릭 기록
            sample.stop(Timer.builder("method.execution.success")
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry));
            
            // 실행 시간 로깅
            if (executionTime > 1000) {
                logger.warn("성능 경고: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
            } else if (executionTime > 500) {
                logger.info("성능 정보: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 실패 메트릭 기록
            sample.stop(Timer.builder("method.execution.failure")
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry));
            
            logger.error("성능 오류: {}.{} 실행 시간 {}ms, 오류: {}", 
                className, methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * 데이터베이스 작업 성능 프로파일링
     */
    @Around("@annotation(profiled) && profiled.database()")
    public Object profileDatabaseOperation(ProceedingJoinPoint joinPoint, Profiled profiled) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 데이터베이스 작업 메트릭 기록
            sample.stop(Timer.builder("database.operation")
                .tag("class", className)
                .tag("method", methodName)
                .tag("status", "success")
                .register(meterRegistry));
            
            // 데이터베이스 성능 로깅
            if (executionTime > 100) {
                logger.warn("DB 성능 경고: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            sample.stop(Timer.builder("database.operation")
                .tag("class", className)
                .tag("method", methodName)
                .tag("status", "failure")
                .register(meterRegistry));
            
            logger.error("DB 성능 오류: {}.{} 실행 시간 {}ms, 오류: {}", 
                className, methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
}
