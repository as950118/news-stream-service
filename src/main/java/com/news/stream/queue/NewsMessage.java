package com.news.stream.queue;

import java.time.LocalDateTime;

/**
 * 뉴스 메시지 클래스
 * 큐를 통해 전달되는 뉴스 관련 메시지 정보를 담습니다.
 * 
 * @param newsId 뉴스 ID
 * @param timestamp 메시지 생성 시각
 * @param type 메시지 타입
 */
public record NewsMessage(
    String newsId,
    LocalDateTime timestamp,
    MessageType type
) {
    
    /**
     * 메시지 타입 열거형
     */
    public enum MessageType {
        /** 뉴스 생성 */
        NEWS_CREATED,
        /** 뉴스 수정 */
        NEWS_UPDATED,
        /** 뉴스 삭제 */
        NEWS_DELETED
    }
    
    /**
     * 기본 생성자
     */
    public NewsMessage {
        if (newsId == null || newsId.trim().isEmpty()) {
            throw new IllegalArgumentException("뉴스 ID는 null이거나 빈 문자열일 수 없습니다");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("타임스탬프는 null일 수 없습니다");
        }
        if (type == null) {
            throw new IllegalArgumentException("메시지 타입은 null일 수 없습니다");
        }
    }
    
    /**
     * 현재 시각으로 뉴스 생성 메시지를 생성합니다.
     * 
     * @param newsId 뉴스 ID
     * @return 뉴스 생성 메시지
     */
    public static NewsMessage newsCreated(String newsId) {
        return new NewsMessage(newsId, LocalDateTime.now(), MessageType.NEWS_CREATED);
    }
    
    /**
     * 현재 시각으로 뉴스 수정 메시지를 생성합니다.
     * 
     * @param newsId 뉴스 ID
     * @return 뉴스 수정 메시지
     */
    public static NewsMessage newsUpdated(String newsId) {
        return new NewsMessage(newsId, LocalDateTime.now(), MessageType.NEWS_UPDATED);
    }
    
    /**
     * 현재 시각으로 뉴스 삭제 메시지를 생성합니다.
     * 
     * @param newsId 뉴스 ID
     * @return 뉴스 삭제 메시지
     */
    public static NewsMessage newsDeleted(String newsId) {
        return new NewsMessage(newsId, LocalDateTime.now(), MessageType.NEWS_DELETED);
    }
}
