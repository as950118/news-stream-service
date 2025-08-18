package com.news.stream.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * LinkedBlockingQueue 기반 메시지 큐 구현체
 * 내부 큐로 사용되며, 향후 AWS SQS 등으로 전환할 수 있습니다.
 */
@Component
public class LinkedBlockingMessageQueue implements MessageQueue<NewsMessage> {
    
    private final LinkedBlockingQueue<NewsMessage> queue;
    private final int capacity;
    
    /**
     * 생성자
     * 
     * @param capacity 큐 용량 (기본값: 1000)
     */
    public LinkedBlockingMessageQueue(@Value("${queue.capacity:1000}") int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    @Override
    public void enqueue(NewsMessage message) throws InterruptedException {
        if (message == null) {
            throw new IllegalArgumentException("메시지는 null일 수 없습니다");
        }
        queue.put(message);
    }
    
    @Override
    public NewsMessage dequeue() throws InterruptedException {
        return queue.take();
    }
    
    @Override
    public NewsMessage dequeue(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }
    
    @Override
    public int size() {
        return queue.size();
    }
    
    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    @Override
    public void clear() {
        queue.clear();
    }
    
    /**
     * 큐의 전체 용량을 반환합니다.
     * 
     * @return 큐 용량
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * 큐의 남은 용량을 반환합니다.
     * 
     * @return 남은 용량
     */
    public int getRemainingCapacity() {
        return queue.remainingCapacity();
    }
    
    /**
     * 큐가 가득 찼는지 확인합니다.
     * 
     * @return 큐가 가득 찼으면 true
     */
    public boolean isFull() {
        return queue.remainingCapacity() == 0;
    }
}
