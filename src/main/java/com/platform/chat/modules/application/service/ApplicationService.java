package com.platform.chat.modules.application.service;

import com.platform.chat.modules.application.entity.Application;
import com.platform.chat.modules.application.enums.ApplicationStatus;
import com.platform.chat.modules.application.enums.ApplicationType;
import com.platform.chat.modules.application.handler.ApplicationHandler;
import com.platform.chat.modules.application.repository.ApplicationRepository;
import com.platform.chat.core.event.ApplicationResolvedEvent;
import com.platform.chat.core.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Диспетчер обработки заявок. Находит подходящий ApplicationHandler
 * по типу заявки (стратегия) и делегирует ему бизнес-логику.
 * Сам сервис не знает деталей — просто создаёт заявку, вызывает
 * обработчик и сохраняет результат.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final List<ApplicationHandler> handlers;
    private final DomainEventPublisher eventPublisher;

    private Map<ApplicationType, ApplicationHandler> handlerMap;

    private Map<ApplicationType, ApplicationHandler> getHandlerMap() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.toMap(ApplicationHandler::getType, h -> h));
        }
        return handlerMap;
    }

    @Transactional
    public Application submit(Long userId, ApplicationType type, String payload) {
        Application application = new Application();
        application.setUserId(userId);
        application.setType(type);
        application.setPayload(payload);
        applicationRepository.save(application);

        ApplicationHandler handler = getHandlerMap().get(type);
        if (handler == null) {
            throw new RuntimeException("Нет обработчика для типа заявки: " + type);
        }

        handler.handle(application);
        application.setProcessedAt(LocalDateTime.now());
        Application resolved = applicationRepository.save(application);

        eventPublisher.publish(new ApplicationResolvedEvent(
                resolved.getUserId(),
                resolved.getId(),
                resolved.getStatus() != null ? resolved.getStatus().name() : null
        ));

        return resolved;
    }

    public List<Application> getMyApplications(Long userId) {
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Отклонить заявку (только PENDING). Вызывается сотрудником мэрии. */
    @Transactional
    public Application rejectApplication(Long applicationId, String reason) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена: " + applicationId));

        if (application.getStatus() != null && !application.getStatus().equals(ApplicationStatus.PENDING)) {
            throw new RuntimeException("Заявка уже обработана, статус: " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setResultMessage(reason);
        application.setProcessedAt(LocalDateTime.now());
        Application rejected = applicationRepository.save(application);

        eventPublisher.publish(new ApplicationResolvedEvent(
                rejected.getUserId(),
                rejected.getId(),
                rejected.getStatus() != null ? rejected.getStatus().name() : null
        ));

        return rejected;
    }

    /** Все заявки платформы (свежие сверху) — для модерации. */
    public List<Application> getAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Создать заявку в статусе PENDING без вызова handler-а.
     *  Подходит для заявок, которые будут рассмотрены вручную сотрудником мэрии
     *  через {@link #approveApplication(Long)}.
     */
    @Transactional
    public Application createPendingApplication(Long userId, ApplicationType type, String payload) {
        Application application = new Application();
        application.setUserId(userId);
        application.setType(type);
        application.setPayload(payload);
        // status остаётся PENDING (значение по умолчанию в entity)
        Application pending = applicationRepository.save(application);
        eventPublisher.publish(new ApplicationResolvedEvent(
                pending.getUserId(),
                pending.getId(),
                pending.getStatus() != null ? pending.getStatus().name() : null
        ));
        return pending;
    }

    /** Одобрить заявку (передать handler-у для обработки).
     *  Handler сам решает — одобрить или отклонить, и выставляет resultMessage.
     *  Используется сотрудником мэрии для ручной модерации заявок.
     */
    @Transactional
    public Application approveApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена: " + applicationId));

        if (application.getStatus() != null && !application.getStatus().equals(ApplicationStatus.PENDING)) {
            throw new RuntimeException("Заявка уже обработана, статус: " + application.getStatus());
        }

        ApplicationHandler handler = getHandlerMap().get(application.getType());
        if (handler == null) {
            throw new RuntimeException("Нет обработчика для типа заявки: " + application.getType());
        }

        handler.handle(application);
        application.setProcessedAt(LocalDateTime.now());
        Application resolved = applicationRepository.save(application);

        eventPublisher.publish(new ApplicationResolvedEvent(
                resolved.getUserId(),
                resolved.getId(),
                resolved.getStatus() != null ? resolved.getStatus().name() : null
        ));

        return resolved;
    }

    public Optional<Application> findById(Long id) {
        return applicationRepository.findById(id);
    }

    public boolean existsPendingApplication(Long userId, ApplicationType type) {
        return applicationRepository.existsByUserIdAndTypeAndStatus(
                userId, type, ApplicationStatus.PENDING);
    }
}
