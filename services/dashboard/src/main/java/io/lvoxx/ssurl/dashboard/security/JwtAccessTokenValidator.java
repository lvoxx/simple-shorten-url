package io.lvoxx.ssurl.dashboard.security;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.lvoxx.ssurl.common.util.Constants;

/**
 * Read-only counterpart of api_service's {@code JwtTokenProvider}: validates and
 * parses access tokens using the shared HMAC secret.
 *
 * <p>
 * Intentionally duplicated (rather than extracted into {@code common}) to keep
 * the api_service blast radius at zero; flagged for future consolidation.
 */
@Component
public class JwtAccessTokenValidator {

    private final SecretKey secretKey;

    public JwtAccessTokenValidator(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** {@code true} only for a structurally valid, signed <em>access</em> token. */
    public boolean isValidAccessToken(String token) {
        try {
            return Constants.Jwt.TYPE_ACCESS.equals(parse(token).get(Constants.Jwt.CLAIM_TYPE));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public String getRole(String token) {
        return (String) parse(token).get(Constants.Jwt.CLAIM_ROLE);
    }
}
