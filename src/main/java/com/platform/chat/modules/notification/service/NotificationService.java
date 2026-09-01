package com.platform.chat.modules.notification.service;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.notification.repository.NotificationRepository;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.modules.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Слой 2 (Use Cases). Сервис уведомлений.
 * Пока сохраняет в БД — отправка через WS будет добавлена позже.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatUserRepository chatUserRepository;

    /**
     * Отправить уведомление одному персонажу.
     */
    @Transactional
    public Notification notify(Long recipientCharacterId, String title, String body,
                               String notificationType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setRecipientCharacterId(recipientCharacterId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setNotificationType(notificationType);
        notification.setRelatedEntityId(relatedEntityId);
        return notificationRepository.save(notification);
    }

    /**
     * Отправить уведомление с требованием действия (подтвердить/отказаться).
     */
    @Transactional
    public Notification notifyWithAction(Long recipientCharacterId, String title, String body,
                                         String notificationType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setRecipientCharacterId(recipientCharacterId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setNotificationType(notificationType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRequiresAction(true);
        return notificationRepository.save(notification);
    }

    /**
     * Разослать уведомление всем жителям района.
     */
    @Transactional
    public void notifyDistrict(Long districtId, String title, String body,
                               String notificationType, Long relatedEntityId) {
        List<ChatUser> residents = chatUserRepository.findByDistrictId(districtId);
        for (ChatUser resident : residents) {
            notify(resident.getId(), title, body, notificationType, relatedEntityId);
        }
    }

    /**
     * Получить уведомления персонажа.
     */
    public List<Notification> getNotifications(Long characterId) {
        return notificationRepository.findByRecipientCharacterIdOrderByCreatedAtDesc(characterId);
    }

    /**
     * Отметить как прочитанное.
     */
    @Transactional
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
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