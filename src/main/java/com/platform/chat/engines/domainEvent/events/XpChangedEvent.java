package com.platform.chat.engines.domainEvent.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.chat.engines.domainEvent.DomainEvent;
import com.platform.chat.engines.domainEvent.EventType;
import com.platform.chat.engines.domainEvent.enums.ScopeType;
import com.platform.chat.engines.transactionLog.enums.CoinUpdateReason;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие изменения опыта (XP) персонажа.
 * Публикуется CoinService при giveXp.
 * Подписчик (TransactionLogEventListener) пишет аудит-запись в TransactionLog.
 *
 * <p>Аналогично {@link CoinsChangedEvent}: вычисляемые геттеры помечены
 * {@code @JsonIgnore}, данные живут в payload базового класса и при replay
 * восстанавливаются через конструктор {@link DomainEvent}.</p>
 */
public class XpChangedEvent extends DomainEvent {

    public XpChangedEvent(Long characterId, Long amount, CoinUpdateReason reason, Long relatedEntityId) {
        super(
                EventType.XP_CHANGED,
                characterId, // actorId — персонаж-инициатор изменения XP
                characterId, // targetId — персонаж, чей XP изменился
                ScopeType.CHARACTER,
                characterId,
                buildPayload(amount, reason, relatedEntityId)
        );
    }

    /**
     * Creator для replay из {@code event_publication}: Jackson не наследует
     * {@code @JsonCreator} базового класса — сумму и причину восстанавливаем
     * из payload (числа читаются через {@link #longPayload(String)}).
     */
    @JsonCreator
    protected XpChangedEvent(@JsonProperty("type") EventType type,
                             @JsonProperty("actorId") Long actorId,
                             @JsonProperty("targetId") Long targetId,
                             @JsonProperty("scopeType") ScopeType scopeType,
                             @JsonProperty("scopeId") Long scopeId,
                             @JsonProperty("payload") Map<String, Object> payload) {
        super(type, actorId, targetId, scopeType, scopeId, payload);
    }

    @JsonIgnore
    public Long getCharacterId() {
        return getActorId();
    }

    @JsonIgnore
    public Long getAmount() {
        return longPayload("amount");
    }

    @JsonIgnore
    public CoinUpdateReason getReason() {
        Object reason = getPayload().get("reason");
        return reason != null ? CoinUpdateReason.valueOf((String) reason) : null;
    }

    @JsonIgnore
    public Long getRelatedEntityId() {
        return longPayload("relatedEntityId");
    }

    private static Map<String, Object> buildPayload(Long amount, CoinUpdateReason reason, Long relatedEntityId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);
        if (reason != null) {
            payload.put("reason", reason.name());
        }
        if (relatedEntityId != null) {
            payload.put("relatedEntityId", relatedEntityId);
        }
        return payload;
    }
}
