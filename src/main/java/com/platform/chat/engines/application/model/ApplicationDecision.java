package com.platform.chat.engines.application.model;

import com.platform.chat.engines.application.enums.ApplicationMessageKey;
import com.platform.chat.engines.application.enums.ApplicationStatus;
import com.platform.chat.engines.domainEvent.DomainEvent;

import java.util.List;

/**
 * Решение стратегии по заявке — то, что {@link ApplicationHandler} возвращает
 * ядру вместо того, чтобы самому менять заявку и публиковать события.
 *
 * <p>Содержит:</p>
 * <ul>
 *   <li>{@code status} — APPROVED/REJECTED;</li>
 *   <li>{@code messageKey} + {@code args} — ключ текста результата и его аргументы:
 *       человекочитаемую строку в {@code Application.resultMessage} пишет ядро
 *       (см. {@code ApplicationMessages}), поэтому стратегия оперирует кодами,
 *       а не готовым текстом;</li>
 *   <li>{@code events} — события-последствия конкретной политики (например,
 *       {@code CharacterUpdatedEvent} после выдачи паспорта). Их публикует ядро,
 *       в той же транзакции, но не зная конкретных типов — именно это позволяет
 *       добавлять новые типы заявок без правки ядра.</li>
 * </ul>
 */
public record ApplicationDecision(
        ApplicationStatus status,
        ApplicationMessageKey messageKey,
        List<Object> args,
        List<DomainEvent> events) {

    public ApplicationDecision {
        args = args == null ? List.of() : List.copyOf(args);
        events = events == null ? List.of() : List.copyOf(events);
    }

    /** Одобрено, без событий-последствий. */
    public static ApplicationDecision approved(ApplicationMessageKey key, Object... args) {
        return new ApplicationDecision(ApplicationStatus.APPROVED, key, List.of(args), List.of());
    }

    /**
     * Одобрено + события-последствия (например, «персонаж изменился»).
     * Перегрузка отличается вторым параметром: {@code List<? extends DomainEvent>}
     * (wildcard — чтобы вызов {@code List.of(new CharacterUpdatedEvent(id))}
     * не «проваливался» в ветку {@code Object...} из-за инвариантности дженериков).
     */
    public static ApplicationDecision approved(ApplicationMessageKey key,
                                               List<? extends DomainEvent> events,
                                               Object... args) {
        return new ApplicationDecision(ApplicationStatus.APPROVED, key, List.of(args), List.copyOf(events));
    }

    /** Отклонено. */
    public static ApplicationDecision rejected(ApplicationMessageKey key, Object... args) {
        return new ApplicationDecision(ApplicationStatus.REJECTED, key, List.of(args), List.of());
    }
}
