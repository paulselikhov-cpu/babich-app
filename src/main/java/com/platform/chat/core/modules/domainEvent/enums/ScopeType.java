package com.platform.chat.core.modules.domainEvent.enums;

/**
 * Область действия события.
 * Дублирует com.platform.chat.modules.enums.poll.ScopeType для использования в core-модуле
 * (чтобы core не зависел от poll).
 */
public enum ScopeType {
    CHARACTER,
    DISTRICT,
    PARLIAMENT,
    GANG
}