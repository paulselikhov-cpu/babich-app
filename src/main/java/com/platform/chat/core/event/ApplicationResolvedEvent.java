package com.platform.core.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Событие разрешения гражданской заявки (APPROVED/REJECTED).
 * Публикуется ApplicationService после handler.handle() + save.
 * Подписчик (NotificationEventListener) уведомляет заявителя о результате.
 *
 * <p>{@code userId} — ВНЕШНИЙ идентификатор владельца заявки (User.id, поле
 * {@code Application.userId}), а не внутренний ChatUser.id: заявка может быть
 * подана за персонажа, но обработчик/подписчик ищут заявителя по внешнему
 * id пользователя через {@code ChatUserRepository.findAllByUserId(...)}.</p>
 *
 * <p>{@code status} — строка (имя enum {@code ApplicationStatus}: APPROVED/
 * REJECTED), а не сам enum: событие живёт в {@code core.event} и не должно
 * зависеть от модуля civic (границы модулей). Подписчик формирует текст
 * уведомления сам по статусу.</p>
 *
 * <p>Вычисляемые геттеры {@code getApplicationId()}, {@code getStatus()}
 * помечены {@code @JsonIgnore}: при сериализации события в event_publication
 * Modulith данные уходят в payload базового класса и при replay
 * восстанавливаются через конструктор {@link DomainEvent} — аналогично
 * {@link CoinsChangedEvent}.</p>
 */
public class ApplicationResolvedEvent extends DomainEvent {

    /**
     * @param userId         внешний id владельца заявки (User.id)
     * @param applicationId  id заявки
     * @param status         итоговый статус (имя enum ApplicationStatus: APPROVED/REJECTED)
     */
    public ApplicationResolvedEvent(Long userId, Long applicationId, String status) {
        super(
                EventType.APPLICATION_RESOLVED,
                userId, // actorId — инициатор (владелец заявки)
                userId, // targetId — кому адресован результат
                ScopeType.CHARACTER,
                userId,
                buildPayload(applicationId, status)
        );
    }

    @JsonIgnore
    public Long getUserId() {
        return getActorId();
    }

    @JsonIgnore
    public Long getApplicationId() {
        return (Long) getPayload().get("applicationId");
    }

    @JsonIgnore
    public String getStatus() {
        return (String) getPayload().get("status");
    }

    private static Map<String, Object> buildPayload(Long applicationId, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationId", applicationId);
        if (status != null) {
            payload.put("status", status);
        }
        return payload;
    }
}