package com.platform.chat.modules.application;

import org.springframework.modulith.NamedInterface;

/**
 * Тип гражданской заявки. Каждый тип обрабатывается своим
 * ApplicationHandler (стратегия) — см. пакет civic.handler.
 * Список открыт для расширения по мере роста платформы
 * (вступление в группировку, покупка бизнеса, назначение на роль и т.д.).
 */
@NamedInterface("ApplicationType")
public enum ApplicationType {
    BUY_FIRST_LOCATION,
    REGISTER_PASSPORT,
    REGISTER_WORK_LICENCE,
}
