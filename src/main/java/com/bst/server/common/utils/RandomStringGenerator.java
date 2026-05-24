package com.bst.server.common.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomStringGenerator {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase();
    private static final String DIGITS = "0123456789";
    private static final String DEFAULT_CHARACTERS = UPPER + LOWER + DIGITS;

    private static final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        return generate(length, DEFAULT_CHARACTERS);
    }

    public static String generate(int length, String characters) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}
