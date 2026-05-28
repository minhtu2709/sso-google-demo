package com.example.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs = 604800000; // 7 ngày

    public JwtUtil(
            @Value("${app.jwt.secret:#{null}}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        if (secret == null || secret.isBlank() || secret.equals("supersecretkey12345678901234567890")) {
            throw new IllegalStateException(
                "CRITICAL SECURITY ERROR: JWT_SECRET environment variable is not set or using insecure default! " +
                "Please set a strong, unique secret key in your environment variables."
            );
        }
        this.secret = secret;
        this.accessTokenExpirationMs = 3600000; // 1 giờ cho Access Token thực tế
    }

    // Tạo token từ email và role của user
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)           // email là "chủ sở hữu" token
                .claim("role", role)      // nhét role vào trong token
                .issuedAt(new Date())     // thời điểm tạo
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(getSigningKey()) // ký bằng secret key
                .compact();
    }

    // Lấy email từ token
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // Lấy role từ token
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // Kiểm tra token còn hợp lệ không
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token); // nếu parse được thì hợp lệ
            return true;
        } catch (Exception e) {
            return false; // token hết hạn hoặc bị sửa
        }
    }

    // Đọc thông tin từ token
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Tạo secret key từ chuỗi config
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}