package io.lvoxx.ssurl.api_service.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.lvoxx.ssurl.api_service.properties.JwtProperties;
import io.lvoxx.ssurl.common.util.Constants;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    /** Spring injection path — derives the key/expiries from bound properties. */
    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this(jwtProperties.getSecret(), jwtProperties.getAccessExpiry(), jwtProperties.getRefreshExpiry());
    }

    /**
     * Primary constructor (also used directly in unit tests). Builds the HMAC
     * key from a Base64-encoded secret and converts the expiry seconds to ms.
     *
     * @param base64Secret         Base64-encoded HMAC-SHA signing secret
     * @param accessExpirySeconds  access-token lifetime in seconds
     * @param refreshExpirySeconds refresh-token lifetime in seconds
     */
    public JwtTokenProvider(String base64Secret, long accessExpirySeconds, long refreshExpirySeconds) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessExpiryMs = accessExpirySeconds * 1000L;
        this.refreshExpiryMs = refreshExpirySeconds * 1000L;
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

    /**
     * Returns {@code true} only if the token is a valid <em>access</em> token
     * (its {@code type} claim equals {@link Constants.Jwt#TYPE_ACCESS}).
     *
     * <p>
     * This prevents a long-lived refresh token from being presented as a
     * {@code Bearer} access token to bypass the short access-token lifetime.
     */
    public boolean isAccessToken(String token) {
        try {
            return Constants.Jwt.TYPE_ACCESS.equals(parseToken(token).get(Constants.Jwt.CLAIM_TYPE));
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
