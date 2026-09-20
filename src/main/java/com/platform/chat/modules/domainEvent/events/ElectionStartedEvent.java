package com.platform.chat.modules.domainEvent.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.core.modules.domainEvent.DomainEvent;
import com.platform.chat.core.modules.domainEvent.EventType;
import com.platform.chat.core.modules.domainEvent.enums.ScopeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие старта выборов губернатора.
 * Публикуется ElectionFacade после инициации голосования (Слой 3).
 * Подписчик (Слой 4, NotificationEventListener) уведомляет жителей района
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

    @JsonIgnore
    public Long getPollId() {
        return (Long) getPayload().get("pollId");
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
