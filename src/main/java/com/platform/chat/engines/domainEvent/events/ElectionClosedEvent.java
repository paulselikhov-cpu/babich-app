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
 * Событие закрытия выборов губернатора.
 * Публикуется ElectionService после обработки результата.
 * Подписчик (NotificationEventListener) сохраняет уведомления
 * жителям района и шлёт WS-пуш.
 *
 * <p>Вычисляемые геттеры {@code getPollId()}/{@code getWinnerName()}
 * помечены {@code @JsonIgnore}: при сериализации события в event_publication
 * Modulith данные уходят в payload базового класса и при replay
 * восстанавливаются через конструктор {@link DomainEvent} — дублировать их
 * отдельными JSON-свойствами не нужно.</p>
 */
public class ElectionClosedEvent extends DomainEvent {

    public ElectionClosedEvent(Long districtId, Long pollId, String winnerName) {
        super(
                EventType.ELECTION_CLOSED,
                null, // actorId — закрытие может идти по таймеру, актор неизвестен
                null, // targetId
                ScopeType.DISTRICT,
                districtId,
                buildPayload(pollId, winnerName)
        );
    }

    /**
     * Creator для replay из {@code event_publication}: Jackson не наследует
     * {@code @JsonCreator} базового класса — id голосования и имя победителя
     * лежат в payload.
     */
    @JsonCreator
    protected ElectionClosedEvent(@JsonProperty("type") EventType type,
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
    public String getWinnerName() {
        Object name = getPayload().get("winnerName");
        return name != null ? (String) name : "не определён";
    }

    private static Map<String, Object> buildPayload(Long pollId, String winnerName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pollId", pollId);
        if (winnerName != null) {
            payload.put("winnerName", winnerName);
        }
        return payload;
    }
}
