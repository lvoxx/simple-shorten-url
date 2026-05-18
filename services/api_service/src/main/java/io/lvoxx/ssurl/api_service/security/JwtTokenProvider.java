package io.lvoxx.ssurl.api_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.lvoxx.ssurl.common.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiry:900}") long accessExpirySec,
            @Value("${app.jwt.refresh-expiry:604800}") long refreshExpirySec) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiryMs = accessExpirySec * 1000;
        this.refreshExpiryMs = refreshExpirySec * 1000;
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
