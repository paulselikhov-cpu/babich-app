package com.platform.chat.core.event.childs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.core.event.base.DomainEvent;
import com.platform.chat.core.event.base.EventType;
import com.platform.chat.core.event.base.ScopeType;
import com.platform.chat.modules.economy.enums.RewardReason;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие изменения опыта (XP) персонажа.
 * Публикуется RewardService при giveXp.
 * Подписчик (TransactionLogEventListener) пишет аудит-запись в TransactionLog.
 *
 * <p>Аналогично {@link CoinsChangedEvent}: вычисляемые геттеры помечены
 * {@code @JsonIgnore}, данные живут в payload базового класса и при replay
 * восстанавливаются через конструктор {@link DomainEvent}.</p>
 */
public class XpChangedEvent extends DomainEvent {

    public XpChangedEvent(Long characterId, Long amount, RewardReason reason, Long relatedEntityId) {
        super(
                EventType.XP_CHANGED,
                characterId, // actorId — персонаж-инициатор изменения XP
                characterId, // targetId — персонаж, чей XP изменился
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
    public RewardReason getReason() {
        Object reason = getPayload().get("reason");
        return reason != null ? RewardReason.valueOf((String) reason) : null;
    }

    @JsonIgnore
    public Long getRelatedEntityId() {
        return (Long) getPayload().get("relatedEntityId");
    }

    private static Map<String, Object> buildPayload(Long amount, RewardReason reason, Long relatedEntityId) {
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