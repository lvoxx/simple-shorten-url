package io.lvoxx.ssurl.common;

import io.lvoxx.ssurl.common.util.Base62Encoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommonApplicationTests {

    @Test
    void base62EncoderRoundTrip() {
        long original = 123456789L;
        String encoded = Base62Encoder.encode(original);
        assertFalse(encoded.isEmpty());
        assertEquals(original, Base62Encoder.decode(encoded));
    }

    @Test
    void base62EncoderZero() {
        assertEquals("0", Base62Encoder.encode(0L));
    }
}
