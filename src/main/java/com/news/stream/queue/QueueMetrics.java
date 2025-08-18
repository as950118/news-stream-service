package com.news.stream.queue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 큐 메트릭 수집 클래스
 * Micrometer를 사용하여 큐 관련 메트릭을 수집합니다.
 */
@Component
public class QueueMetrics {
    
    private final MessageQueue<NewsMessage> messageQueue;
    private final MeterRegistry meterRegistry;
    
    private final Counter messagesProcessedCounter;
    private final Timer processingTimeTimer;
    
    /**
     * 생성자
     * 
     * @param messageQueue 메시지 큐
     * @param meterRegistry 메트릭 레지스트리
     */
    public QueueMetrics(MessageQueue<NewsMessage> messageQueue, 
                       MeterRegistry meterRegistry) {
        this.messageQueue = messageQueue;
        this.meterRegistry = meterRegistry;
        
        // 메트릭 초기화
        this.messagesProcessedCounter = Counter.builder("queue.messages.processed")
            .description("처리된 메시지 수")
            .register(meterRegistry);
        
        this.processingTimeTimer = Timer.builder("queue.processing.time")
            .description("메시지 처리 시간")
            .register(meterRegistry);
        
        initializeMetrics();
    }
    
    /**
     * 메트릭을 초기화합니다.
     */
    private void initializeMetrics() {
        // 큐 크기 게이지
        Gauge.builder("queue.size", messageQueue, MessageQueue::size)
            .description("현재 큐에 있는 메시지 수")
            .register(meterRegistry);
        
        // 큐 용량 게이지
        if (messageQueue instanceof LinkedBlockingMessageQueue) {
            LinkedBlockingMessageQueue linkedQueue = (LinkedBlockingMessageQueue) messageQueue;
            Gauge.builder("queue.capacity", linkedQueue, LinkedBlockingMessageQueue::getCapacity)
                .description("큐의 전체 용량")
                .register(meterRegistry);
            
            Gauge.builder("queue.remaining.capacity", linkedQueue, LinkedBlockingMessageQueue::getRemainingCapacity)
                .description("큐의 남은 용량")
                .register(meterRegistry);
        }
        
        // 큐 사용률 게이지
        if (messageQueue instanceof LinkedBlockingMessageQueue) {
            LinkedBlockingMessageQueue linkedQueue = (LinkedBlockingMessageQueue) messageQueue;
            Gauge.builder("queue.utilization", linkedQueue, this::calculateUtilization)
                .description("큐 사용률 (0.0 ~ 1.0)")
                .register(meterRegistry);
        }
    }
    
    /**
     * 큐 사용률을 계산합니다.
     * 
     * @param queue 큐
     * @return 사용률 (0.0 ~ 1.0)
     */
    private double calculateUtilization(LinkedBlockingMessageQueue queue) {
        int capacity = queue.getCapacity();
        if (capacity == 0) return 0.0;
        
        int used = capacity - queue.getRemainingCapacity();
        return (double) used / capacity;
    }
    
    /**
     * 메시지 처리 완료를 기록합니다.
     */
    public void recordMessageProcessed() {
        messagesProcessedCounter.increment();
    }
    
    /**
     * 메시지 처리 시간을 기록합니다.
     * 
     * @param timeInMs 처리 시간 (밀리초)
     */
    public void recordProcessingTime(long timeInMs) {
        processingTimeTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 메시지 처리 시간을 기록합니다.
     * 
     * @param time 처리 시간
     * @param unit 시간 단위
     */
    public void recordProcessingTime(long time, TimeUnit unit) {
        processingTimeTimer.record(time, unit);
    }
    
    /**
     * 메시지 처리 시간을 측정하는 Timer.Sample을 반환합니다.
     * 
     * @return Timer.Sample
     */
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 메시지 처리 시간 측정을 완료합니다.
     * 
     * @param sample Timer.Sample
     */
    public void stopProcessingTimer(Timer.Sample sample) {
        sample.stop(processingTimeTimer);
    }
}
