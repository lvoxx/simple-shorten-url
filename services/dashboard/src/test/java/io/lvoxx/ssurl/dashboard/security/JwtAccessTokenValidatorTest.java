package io.lvoxx.ssurl.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.lvoxx.ssurl.common.util.Constants;

@DisplayName("JwtAccessTokenValidator Tests")
@Tag("Unit")
class JwtAccessTokenValidatorTest {

    private static final byte[] RAW = "0123456789abcdef0123456789abcdef0123456789abcdef".getBytes();

    private SecretKey key;
    private JwtAccessTokenValidator validator;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(Base64.getEncoder().encodeToString(RAW));
        validator = new JwtAccessTokenValidator(props);
        key = Keys.hmacShaKeyFor(RAW);
    }

    private String token(String type) {
        return Jwts.builder()
                .subject("alice")
                .claim(Constants.Jwt.CLAIM_ROLE, "USER")
                .claim(Constants.Jwt.CLAIM_TYPE, type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void acceptsValidAccessToken() {
        String t = token(Constants.Jwt.TYPE_ACCESS);
        assertThat(validator.isValidAccessToken(t)).isTrue();
        assertThat(validator.getUsername(t)).isEqualTo("alice");
        assertThat(validator.getRole(t)).isEqualTo("USER");
    }

    @Test
    void rejectsRefreshTokenAsAccess() {
        assertThat(validator.isValidAccessToken(token(Constants.Jwt.TYPE_REFRESH))).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(validator.isValidAccessToken("not.a.jwt")).isFalse();
    }

    @Test
    void rejectsTokenSignedWithWrongKey() {
        String foreign = Jwts.builder()
                .subject("mallory")
                .claim(Constants.Jwt.CLAIM_TYPE, Constants.Jwt.TYPE_ACCESS)
                .signWith(Keys.hmacShaKeyFor("ffffffffffffffffffffffffffffffffffffffffffffffff".getBytes()))
                .compact();
        assertThat(validator.isValidAccessToken(foreign)).isFalse();
    }
}
