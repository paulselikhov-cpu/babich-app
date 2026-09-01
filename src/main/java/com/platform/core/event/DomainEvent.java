package com.platform.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Абстрактный базовый класс доменного события.
 * Не хранится в БД — это транспортный объект для событийной шины.
 *
 * <p>Сериализация для персистентного event registry Modulith: событие пишется
 * в event_publication в той же транзакции, что и бизнес-изменение, и
 * десериализуется обратно при replay после рестарта. {@code Serializable} —
 * базовый путь (JDK serialization, используется Modulith по умолчанию);
 * Jackson-аннотации покрывают вариант с ObjectMapper, когда он доступен в
 * контексте (Modulith выбирает JSON-сериализацию).</p>
 *
 * <p>{@code eventId} и {@code occurredAt} помечены {@code @JsonIgnore} и не
 * сериализуются: это технические поля транспорта, при десериализации они
 * пересоздаются заново, бизнес-данным не являются.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ElectionClosedEvent.class, name = "ElectionClosedEvent"),
        @JsonSubTypes.Type(value = CoinsChangedEvent.class, name = "CoinsChangedEvent"),
        @JsonSubTypes.Type(value = XpChangedEvent.class, name = "XpChangedEvent"),
        @JsonSubTypes.Type(value = ApplicationResolvedEvent.class, name = "ApplicationResolvedEvent"),
        @JsonSubTypes.Type(value = PollClosedEvent.class, name = "PollClosedEvent")
})
public abstract class DomainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Уникальный идентификатор события (генерируется при создании). */
    private final UUID eventId;

    /** Момент времени, когда событие произошло (UTC, выставляется при создании). */
    private final Instant occurredAt;

    /** Тип доменного события — определяет, какое именно действие произошло. */
    private final EventType type;

    /**
     * Идентификатор актора — сущности (ChatUser), которая инициировала событие.
     * Может быть null для системных событий.
     */
    private final Long actorId;

    /**
     * Идентификатор цели — сущности, над которой выполнено действие.
     * Например, для уведомления о конце выборов — кандидат, получивший должность.
     */
    private final Long targetId;

    /** Тип скоупа — область действия события (район, локация, персонаж и т.п.). */
    private final ScopeType scopeType;

    /** Идентификатор скоупа — конкретного района/локации/персонажа, к которому относится событие. */
    private final Long scopeId;

    /** Произвольные дополнительные данные события в формате ключ-значение (опционально). */
    private final Map<String, Object> payload;

    @JsonCreator
    protected DomainEvent(@JsonProperty("type") EventType type,
                          @JsonProperty("actorId") Long actorId,
                          @JsonProperty("targetId") Long targetId,
                          @JsonProperty("scopeType") ScopeType scopeType,
                          @JsonProperty("scopeId") Long scopeId,
                          @JsonProperty("payload") Map<String, Object> payload) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.type = type;
        this.actorId = actorId;
        this.targetId = targetId;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.payload = payload;
    }

    protected DomainEvent(EventType type, Long actorId, Long targetId,
                          ScopeType scopeType, Long scopeId) {
        this(type, actorId, targetId, scopeType, scopeId, null);
    }

    @JsonIgnore
    public UUID getEventId() { return eventId; }

    @JsonIgnore
    public Instant getOccurredAt() { return occurredAt; }

    public EventType getType() { return type; }
    public Long getActorId() { return actorId; }
    public Long getTargetId() { return targetId; }
    public ScopeType getScopeType() { return scopeType; }
    public Long getScopeId() { return scopeId; }
    public Map<String, Object> getPayload() { return payload; }
}