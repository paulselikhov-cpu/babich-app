package com.platform.chat.engines.application.exception;

/**
 * Заявка с таким id не найдена (команда approve/reject по несуществующему id).
 * Бизнес-ошибка: отдаётся клиенту с текстом, см. {@code GlobalExceptionHandler}.
 */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long applicationId) {
        super("Заявка не найдена: " + applicationId);
    }
}
