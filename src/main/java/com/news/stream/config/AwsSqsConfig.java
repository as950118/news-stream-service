package com.news.stream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

/**
 * AWS SQS 설정 클래스
 * aws-sqs 프로파일이 활성화될 때만 로드됩니다.
 */
@Configuration
@Profile("aws-sqs")
public class AwsSqsConfig {
    
    @Value("${queue.aws.sqs.endpoint:}")
    private String endpoint;
    
    @Value("${queue.aws.sqs.region:ap-northeast-2}")
    private String region;
    
    @Value("${queue.aws.access-key-id:}")
    private String accessKeyId;
    
    @Value("${queue.aws.secret-access-key:}")
    private String secretAccessKey;
    
    /**
     * SQS 클라이언트를 생성합니다.
     * 로컬 테스트를 위한 endpoint 설정을 지원합니다.
     */
    @Bean
    public SqsClient sqsClient() {
        SqsClient.Builder builder = SqsClient.builder()
                .region(Region.of(region));
        
        // 로컬 테스트를 위한 endpoint 설정
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        // AWS 자격 증명 설정 (로컬 테스트용)
        if (accessKeyId != null && !accessKeyId.trim().isEmpty() &&
            secretAccessKey != null && !secretAccessKey.trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }
        
        return builder.build();
    }
}
