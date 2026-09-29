package com.platform.chat.engines.notification.service;

import com.platform.chat.engines.notification.repository.NotificationRepository;
import com.platform.chat.engines.notification.entity.Notification;
import com.platform.chat.engines.notification.dto.NotificationContextData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Слой 2 (Use Cases). Сервис уведомлений.
 * Пока сохраняет в БД — отправка через WS будет добавлена позже.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Отправить уведомление одному персонажу (без контекстных данных).
     */
    @Transactional
    public Notification notify(Long recipientCharacterId, String title, String body,
                               String notificationType, Long relatedEntityId) {
        return notify(recipientCharacterId, title, body, notificationType, relatedEntityId, null);
    }

    /**
     * Отправить уведомление одному персонажу со структурированными контекстными
     * данными (сохраняются в поле {@code data} и возвращаются клиенту и по WS,
     * и по REST-истории).
     */
    @Transactional
    public Notification notify(Long recipientCharacterId, String title, String body,
                               String notificationType, Long relatedEntityId,
                               Map<String, Object> data) {
        Notification notification = new Notification();
        notification.setRecipientCharacterId(recipientCharacterId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setNotificationType(notificationType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setData(NotificationContextData.toText(data));
        return notificationRepository.save(notification);
    }

    /**
     * Получить уведомления персонажа.
     */
    public List<Notification> getNotifications(Long characterId) {
        return notificationRepository.findByRecipientCharacterIdOrderByCreatedAtDesc(characterId);
    }

    /**
     * Количество непрочитанных уведомлений персонажа.
     */
    public long countUnread(Long characterId) {
        return notificationRepository.countByRecipientCharacterIdAndReadFalse(characterId);
    }

    /**
     * Отметить как прочитанное. Возвращает обновлённое уведомление
     * (null — если не найдено).
     */
    @Transactional
    public Notification markRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(n -> {
                    n.setRead(true);
                    return notificationRepository.save(n);
                })
                .orElse(null);
    }

    /**
     * Подтвердить действие по уведомлению.
     */
    @Transactional
    public Notification acceptAction(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Уведомление не найдено: " + notificationId));
        if (!notification.isRequiresAction()) {
            throw new IllegalStateException("Уведомление не требует действия");
        }
        notification.setActionStatus("ACCEPTED");
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    /**
     * Отказаться от действия по уведомлению.
     */
    @Transactional
    public Notification declineAction(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Уведомление не найдено: " + notificationId));
        if (!notification.isRequiresAction()) {
            throw new IllegalStateException("Уведомление не требует действия");
        }
        notification.setActionStatus("DECLINED");
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}