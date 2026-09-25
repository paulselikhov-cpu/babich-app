package com.platform.chat.core.modules.application.exception;

import com.platform.chat.core.modules.application.enums.ApplicationType;

/**
 * Для типа заявки не зарегистрирована стратегия обработки.
 * Ошибка времени выполнения (заявка существует, обработать её нечем):
 * отдаётся клиенту с текстом, см. {@code GlobalExceptionHandler}.
 */
public class ApplicationHandlerMissingException extends RuntimeException {

    public ApplicationHandlerMissingException(ApplicationType type) {
        super("Нет обработчика для типа заявки: " + type);
    }
}
