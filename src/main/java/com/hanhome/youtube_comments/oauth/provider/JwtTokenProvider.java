package com.hanhome.youtube_comments.oauth.provider;

import com.hanhome.youtube_comments.oauth.dto.CustomTokenRecord;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret-key}")
    private String secretKey;

    private final long accessInMillisec = 1000 * 60 * 60; // 1 hours
    private final long refreshInMilliSec = accessInMillisec * 24 * 7; // 7 days

    public CustomTokenRecord createAccessToken(UUID uid, String email) {
        Date now = new Date();
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);

        String token = Jwts.builder()
                .setSubject(uid.toString())
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessInMillisec))
                .signWith(key)
                .compact();

        return new CustomTokenRecord(accessInMillisec, token, TimeUnit.MILLISECONDS);
    }

    public CustomTokenRecord createRefreshToken(UUID uid) {
        Date now = new Date();
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        String token = Jwts.builder()
                .setSubject(uid.toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshInMilliSec))
                .signWith(key)
                .compact();

        return new CustomTokenRecord(refreshInMilliSec, token, TimeUnit.MILLISECONDS);
    }

    public Claims validate(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT expired");
        } catch (Exception e) {
            throw new RuntimeException("JWT Validation failed");
        }
    }
}
