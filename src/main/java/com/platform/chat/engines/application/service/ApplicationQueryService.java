package com.platform.chat.engines.application.service;

import com.platform.chat.engines.application.dto.ApplicationSummary;
import com.platform.chat.engines.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Чтения по заявкам (read-сторона процесса), отдельно от команд
 * ({@link ApplicationService}): у них разные зависимости и разные потребители —
 * команды ходят к стратегиям и публикуют события, чтения только читают репозиторий.
 */
@Service
@RequiredArgsConstructor
public class ApplicationQueryService {

    private final ApplicationRepository applicationRepository;

    /** Заявки пользователя (внешний User.id), свежие сверху. */
    public List<ApplicationSummary> getMyApplications(Long userId) {
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ApplicationSummary::from)
                .toList();
    }

    /** Все заявки платформы (для модерации), свежие сверху. */
    public List<ApplicationSummary> getAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ApplicationSummary::from)
                .toList();
    }
}
