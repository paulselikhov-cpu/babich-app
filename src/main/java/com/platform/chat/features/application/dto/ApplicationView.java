package com.platform.chat.features.application.dto;

import com.platform.chat.engines.application.dto.ApplicationSummary;
import com.platform.chat.engines.application.enums.ApplicationStatus;
import com.platform.chat.engines.application.enums.ApplicationType;

import java.time.LocalDateTime;

/**
 * Гражданская заявка для модерации: данные заявки + имя (username) заявителя.
 * Поля повторяют прежний контракт {@code /api/applications} (фронт читает
 * {@code id}, {@code type}, {@code status}, {@code resultMessage},
 * {@code userId}, {@code applicantUsername}).
 *
 * <p>Живёт в модуле-исполнителе, а не в ядре: имя заявителя берётся из
 * {@code auth.User}, про который ядро процесса не знает (см.
 * ApplicationViewAssembler).</p>
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

    public static ApplicationView from(ApplicationSummary summary, String applicantUsername) {
        return new ApplicationView(
                summary.id(),
                summary.type(),
                summary.status(),
                summary.resultMessage(),
                summary.createdAt(),
                summary.processedAt(),
                summary.userId(),
                applicantUsername);
    }
}
