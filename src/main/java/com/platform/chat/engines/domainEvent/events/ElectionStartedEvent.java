package com.platform.chat.engines.domainEvent.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.chat.engines.domainEvent.DomainEvent;
import com.platform.chat.engines.domainEvent.EventType;
import com.platform.chat.engines.domainEvent.enums.ScopeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие старта выборов губернатора.
 * Публикуется ElectionService после инициации голосования.
 * Подписчик (NotificationEventListener) уведомляет жителей района
 * о начале выборов и шлёт WS-пуш.
 *
 * <p>{@code initiatorCharacterId} — персонаж, выдвинувший кандидатуру
 * (внутренний ChatUser.id, как и все characterId в poll-модуле).</p>
 *
 * <p>Вычисляемые геттеры {@code getPollId()}/{@code getInitiatorCharacterId()}
 * помечены {@code @JsonIgnore}: при сериализации события в event_publication
 * данные уходят в payload базового класса и при replay восстанавливаются через
 * конструктор {@link DomainEvent}.</p>
 */
public class ElectionStartedEvent extends DomainEvent {

    public ElectionStartedEvent(Long districtId, Long pollId, Long initiatorCharacterId) {
        super(
                EventType.ELECTION_STARTED,
                initiatorCharacterId, // actorId — тот, кто инициировал выборы
                initiatorCharacterId, // targetId — первый кандидат
                ScopeType.DISTRICT,
                districtId,
                buildPayload(pollId, initiatorCharacterId)
        );
    }

    /**
     * Creator для replay из {@code event_publication}: Jackson не наследует
     * {@code @JsonCreator} базового класса — id голосования берётся из payload.
     */
    @JsonCreator
    protected ElectionStartedEvent(@JsonProperty("type") EventType type,
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
    public Long getInitiatorCharacterId() {
        return getActorId();
    }

    private static Map<String, Object> buildPayload(Long pollId, Long initiatorCharacterId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pollId", pollId);
        if (initiatorCharacterId != null) {
            payload.put("initiatorCharacterId", initiatorCharacterId);
        }
        return payload;
    }
}
