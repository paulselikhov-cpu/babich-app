package com.platform.chat.core.modules.application.dto;

import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationStatus;
import com.platform.chat.core.modules.application.enums.ApplicationType;

import java.time.LocalDateTime;

/**
 * Заявка как данные для внешнего мира: то, что ядро процесса отдаёт наружу
 * вместо JPA-entity. Имени заявителя здесь нет — ядро не знает про {@code auth};
 * DTO модерации добирает его в модуле-исполнителе (см. ApplicationViewAssembler).
 */
public record ApplicationSummary(
        Long id,
        ApplicationType type,
        ApplicationStatus status,
        String resultMessage,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        Long userId,
        Long characterId,
        String payload
) {

    public static ApplicationSummary from(Application application) {
        return new ApplicationSummary(
                application.getId(),
                application.getType(),
                application.getStatus(),
                application.getResultMessage(),
                application.getCreatedAt(),
                application.getProcessedAt(),
                application.getUserId(),
                application.getCharacterId(),
                application.getPayload());
    }
}
