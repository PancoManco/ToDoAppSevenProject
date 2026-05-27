package ru.pancomanco.todoappsevenproject.util;

import java.security.SecureRandom;

public final class VerificationCodeGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int MAX_VALUE = (int) Math.pow(10, CODE_LENGTH);

    private VerificationCodeGenerator() {
    }

    public static String getRandomVerificationCode() {
        int code = SECURE_RANDOM.nextInt(MAX_VALUE);
        return String.format("%06d", code);
    }
}
