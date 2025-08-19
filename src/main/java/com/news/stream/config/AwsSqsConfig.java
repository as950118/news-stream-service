package com.news.stream.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
    public AmazonSQS sqsClient() {
        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard()
                .withRegion(region);
        
        // 로컬 테스트를 위한 endpoint 설정
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            builder.withEndpointConfiguration(
                new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(endpoint, region)
            );
        }
        
        // AWS 자격 증명 설정 (로컬 테스트용)
        if (accessKeyId != null && !accessKeyId.trim().isEmpty() &&
            secretAccessKey != null && !secretAccessKey.trim().isEmpty()) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
            builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        }
        
        return builder.build();
    }
}
