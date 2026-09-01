package com.platform.civic.enums;

/**
 * Тип гражданской заявки. Каждый тип обрабатывается своим
 * ApplicationHandler (стратегия) — см. пакет civic.handler.
 * Список открыт для расширения по мере роста платформы
 * (вступление в группировку, покупка бизнеса, назначение на роль и т.д.).
 */
public enum ApplicationType {
    BUY_FIRST_LOCATION
}
