package com.platform.core.event;

/**
 * Область действия события.
 * Дублирует com.platform.poll.enums.ScopeType для использования в core-модуле
 * (чтобы core не зависел от poll).
 */
public enum ScopeType {
    CHARACTER,
    DISTRICT,
    PARLIAMENT,
    GANG
}