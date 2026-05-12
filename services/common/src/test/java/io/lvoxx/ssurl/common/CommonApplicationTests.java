package io.lvoxx.ssurl.common;

import org.junit.jupiter.api.Test;

import io.lvoxx.ssurl.common.util.NumberToBytes;
import io.seruco.encoding.base62.Base62;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonApplicationTests {

    Base62 base62 = Base62.createInstance();

    @Test
    void base62EncoderRoundTrip() {
        long original = 123456789L;
        byte[] originalBytes = NumberToBytes.longToBytes(original);
        byte[] encoded = base62.encode(originalBytes);
        assertFalse(encoded.length == 0);
        byte[] decoded = base62.decode(encoded);
        assertArrayEquals(originalBytes, decoded);
    }

    @Test
    void base62EncoderZero() {
        String encoded = new String(base62.encode(NumberToBytes.longToBytes(0L)), StandardCharsets.US_ASCII);
        assertFalse(encoded.isEmpty());
    }

    @Test
    void base62EncoderMaxLong() {
        byte[] originalBytes = NumberToBytes.longToBytes(Long.MAX_VALUE);
        byte[] encoded = base62.encode(originalBytes);
        assertFalse(encoded.length == 0);
        byte[] decoded = base62.decode(encoded);
        assertArrayEquals(originalBytes, decoded);
    }

    @Test
    void base62EncoderMinLong() {
        byte[] originalBytes = NumberToBytes.longToBytes(Long.MIN_VALUE);
        byte[] encoded = base62.encode(originalBytes);
        assertFalse(encoded.length == 0);
        byte[] decoded = base62.decode(encoded);
        assertArrayEquals(originalBytes, decoded);
    }

    @Test
    void base62EncoderOne() {
        byte[] originalBytes = NumberToBytes.longToBytes(1L);
        byte[] encoded = base62.encode(originalBytes);
        String encodedStr = new String(encoded, StandardCharsets.US_ASCII);
        assertFalse(encodedStr.isEmpty());
        byte[] decoded = base62.decode(encoded);
        assertArrayEquals(originalBytes, decoded);
    }

    @Test
    void longToBytesNullSafe() {
        byte[] result = NumberToBytes.longToBytes((Long) null);
        assertNotNull(result);
        assertEquals(8, result.length);
    }

    @Test
    void intToBytes() {
        byte[] result = NumberToBytes.intToBytes(255);
        assertNotNull(result);
        assertEquals(4, result.length);
    }

    @Test
    void intToBytesNullSafe() {
        byte[] result = NumberToBytes.intToBytes((Integer) null);
        assertNotNull(result);
        assertEquals(4, result.length);
    }

    @Test
    void intToBytesZero() {
        byte[] result = NumberToBytes.intToBytes(0);
        assertNotNull(result);
        assertEquals(4, result.length);
    }

    @Test
    void shortCodeGenerationConsistency() {
        long id = 42L;
        byte[] bytes = NumberToBytes.longToBytes(id);
        byte[] encoded = base62.encode(bytes);
        assertFalse(encoded.length == 0);
        byte[] decoded = base62.decode(encoded);
        long decodedId = bytesToLong(decoded);
        assertEquals(id, decodedId);
    }

    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) + (b & 0xFF);
        }
        return value;
    }
}
