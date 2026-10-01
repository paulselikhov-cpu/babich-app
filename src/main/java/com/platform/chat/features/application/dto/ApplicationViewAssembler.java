package com.platform.chat.features.application.dto;

import com.platform.auth.service.UserService;
import com.platform.chat.engines.application.dto.ApplicationSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Собирает DTO модерации: заявки + имя заявителя.
 *
 * <p>Имя берётся через публичный API модуля {@code auth}
 * ({@link UserService#getUsernamesByIds}) — одним запросом на всю пачку, без
 * N+1 на {@code /api/applications/all}. Прямой доступ в чужой репозиторий
 * модулю заявок не нужен.</p>
 */
@Component
@RequiredArgsConstructor
public class ApplicationViewAssembler {

    private final UserService userService;

    public List<ApplicationView> assemble(List<ApplicationSummary> applications) {
        if (applications.isEmpty()) {
            return List.of();
        }

        Map<Long, String> usernamesById = userService.getUsernamesByIds(
                applications.stream()
                        .map(ApplicationSummary::userId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList());

        return applications.stream()
                .map(application -> ApplicationView.from(application, usernamesById.get(application.userId())))
                .toList();
    }
}
