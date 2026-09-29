package com.platform.chat.engines.domainEvent.enums;

/**
 * Область действия события.
 * Дублирует com.platform.chat.features.enums.poll.ScopeType для использования в core-модуле
 * (чтобы core не зависел от poll).
 */
public enum ScopeType {
    CHARACTER,
    DISTRICT,
    PARLIAMENT,
    GANG
}