package com.corner.takecontrol.util;

import java.security.SecureRandom;
import java.util.Locale;

public final class ShareCodeGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ShareCodeGenerator() {
    }

    public static String generate() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toUpperCase(Locale.US);
    }
}
