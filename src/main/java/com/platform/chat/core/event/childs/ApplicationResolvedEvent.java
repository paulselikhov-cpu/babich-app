package com.platform.chat.core.event.childs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.core.event.base.DomainEvent;
import com.platform.chat.core.event.base.EventType;
import com.platform.chat.core.event.base.ScopeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие разрешения гражданской заявки (APPROVED/REJECTED).
 * Публикуется ApplicationService после handler.handle() + save.
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
 * событие живёт в {@code core.event} и не должно зависеть от модуля civic
 * (границы модулей). Подписчик формирует текст уведомления сам по статусу.</p>
 *
 * <p>Вычисляемые геттеры помечены {@code @JsonIgnore}: при сериализации события
 * в event_publication Modulith данные уходят в payload базового класса и при
 * replay восстанавливаются через конструктор {@link DomainEvent} — аналогично
 * {@link CoinsChangedEvent}. Внимание: сериализованный JSON ограничен длиной
 * колонки {@code event_publication.serialized_event} (varchar(255)), поэтому
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
        return (Long) getPayload().get("applicationId");
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