package io.lvoxx.ssurl.common;

import org.junit.jupiter.api.Test;

import io.lvoxx.ssurl.common.util.NumberToBytes;
import io.seruco.encoding.base62.Base62;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
