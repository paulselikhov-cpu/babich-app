package com.platform.chat.engines.application.service;

import com.platform.chat.engines.application.entity.Application;
import com.platform.chat.engines.application.enums.ApplicationStatus;
import com.platform.chat.engines.application.enums.ApplicationType;
import com.platform.chat.engines.application.exception.ApplicationAlreadyResolvedException;
import com.platform.chat.engines.application.exception.ApplicationNotFoundException;
import com.platform.chat.engines.application.i18n.ApplicationMessages;
import com.platform.chat.engines.application.model.ApplicationDecision;
import com.platform.chat.engines.application.repository.ApplicationRepository;
import com.platform.chat.engines.domainEvent.service.DomainEventPublisher;
import com.platform.chat.engines.domainEvent.events.ApplicationResolvedEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Ядро процесса заявки: команды (создание, одобрение, отклонение) и публикация
 * процессных событий. Детали обработки делегируются стратегии
 * ({@code ApplicationHandler}) через {@link ApplicationHandlerRegistry} —
 * сервис не знает ни одного конкретного типа заявки.
 *
 * <p>Ответственности разделены так:</p>
 * <ul>
 *   <li>стратегия применяет эффект (выдаёт паспорт/лицензию) и возвращает решение;</li>
 *   <li>сервис — единственная точка перевода заявки в APPROVED/REJECTED: применяет
 *       решение, сохраняет заявку и публикует события — {@code ApplicationResolvedEvent}
 *       (факт процесса) плюс события-последствия из решения (например,
 *       {@code CharacterUpdatedEvent}), в той же транзакции.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationHandlerRegistry handlerRegistry;
    private final DomainEventPublisher eventPublisher;
    private final ApplicationMessages messages;

    /** Отклонить заявку (только PENDING). Вызывается сотрудником мэрии. */
    @Transactional
    public Application rejectApplication(Long applicationId, String reason) {
        Application application = requirePending(applicationId);

        application.setStatus(ApplicationStatus.REJECTED);
        application.setResultMessage(reason);
        application.setProcessedAt(LocalDateTime.now());
        Application rejected = applicationRepository.save(application);

        publishResolved(rejected);

        return rejected;
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

    /** Одобрить заявку: стратегия применяет эффект и возвращает решение,
     *  ядро применяет решение и публикует события. Вызывается сотрудником мэрии
     *  для ручной модерации заявок.
     */
    @Transactional
    public Application approveApplication(Long applicationId) {
        Application application = requirePending(applicationId);

        ApplicationDecision decision = handlerRegistry.get(application.getType())
                .resolve(application);
        applyDecision(application, decision);

        Application resolved = applicationRepository.save(application);

        // События-последствия конкретной стратегии («персонаж изменился» и т.п.)
        // публикует ядро, но не зная их типов: они приходят в решении.
        decision.events().forEach(eventPublisher::publish);
        publishResolved(resolved);

        return resolved;
    }

    public boolean existsPendingApplication(Long characterId, ApplicationType type) {
        return applicationRepository.existsByCharacterIdAndTypeAndStatus(
                characterId, type, ApplicationStatus.PENDING);
    }

    /**
     * Найти заявку и убедиться, что она ещё не решена — общий guard команд
     * approve/reject.
     */
    private Application requirePending(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (application.getStatus() != null && !ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new ApplicationAlreadyResolvedException(application.getStatus());
        }
        return application;
    }

    /**
     * Применить решение стратегии к заявке: статус + человекочитаемый текст
     * (резолв ключа сообщения с аргументами) + время обработки.
     */
    private void applyDecision(Application application, ApplicationDecision decision) {
        application.setStatus(decision.status());
        application.setResultMessage(messages.resolve(decision.messageKey(), decision.args().toArray()));
        application.setProcessedAt(LocalDateTime.now());
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
}
