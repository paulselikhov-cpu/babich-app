package com.platform.chat.engines.application.utils;

import com.platform.chat.engines.application.enums.ApplicationType;
import com.platform.chat.engines.application.model.DocumentNumberGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Случайный номер документа: {@code <префикс типа>-XXXX-XXXX}.
 * Префикс берётся у {@link ApplicationType} — тип заявки знает про свой документ,
 * генератор про типы не знает ничего.
 */
@Component
public class RandomDocumentNumberGenerator implements DocumentNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Префикс, если тип заявки не определён (защита от null). */
    private static final String DEFAULT_PREFIX = "DF";

    @Override
    public String next(ApplicationType type) {
        String prefix = type == null ? DEFAULT_PREFIX : type.getDocumentPrefix();
        int first = RANDOM.nextInt(9000) + 1000;
        int second = RANDOM.nextInt(9000) + 1000;
        return String.format("%s-%04d-%04d", prefix, first, second);
    }
}
