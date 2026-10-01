package com.platform.chat.domain.location.exception;

/**
 * Локация с таким id или кодом не найдена (переход по /chat/location/{code}).
 * Бизнес-ошибка: отдаётся клиенту с текстом, см. {@code GlobalExceptionHandler}.
 */
public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(Long locationId) {
        super("Локация не найдена: " + locationId);
    }

    /**
     * Поиск по коду — искали в районе текущего персонажа.
     * Район в сообщении намеренно не указываем: он и так известен клиенту.
     */
    public LocationNotFoundException(String code) {
        super("Локация не найдена: " + code);
    }
}
