package io.lvoxx.ssurl.api_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String BASE64_SECRET = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi0yNTYtYml0";
    private static final long ACCESS_EXPIRY = 900;
    private static final long REFRESH_EXPIRY = 604800;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(BASE64_SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    }

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("creates a valid access token with correct claims")
        void createsValidAccessToken() {
            String token = jwtTokenProvider.createAccessToken("alice", "USER");

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("alice");
            assertThat(jwtTokenProvider.getRole(token)).isEqualTo("USER");
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("creates a valid refresh token with correct claims")
        void createsValidRefreshToken() {
            String token = jwtTokenProvider.createRefreshToken("alice");

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("returns true for a valid token")
        void validToken_returnsTrue() {
            String token = jwtTokenProvider.createAccessToken("alice", "USER");
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for a malformed token")
        void malformedToken_returnsFalse() {
            assertThat(jwtTokenProvider.validateToken("not-a-valid-token")).isFalse();
        }

        @Test
        @DisplayName("returns false for an empty token")
        void emptyToken_returnsFalse() {
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("returns false for a token with wrong signature")
        void wrongSignature_returnsFalse() {
            String differentSecret = "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLTI1Ni1iaXQ=";
            var differentKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(differentSecret));
            String token = Jwts.builder()
                    .subject("alice")
                    .claim("role", "USER")
                    .signWith(differentKey)
                    .compact();

            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("parseToken")
    class ParseToken {

        @Test
        @DisplayName("returns valid claims for a correct token")
        void parseToken_returnsClaims() {
            String token = jwtTokenProvider.createAccessToken("alice", "USER");
            Claims claims = jwtTokenProvider.parseToken(token);

            assertThat(claims.getSubject()).isEqualTo("alice");
            assertThat(claims.get("role", String.class)).isEqualTo("USER");
            assertThat(claims.get("type", String.class)).isEqualTo("access");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("throws JwtException for an invalid token")
        void parseToken_invalid_throws() {
            assertThatThrownBy(() -> jwtTokenProvider.parseToken("invalid-token"))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("getAccessExpiryMs")
    class GetAccessExpiryMs {

        @Test
        @DisplayName("returns access expiry in milliseconds")
        void returnsAccessExpiryMs() {
            assertThat(jwtTokenProvider.getAccessExpiryMs()).isEqualTo(ACCESS_EXPIRY * 1000);
        }
    }
}
