package io.lvoxx.ssurl.common;

import org.junit.jupiter.api.Test;

import io.lvoxx.ssurl.common.util.NumberToBytes;
import io.seruco.encoding.base62.Base62;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommonApplicationTests {

    Base62 base62 = Base62.createInstance();

    @Test
    void base62EncoderRoundTrip() {
        long original = 123456789L;
        String encoded = base62.encode(NumberToBytes.longToBytes(original)).toString();
        assertFalse(encoded.isEmpty());
        assertEquals(original, base62.decode(encoded.getBytes()));
    }

    @Test
    void base62EncoderZero() {
        assertEquals("0", base62.encode(NumberToBytes.longToBytes(0L)));
    }
}
