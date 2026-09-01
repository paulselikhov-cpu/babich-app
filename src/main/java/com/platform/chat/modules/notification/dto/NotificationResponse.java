package com.platform.notification.dto;

import com.platform.notification.entity.Notification;
import java.time.Instant;

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
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getRecipientCharacterId(), n.getTitle(), n.getBody(),
                n.getNotificationType(), n.getRelatedEntityId(), n.isRead(),
                n.isRequiresAction(), n.getActionStatus(), n.getCreatedAt());
    }
}