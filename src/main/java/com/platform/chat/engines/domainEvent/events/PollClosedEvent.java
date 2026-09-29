package com.platform.chat.core.modules.domainEvent.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.chat.core.modules.domainEvent.DomainEvent;
import com.platform.chat.core.modules.domainEvent.EventType;
import com.platform.chat.core.modules.domainEvent.enums.ScopeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие закрытия голосования — общий факт для ЛЮБОГО типа голосования
 * (выборы, обычный опрос, отзыв и т.п.).
 * <p>
 * Публикуется {@code PollResultDispatcher} после завершения обработки результата
 * для ЛЮБОГО типа голосования — выборов, обычного опроса, отзыва и т.п.
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

    /**
     * Creator для replay из {@code event_publication}: Jackson не наследует
     * {@code @JsonCreator} базового класса — тип голосования восстанавливается
     * из payload.
     */
    @JsonCreator
    protected PollClosedEvent(@JsonProperty("type") EventType type,
                              @JsonProperty("actorId") Long actorId,
                              @JsonProperty("targetId") Long targetId,
                              @JsonProperty("scopeType") ScopeType scopeType,
                              @JsonProperty("scopeId") Long scopeId,
                              @JsonProperty("payload") Map<String, Object> payload) {
        super(type, actorId, targetId, scopeType, scopeId, payload);
    }

    @JsonIgnore
    public Long getPollId() {
        return longPayload("pollId");
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
    private static ScopeType coreScopeType(String pollScopeTypeName) {
        if (pollScopeTypeName == null) {
            return null;
        }
        return ScopeType.valueOf(pollScopeTypeName);
    }
}
