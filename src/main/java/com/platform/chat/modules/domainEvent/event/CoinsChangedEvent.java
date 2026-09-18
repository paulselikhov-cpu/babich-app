package com.platform.chat.modules.domainEvent.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.core.modules.domainEvent.DomainEvent;
import com.platform.chat.core.modules.domainEvent.EventType;
import com.platform.chat.core.modules.domainEvent.enums.ScopeType;
import com.platform.chat.modules.coin.enums.CoinUpdateReason;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие изменения баланса монет персонажа.
 * Публикуется RewardService при giveCoins/takeCoins.
 * Подписчик (TransactionLogEventListener) пишет аудит-запись в TransactionLog.
 *
 * <p>Вычисляемые геттеры {@code getCharacterId()}, {@code getAmount()},
 * {@code getReason()}, {@code getRelatedEntityId()} помечены {@code @JsonIgnore}:
 * при сериализации события в event_publication Modulith данные уходят в payload
 * базового класса и при replay восстанавливаются через конструктор
 * {@link DomainEvent} — аналогично {@link ElectionClosedEvent}.</p>
 */
public class CoinsChangedEvent extends DomainEvent {

    public CoinsChangedEvent(Long characterId, Long amount, CoinUpdateReason reason, Long relatedEntityId) {
        super(
                EventType.COINS_CHANGED,
                characterId, // actorId — персонаж-инициатор изменения баланса
                characterId, // targetId — персонаж, чей баланс изменился
                ScopeType.CHARACTER,
                characterId,
                buildPayload(amount, reason, relatedEntityId)
        );
    }

    @JsonIgnore
    public Long getCharacterId() {
        return getActorId();
    }

    @JsonIgnore
    public Long getAmount() {
        return (Long) getPayload().get("amount");
    }

    @JsonIgnore
    public CoinUpdateReason getReason() {
        Object reason = getPayload().get("reason");
        return reason != null ? CoinUpdateReason.valueOf((String) reason) : null;
    }

    @JsonIgnore
    public Long getRelatedEntityId() {
        return (Long) getPayload().get("relatedEntityId");
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