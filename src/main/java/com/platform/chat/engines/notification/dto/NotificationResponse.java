package com.platform.chat.engines.notification.dto;

import com.platform.chat.engines.notification.entity.Notification;
import java.time.Instant;
import java.util.Map;

/** DTO для WS-пуша в /user/{username}/queue/notifications. */
public record NotificationResponse(
        Long id,
        Long recipientCharacterId,
        String title,
        String body,
        String notificationType,
        Long relatedEntityId,
        boolean read,
        boolean requiresAction,
        String actionStatus,
        Instant createdAt,
        Map<String, Object> data
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getRecipientCharacterId(), n.getTitle(), n.getBody(),
                n.getNotificationType(), n.getRelatedEntityId(), n.isRead(),
                n.isRequiresAction(), n.getActionStatus(), n.getCreatedAt(),
                NotificationContextData.parse(n.getData()));
    }
}