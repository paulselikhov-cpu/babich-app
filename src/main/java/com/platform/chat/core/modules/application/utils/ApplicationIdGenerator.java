package com.platform.chat.core.modules.application.utils;

import com.platform.chat.core.modules.application.ApplicationType;

import java.security.SecureRandom;

public final class ApplicationIdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApplicationIdGenerator() {}

    public static String generate(ApplicationType type) {
        int first = RANDOM.nextInt(9000) + 1000;
        int second = RANDOM.nextInt(9000) + 1000;
        return switch (type) {
            case REGISTER_WORK_LICENCE -> String.format("WRK-%04d-%04d", first, second);
            case REGISTER_PASSPORT -> String.format("PP-%04d-%04d", first, second);
            case null, default -> String.format("DF-%04d-%04d", first, second);
        };
    }
}
