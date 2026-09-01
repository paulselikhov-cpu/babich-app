package com.platform.chat.modules.notification.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Уведомление в "меню уведомлений" (колокольчик в шапке).
 * Слой 3 (Процессы). Хранится в БД для последующей отправки через WS.
 */
@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Кому адресовано (characterId). */
    @Column(name = "recipient_character_id", nullable = false)
    private Long recipientCharacterId;

    /** Заголовок уведомления. */
    @Column(nullable = false)
    private String title;

    /** Текст уведомления. */
    @Column(length = 2000)
    private String body;

    /** Тип уведомления (для клиентской маршрутизации). */
    @Column(name = "notification_type", length = 50)
    private String notificationType;

    /** ID связанной сущности (pollId, campaignId и т.п.). */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /** Прочитано ли. */
    @Column(nullable = false)
    private boolean read = false;

    /** Требуется действие от пользователя (подтверждение). */
    @Column(name = "requires_action", nullable = false)
    private boolean requiresAction = false;

    /** Статус действия (null/ACCEPTED/DECLINED). */
    @Column(name = "action_status", length = 20)
    private String actionStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}