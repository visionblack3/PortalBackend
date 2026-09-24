package com.example.CarrerPortal.Config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.CarrerPortal.Model.Role;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    // Secret key defined in application.properties (must be at least 256 bits / 32 chars)
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Token validity duration in milliseconds (e.g., 24 hours = 86400000 ms)
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // Generate cryptographic key from the secret string
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. Generate JWT Token during user login
    public String generateToken(Long userId, String email, Role role) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // 2. Extract Username/Email from Token
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 3. Extract Role from Token
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    // 4. Validate Token Integrity and Expiration
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid, expired, or tampered token
            System.err.println("Invalid JWT Token: " + e.getMessage());
        }
        return false;
    }
}