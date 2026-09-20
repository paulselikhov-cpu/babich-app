package com.platform.chat.core.modules.application.service;

import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.models.ApplicationStatus;
import com.platform.chat.core.modules.domainEvent.service.DomainEventPublisher;
import com.platform.chat.core.modules.application.models.ApplicationHandler;
import com.platform.chat.core.modules.application.repository.ApplicationRepository;

import com.platform.chat.modules.domainEvent.events.ApplicationResolvedEvent;
import com.platform.chat.modules.domainEvent.events.CharacterUpdatedEvent;
import com.platform.chat.modules.application.ApplicationType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

        publishResolved(rejected);

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
    public Application createPendingApplication(Long userId, Long characterId, ApplicationType type, String payload) {
        Application application = new Application();
        application.setUserId(userId);
        application.setCharacterId(characterId);
        application.setType(type);
        application.setPayload(payload);
        // status остаётся PENDING (значение по умолчанию в entity).
        // Событие ApplicationResolvedEvent на подаче НЕ публикуем: его смысл —
        // «заявка РЕШЕНА» (APPROVED/REJECTED), а подача к этому не относится.
        // Пользователь и так видит тост «Заявка подана» на фронте.
        return applicationRepository.save(application);
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

        publishResolved(resolved);
        publishCharacterUpdatedIfApproved(resolved);

        return resolved;
    }

    public boolean existsPendingApplication(Long characterId, ApplicationType type) {
        return applicationRepository.existsByCharacterIdAndTypeAndStatus(
                characterId, type, ApplicationStatus.PENDING);
    }

    /** Опубликовать факт «заявка решена» (несущий applicationType + status). */
    private void publishResolved(Application resolved) {
        eventPublisher.publish(new ApplicationResolvedEvent(
                resolved.getUserId(),
                resolved.getCharacterId(),
                resolved.getId(),
                resolved.getType() != null ? resolved.getType().name() : null,
                resolved.getStatus() != null ? resolved.getStatus().name() : null
        ));
    }

    /**
     * «Персонаж изменился» — только при одобрении: именно оно меняет ChatUser
     * (паспорт/лицензия). Отклонение состояния персонажа не меняет, поэтому
     * событие не публикуем. Фронт по нему перезагружает currentChatUser.
     */
    private void publishCharacterUpdatedIfApproved(Application resolved) {
        if (ApplicationStatus.APPROVED.equals(resolved.getStatus())
                && resolved.getCharacterId() != null) {
            eventPublisher.publish(new CharacterUpdatedEvent(resolved.getCharacterId()));
        }
    }
}
