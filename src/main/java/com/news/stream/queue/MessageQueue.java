package com.news.stream.queue;

import java.util.concurrent.TimeUnit;

/**
 * 메시지 큐 인터페이스
 * 향후 AWS SQS 등 외부 메시지 큐로 전환할 수 있도록 인터페이스로 설계
 * 
 * @param <T> 메시지 타입
 */
public interface MessageQueue<T> {
    
    /**
     * 메시지를 큐에 추가합니다.
     * 
     * @param message 추가할 메시지
     * @throws InterruptedException 인터럽트 발생 시
     */
    void enqueue(T message) throws InterruptedException;
    
    /**
     * 큐에서 메시지를 가져옵니다. (블로킹)
     * 
     * @return 큐에서 가져온 메시지
     * @throws InterruptedException 인터럽트 발생 시
     */
    T dequeue() throws InterruptedException;
    
    /**
     * 지정된 시간 동안 큐에서 메시지를 가져옵니다. (타임아웃)
     * 
     * @param timeout 대기 시간
     * @param unit 시간 단위
     * @return 큐에서 가져온 메시지, 타임아웃 시 null
     * @throws InterruptedException 인터럽트 발생 시
     */
    T dequeue(long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * 현재 큐에 있는 메시지 수를 반환합니다.
     * 
     * @return 큐 크기
     */
    int size();
    
    /**
     * 큐가 비어있는지 확인합니다.
     * 
     * @return 큐가 비어있으면 true
     */
    boolean isEmpty();
    
    /**
     * 큐의 모든 메시지를 제거합니다.
     */
    void clear();
}
