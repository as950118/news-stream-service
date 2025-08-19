package com.news.stream;

import com.news.stream.model.Customer;
import com.news.stream.model.TranslatedNews;
import com.news.stream.queue.NewsMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 테스트 데이터 생성을 위한 유틸리티 클래스
 */
public class TestDataBuilder {
    
    /**
     * TranslatedNews 테스트 데이터 생성
     */
    public static TranslatedNews createTranslatedNews(String id) {
        TranslatedNews news = new TranslatedNews();
        String newsId = id != null ? id : "test-news-" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        
        news.setId(newsId);
        news.setTitle("테스트 뉴스 제목 - " + newsId);
        news.setContent("테스트 뉴스 내용입니다. ID: " + newsId);
        news.setPublishedAt(now);
        news.setCreatedAt(now);
        news.setUpdatedAt(now);
        
        return news;
    }
    
    /**
     * Customer 테스트 데이터 생성
     */
    public static Customer createCustomer(String id) {
        Customer customer = new Customer();
        String customerId = id != null ? id : "customer-" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        
        customer.setId(customerId);
        customer.setName("테스트 고객사 - " + customerId);
        customer.setToken("test-token-" + customerId);
        customer.setActive(true);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        
        return customer;
    }
    
    /**
     * NewsMessage 테스트 데이터 생성
     */
    public static NewsMessage createNewsMessage(String newsId, NewsMessage.MessageType type) {
        String messageId = newsId != null ? newsId : "test-news-" + System.currentTimeMillis();
        NewsMessage.MessageType messageType = type != null ? type : NewsMessage.MessageType.NEWS_CREATED;
        
        return new NewsMessage(messageId, LocalDateTime.now(), messageType);
    }
    
    /**
     * 뉴스 목록 테스트 데이터 생성
     */
    public static List<TranslatedNews> createNewsList(int count) {
        List<TranslatedNews> newsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            newsList.add(createTranslatedNews("test-news-" + String.format("%03d", i)));
        }
        return newsList;
    }
    
    /**
     * 고객사 목록 테스트 데이터 생성
     */
    public static List<Customer> createCustomerList(int count) {
        List<Customer> customerList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            customerList.add(createCustomer("customer-" + String.format("%03d", i)));
        }
        return customerList;
    }
}
