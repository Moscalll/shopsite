package com.example.shopsite.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // 从配置文件注入密钥
    @Value("${jwt.secret}")
    private String jwtSecret;

    // 从配置文件注入有效期
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // 1. 获取签名密钥
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 2. 生成 Token
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username) // 主体（通常是用户名或用户ID）
                .issuedAt(now)     // 签发时间
                .expiration(expiryDate) // 过期时间
                .signWith(getSigningKey(), Jwts.SIG.HS256) // 使用密钥签名
                .compact();
    }

    // 3. 从 Token 中获取用户名 (用于后续验证)
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // 4. 验证 Token 是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parse(token);
            return true;
        } catch (Exception e) {
            // 在实际项目中，需要根据不同的异常类型记录日志
            return false;
        }
    }
}