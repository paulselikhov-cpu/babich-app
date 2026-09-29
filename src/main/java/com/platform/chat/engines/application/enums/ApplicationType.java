package com.platform.chat.core.modules.application.enums;

import lombok.Getter;

import org.springframework.modulith.NamedInterface;

/**
 * Тип гражданской заявки. Каждый тип обрабатывается своим
 * {@code ApplicationHandler} (стратегия) — см. пакет
 * {@code com.platform.chat.modules.application.handler}.
 * Список открыт для расширения по мере роста платформы
 * (вступление в группировку, покупка бизнеса, назначение на роль и т.д.).
 *
 * <p>Живёт в ядре процесса заявки ({@code core.modules.application}), а не в
 * модуле исполнителей: это доменный словарь процесса, на который ссылается
 * поле {@code Application.type}. Иначе ядро зависело бы от модуля исполнителей
 * (направленный цикл core ↔ feature).</p>
 */
@Getter
@NamedInterface("ApplicationType")
public enum ApplicationType {

    /** Покупка первого жилья: номер лота. */
    BUY_FIRST_LOCATION("LOC"),

    /** Регистрация паспорта: номер паспорта. */
    REGISTER_PASSPORT("PP"),

    /** Регистрация рабочей лицензии: номер разрешения на работу. */
    REGISTER_WORK_LICENCE("WRK");

    /**
     * Префикс номера документа, который выдаётся по заявке этого типа.
     * Живёт здесь, а не в генераторе номеров: тип заявки — единственное место,
     * которое знает про свой документ (иначе знание о типах дублировалось бы
     * в switch внутри генератора).
     */
    private final String documentPrefix;

    ApplicationType(String documentPrefix) {
        this.documentPrefix = documentPrefix;
    }
}
