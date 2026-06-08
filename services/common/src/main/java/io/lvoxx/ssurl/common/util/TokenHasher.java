package io.lvoxx.ssurl.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * One-way hashing for opaque bearer secrets (e.g. refresh tokens).
 *
 * <p>
 * Refresh tokens are persisted and cached by their SHA-256 hex digest rather
 * than their raw value, so that a read-only compromise of Postgres or Redis
 * does not yield directly replayable session tokens. The raw token is only ever
 * held by the client (HTTP-only cookie); the server hashes the incoming value
 * and looks records up by digest.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /**
     * Computes the lowercase SHA-256 hex digest of {@code token}.
     *
     * @param token the raw token value (must not be {@code null})
     * @return 64-character lowercase hex digest
     */
    public static String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec; this is unreachable.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
