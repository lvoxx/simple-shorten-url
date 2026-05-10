package io.lvoxx.ssurl.common.util;

public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62Encoder() {}

    public static String encode(long value) {
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            sb.append(ALPHABET.charAt((int) (remaining % BASE)));
            remaining /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String encoded) {
        long result = 0;
        for (char c : encoded.toCharArray()) {
            result = result * BASE + ALPHABET.indexOf(c);
        }
        return result;
    }
}
