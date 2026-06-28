package com.platform.chat.entity;

import com.platform.chat.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Сообщение в комнате чата.
 * Сохраняется в БД только если Room.storeHistory = true.
 * Если storeHistory = false — сообщение летит только через WebSocket
 * и нигде не хранится.
 *
 * Индексы по room_id и created_at критичны для производительности —
 * при загрузке истории чата всегда запрашиваем по комнате + сортируем по времени.
 */
@Data
@Entity
@Table(name = "messages",
        indexes = {
                // индекс для быстрой загрузки истории комнаты
                @Index(name = "idx_messages_room_created",
                        columnList = "room_id, created_at")
        }
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Комната в которой отправлено сообщение.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * Персонаж который отправил сообщение.
     * null = системное сообщение (юзер вошёл/вышел).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private ChatUser character;

    /**
     * Текст сообщения.
     * TEXT в PostgreSQL = без ограничения длины.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Тип сообщения:
     * CHAT = обычное текстовое сообщение
     * ACTION = действие персонажа (/me идёт на кухню)
     * SYSTEM = системное (Иван вошёл в комнату)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.CHAT;

    /**
     * Мягкое удаление (soft delete).
     * true = сообщение удалено, но запись в БД остаётся.
     * Вместо текста показываем "Сообщение удалено".
     * Это лучше чем физическое удаление — сохраняется история,
     * можно восстановить если нужно.
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}