package com.news.stream.websocket;

import com.news.stream.model.Customer;
import com.news.stream.service.AuthenticationService;
import com.news.stream.service.CustomerService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {
    
    private final AuthenticationService authenticationService;
    private final CustomerService customerService;
    
    public WebSocketAuthenticationInterceptor(AuthenticationService authenticationService,
                                             CustomerService customerService) {
        this.authenticationService = authenticationService;
        this.customerService = customerService;
    }
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new MessageDeliveryException("인증 토큰이 필요합니다");
            }
            
            token = token.substring(7); // "Bearer " 제거
            Optional<Customer> customer = authenticationService.validateToken(token);
            if (customer.isEmpty()) {
                throw new MessageDeliveryException("유효하지 않은 토큰입니다");
            }
            
            Customer cust = customer.get();
            if (!authenticationService.isConnectionAvailable(cust.getId())) {
                throw new MessageDeliveryException("이미 연결된 고객사입니다");
            }
            
            // 연결 정보 저장
            String sessionId = accessor.getSessionId();
            cust.setConnectionId(sessionId);
            customerService.save(cust);
        }
        
        return message;
    }
}
