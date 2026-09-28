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
 * Событие разрешения гражданской заявки (APPROVED/REJECTED).
 * Публикуется ApplicationService (ядро процесса) после того, как стратегия
 * применила эффект и вернула решение (resolve), а ядро применило его к заявке
 * и сохранило её.
 * Подписчик (NotificationEventListener) уведомляет заявителя о результате.
 *
 * <p>{@code userId} — ВНЕШНИЙ идентификатор владельца заявки (User.id, поле
 * {@code Application.userId}). {@code characterId} — ВНУТРЕННИЙ ChatUser.id,
 * за которого подана заявка (поле {@code Application.characterId}): адресация
 * уведомления идёт по конкретному персонажу, а не по всем персонажам
 * пользователя.</p>
 *
 * <p>{@code applicationType} и {@code status} — строки (имена enum
 * {@code ApplicationType} / {@code ApplicationStatus}), а не сами enum:
 * событие живёт в {@code core.modules.domainEvent} и не должно зависеть от
 * модуля-исполнителя заявок (границы модулей). Подписчик формирует текст
 * уведомления сам по статусу.</p>
 *
 * <p>Вычисляемые геттеры помечены {@code @JsonIgnore}: при сериализации события
 * в event_publication Modulith данные уходят в payload базового класса, а при
 * replay восстанавливаются через собственный {@code @JsonCreator} (Jackson не
 * наследует creator базового класса) — аналогично {@link CoinsChangedEvent}.
 * Внимание: сериализованный JSON ограничен длиной колонки
 * {@code event_publication.serialized_event} (varchar(255)), поэтому
 * {@code resultMessage} в событие сознательно НЕ входит.</p>
 */
public class ApplicationResolvedEvent extends DomainEvent {

    /**
     * @param userId           внешний id владельца заявки (User.id)
     * @param characterId      внутренний id персонажа (ChatUser.id), за которого подана заявка
     * @param applicationId    id заявки
     * @param applicationType  имя enum ApplicationType (BUY_FIRST_LOCATION, REGISTER_PASSPORT, ...)
     * @param status           итоговый статус (имя enum ApplicationStatus: APPROVED/REJECTED)
     */
    public ApplicationResolvedEvent(Long userId, Long characterId, Long applicationId,
                                    String applicationType, String status) {
        super(
                EventType.APPLICATION_RESOLVED,
                userId,       // actorId — инициатор (владелец заявки, внешний User.id)
                characterId,  // targetId — кому адресован результат (внутренний ChatUser.id)
                ScopeType.CHARACTER,
                characterId,  // scopeId — скоуп «персонаж»
                buildPayload(applicationId, applicationType, status)
        );
    }

    /**
     * Creator для replay из {@code event_publication}: Jackson не наследует
     * {@code @JsonCreator} базового класса, поэтому у каждого факта объявлен
     * свой — он восстанавливает поля конверта, а бизнес-данные лежат в payload.
     */
    @JsonCreator
    protected ApplicationResolvedEvent(@JsonProperty("type") EventType type,
                                       @JsonProperty("actorId") Long actorId,
                                       @JsonProperty("targetId") Long targetId,
                                       @JsonProperty("scopeType") ScopeType scopeType,
                                       @JsonProperty("scopeId") Long scopeId,
                                       @JsonProperty("payload") Map<String, Object> payload) {
        super(type, actorId, targetId, scopeType, scopeId, payload);
    }

    @JsonIgnore
    public Long getUserId() {
        return getActorId();
    }

    /** Внутренний id персонажа (ChatUser.id), чья заявка решена. */
    @JsonIgnore
    public Long getCharacterId() {
        return getScopeId();
    }

    @JsonIgnore
    public Long getApplicationId() {
        return longPayload("applicationId");
    }

    @JsonIgnore
    public String getApplicationType() {
        return (String) getPayload().get("applicationType");
    }

    @JsonIgnore
    public String getStatus() {
        return (String) getPayload().get("status");
    }

    private static Map<String, Object> buildPayload(Long applicationId, String applicationType, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationId", applicationId);
        if (applicationType != null) {
            payload.put("applicationType", applicationType);
        }
        if (status != null) {
            payload.put("status", status);
        }
        return payload;
    }
}
