package com.news.stream.queue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 큐 메트릭 클래스
 * 큐 성능과 상태를 모니터링하기 위한 메트릭을 제공합니다.
 */
@Component
public class QueueMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter messagesEnqueued;
    private final Counter messagesDequeued;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;
    private final Timer messageProcessingTime;
    private final Timer messageEnqueueTime;
    private final Timer messageDequeueTime;
    
    public QueueMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesEnqueued = Counter.builder("queue.messages.enqueued")
                .description("큐에 추가된 메시지 수")
                .register(meterRegistry);
        
        this.messagesDequeued = Counter.builder("queue.messages.dequeued")
                .description("큐에서 가져온 메시지 수")
                .register(meterRegistry);
        
        this.messagesProcessed = Counter.builder("queue.messages.processed")
                .description("성공적으로 처리된 메시지 수")
                .register(meterRegistry);
        
        this.messagesFailed = Counter.builder("queue.messages.failed")
                .description("처리 실패한 메시지 수")
                .register(meterRegistry);
        
        this.messageProcessingTime = Timer.builder("queue.message.processing.time")
                .description("메시지 처리 시간")
                .register(meterRegistry);
        
        this.messageEnqueueTime = Timer.builder("queue.message.enqueue.time")
                .description("메시지 큐잉 시간")
                .register(meterRegistry);
        
        this.messageDequeueTime = Timer.builder("queue.message.dequeue.time")
                .description("메시지 디큐잉 시간")
                .register(meterRegistry);
    }
    
    /**
     * 메시지 큐잉 메트릭을 기록합니다.
     */
    public void recordMessageEnqueued() {
        messagesEnqueued.increment();
    }
    
    /**
     * 메시지 디큐잉 메트릭을 기록합니다.
     */
    public void recordMessageDequeued() {
        messagesDequeued.increment();
    }
    
    /**
     * 메시지 처리 성공 메트릭을 기록합니다.
     */
    public void recordMessageProcessed() {
        messagesProcessed.increment();
    }
    
    /**
     * 메시지 처리 실패 메트릭을 기록합니다.
     */
    public void recordMessageFailed() {
        messagesFailed.increment();
    }
    
    /**
     * 메시지 처리 시간을 기록합니다.
     */
    public void recordProcessingTime(long timeInMillis) {
        messageProcessingTime.record(timeInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 메시지 큐잉 시간을 기록합니다.
     */
    public void recordEnqueueTime(long timeInMillis) {
        messageEnqueueTime.record(timeInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 메시지 디큐잉 시간을 기록합니다.
     */
    public void recordDequeueTime(long timeInMillis) {
        messageDequeueTime.record(timeInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * AWS SQS 전용 메트릭을 기록합니다.
     */
    public void recordSqsMetrics(String operation, long timeInMillis, boolean success) {
        if (success) {
            recordMessageProcessed();
        } else {
            recordMessageFailed();
        }
        
        // SQS 작업별 시간 기록
        Timer.builder("queue.sqs." + operation + ".time")
                .description("SQS " + operation + " 작업 시간")
                .register(meterRegistry)
                .record(timeInMillis, TimeUnit.MILLISECONDS);
    }
}
