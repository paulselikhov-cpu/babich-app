package com.platform.chat.engines.application.exception;

/**
 * Подача заявки отклонена бизнес-правилом: у персонажа уже есть такой документ
 * или уже есть незакрытая заявка этого типа. Бизнес-ошибка — текст показывается
 * в тосте на фронте, см. {@code GlobalExceptionHandler}.
 */
public class ApplicationSubmissionRejectedException extends RuntimeException {

    public ApplicationSubmissionRejectedException(String reason) {
        super(reason);
    }
}
