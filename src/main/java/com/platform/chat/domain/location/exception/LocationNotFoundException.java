package com.platform.chat.domain.location.exception;

/**
 * Локация с таким id не найдена (глубокий переход по /chat/location/{id}).
 * Бизнес-ошибка: отдаётся клиенту с текстом, см. {@code GlobalExceptionHandler}.
 */
public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(Long locationId) {
        super("Локация не найдена: " + locationId);
    }
}
