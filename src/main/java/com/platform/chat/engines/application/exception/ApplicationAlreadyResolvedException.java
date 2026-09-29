package com.platform.chat.engines.application.exception;

import com.platform.chat.engines.application.enums.ApplicationStatus;

/**
 * Заявка уже решена (повторный approve/reject по не-PENDING заявке).
 * Бизнес-ошибка: отдаётся клиенту с текстом, см. {@code GlobalExceptionHandler}.
 */
public class ApplicationAlreadyResolvedException extends RuntimeException {

    public ApplicationAlreadyResolvedException(ApplicationStatus status) {
        super("Заявка уже обработана, статус: " + status);
    }
}
