package com.platform.core.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие закрытия голосования (этап 6 плана миграции событий).
 * <p>
 * Публикуется {@code PollResultDispatcher} после завершения обработки результата
 * (Слой 4) для ЛЮБОГО типа голосования — выборов, обычного опроса, отзыва и т.п.
 * Подписчики (например {@code NotificationEventListener}) могут использовать
 * {@link #getPollType()} и {@link #getScopeTypeName()} для собственной логики —
 * единого обработчика последствий больше нет.
 * <p>
 * Для выборов губернатора продолжает публиковаться отдельное событие
 * {@link ElectionClosedEvent} — их подписчики (уведомления с именем победителя)
 * не должны ломаться. {@code PollClosedEvent} в этом случае идёт дополнительно.
 */
public class PollClosedEvent extends DomainEvent {

    public PollClosedEvent(Long scopeId, Long pollId, String pollTypeName, String scopeTypeName) {
        super(
                EventType.POLL_CLOSED,
                null, // actorId — закрытие может идти по таймеру, актор неизвестен
                null, // targetId
                coreScopeType(scopeTypeName),
                scopeId,
                buildPayload(pollId, pollTypeName, scopeTypeName)
        );
    }

    @JsonIgnore
    public Long getPollId() {
        return (Long) getPayload().get("pollId");
    }

    @JsonIgnore
    public String getPollType() {
        return (String) getPayload().get("pollType");
    }

    @JsonIgnore
    public String getScopeTypeName() {
        return (String) getPayload().get("scopeType");
    }

    private static Map<String, Object> buildPayload(Long pollId, String pollTypeName, String scopeTypeName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pollId", pollId);
        payload.put("pollType", pollTypeName);
        payload.put("scopeType", scopeTypeName);
        return payload;
    }

    /**
     * Маппит ScopeType из poll-модуля в core-модуль.
     * Значения enum совпадают (DISTRICT, PARLIAMENT, GANG) — core держит
     * собственный enum, чтобы не зависеть от poll.
     */
    private static com.platform.core.event.ScopeType coreScopeType(String pollScopeTypeName) {
        if (pollScopeTypeName == null) {
            return null;
        }
        return com.platform.core.event.ScopeType.valueOf(pollScopeTypeName);
    }
}