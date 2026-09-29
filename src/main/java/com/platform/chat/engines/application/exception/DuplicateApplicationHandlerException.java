package com.platform.chat.engines.application.exception;

import com.platform.chat.engines.application.enums.ApplicationType;

/**
 * На один тип заявки зарегистрировано больше одной стратегии — ошибка
 * конфигурации приложения. Бросается при старте (реестр строит карту в
 * {@code @PostConstruct}), чтобы приложение не поднялось с неоднозначной
 * обработкой заявок — по образцу {@code PollResultDispatcher}.
 */
public class DuplicateApplicationHandlerException extends RuntimeException {

    public DuplicateApplicationHandlerException(ApplicationType type,
                                                String firstHandler,
                                                String secondHandler) {
        super("Дублирующийся ApplicationHandler для типа " + type + ": "
                + firstHandler + " и " + secondHandler);
    }
}
