package io.lvoxx.ssurl.api_service.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.lvoxx.ssurl.api_service.properties.JwtProperties;
import io.lvoxx.ssurl.common.util.Constants;
import jakarta.annotation.PostConstruct;

@Component
public class JwtTokenProvider {

    private SecretKey secretKey;
    private long accessExpiryMs;
    private long refreshExpiryMs;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
        this.accessExpiryMs = jwtProperties.getAccessExpiry() * 1000L;
        this.refreshExpiryMs = jwtProperties.getRefreshExpiry() * 1000L;
    }

    public String createAccessToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim(Constants.Jwt.CLAIM_ROLE, role)
                .claim(Constants.Jwt.CLAIM_TYPE, Constants.Jwt.TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiryMs))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim(Constants.Jwt.CLAIM_TYPE, Constants.Jwt.TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiryMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return (String) parseToken(token).get(Constants.Jwt.CLAIM_ROLE);
    }

    public long getAccessExpiryMs() {
        return accessExpiryMs;
    }
}
