package com.platform.chat.modules.civic.dto;

import com.platform.chat.modules.civic.enums.ApplicationStatus;
import com.platform.chat.modules.civic.enums.ApplicationType;

import java.time.LocalDateTime;

/**
 * Гражданская заявка для модерации: данные заявки + имя (username)
 * пользователя, который её оставил. Сам Application хранит только userId.
 */
public record ApplicationView(
        Long id,
        ApplicationType type,
        ApplicationStatus status,
        String resultMessage,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        Long userId,
        String applicantUsername
) {
}