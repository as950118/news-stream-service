package com.news.stream.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret:your-256-bit-secret-key-here}")
    private String jwtSecret;
    
    @Value("${jwt.expiration-hours:24}")
    private long jwtExpirationHours;
    
    public String generateToken(String customerId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 
            TimeUnit.HOURS.toMillis(jwtExpirationHours));
        
        return Jwts.builder()
            .subject(customerId)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }
    
    public String getCustomerIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    private SecretKey getSigningKey() {
        // 256비트 (32바이트) 키 생성
        byte[] keyBytes = jwtSecret.getBytes();
        if (keyBytes.length < 32) {
            // 키가 32바이트보다 작으면 패딩
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
