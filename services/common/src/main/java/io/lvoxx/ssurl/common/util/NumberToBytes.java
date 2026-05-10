package io.lvoxx.ssurl.common.util;

public class NumberToBytes {
    public static byte[] longToBytes(long value) {
        return new byte[] {
                (byte) (value >>> 56),
                (byte) (value >>> 48),
                (byte) (value >>> 40),
                (byte) (value >>> 32),
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static byte[] longToBytes(Long value) {
        return longToBytes(value != null ? value : 0L);
    }

    public static byte[] intToBytes(int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static byte[] intToBytes(Integer value) {
        return intToBytes(value != null ? value : 0);
    }
}
