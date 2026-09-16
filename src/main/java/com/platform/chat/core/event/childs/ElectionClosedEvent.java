package com.platform.chat.core.event.childs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.core.event.base.DomainEvent;
import com.platform.chat.core.event.base.EventType;
import com.platform.chat.core.event.base.ScopeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие закрытия выборов губернатора.
 * Публикуется ElectionFacade после обработки результата (Слой 3).
 * Подписчик (Слой 4, NotificationEventListener) сохраняет уведомления
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

    @JsonIgnore
    public Long getPollId() {
        return (Long) getPayload().get("pollId");
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