package com.booking.urlshortener.service;

/**
 * Base62 encoding using {@code [0-9a-zA-Z]} (62 chars). Length 6 yields ~56.8 B
 * combinations, length 7 yields ~3.5 T -- both far exceed the addressable
 * counter space we use for allocation.
 */
public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private Base62() {}

    public static String encode(long value) {
        if (value < 0) throw new IllegalArgumentException("value must be non-negative");
        if (value == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET[(int) (value % 62)]);
            value /= 62;
        }
        return sb.reverse().toString();
    }
}
