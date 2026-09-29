package com.platform.chat.engines.application.entity;

import com.platform.chat.engines.application.enums.ApplicationStatus;
import com.platform.chat.engines.application.enums.ApplicationType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Гражданская заявка. Обрабатывается соответствующим ApplicationHandler
 * (стратегия по ApplicationType) — см. ApplicationService.
 * В MVP большинство заявок обрабатываются мгновенно (автоодобрение),
 * но структура таблицы уже готова под ручную модерацию в будущем.
 */
@Data
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Внутренний id персонажа (ChatUser.id), за которого подана заявка.
     * Отличается от {@code userId} (внешний User.id): у пользователя может быть
     * несколько персонажей (по одному на район), и обработка/уведомление
     * адресуются именно тому персонажу, чья заявка подана.
     */
    @Column(name = "character_id")
    private Long characterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApplicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /**
     * Произвольные данные заявки (например id выбранного шаблона локации).
     * Хранится как обычный текст — простая сериализация без JSONB
     * для совместимости на этапе MVP.
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * Комментарий/причина отказа или результат обработки.
     */
    @Column(name = "result_message")
    private String resultMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
