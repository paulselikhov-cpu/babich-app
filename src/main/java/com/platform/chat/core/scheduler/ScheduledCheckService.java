package com.platform.chat.core.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Сервис реестра отложенных проверок {@link ScheduledCheck}.
 *
 * <p>Бизнес-сервисы регистрируют проверку в момент откладывания события
 * (например, Poll создан с {@code endsAt} на 24 часа — приходит вызов
 * {@link #schedule(String, String, Long, Instant)}), а
 * {@link SchedulerGateway} находит просроченные проверки и диспетчеризует
 * их {ScheduledCheckHandler} по типу. После успешной обработки проверка
 * закрывается {@link #markDone(Long)}.</p>
 */
@Service
@RequiredArgsConstructor
public class ScheduledCheckService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCheckService.class);

    private final ScheduledCheckRepository repository;

    /**
     * Зарегистрировать отложенную проверку.
     *
     * @param checkType  тип проверки (совпадает с {@code ScheduledCheckHandler#checkType()})
     * @param entityType тип бизнес-сущности (Poll, Deposit, ...)
     * @param entityId   идентификатор бизнес-сущности
     * @param dueAt      момент срабатывания
     * @return сохранённая проверка
     */
    @Transactional
    public ScheduledCheck schedule(String checkType, String entityType, Long entityId, Instant dueAt) {
        return schedule(checkType, entityType, entityId, dueAt, null);
    }

    /**
     * Зарегистрировать отложенную проверку с аварийным пределом
     * {@code timeoutAt}: если проверка не будет закрыта обработчиком к этому
     * моменту, {@link SchedulerGateway} закроет её принудительно.
     */
    @Transactional
    public ScheduledCheck schedule(String checkType, String entityType, Long entityId,
                                   Instant dueAt, Instant timeoutAt) {
        ScheduledCheck check = new ScheduledCheck();
        check.setCheckType(checkType);
        check.setEntityType(entityType);
        check.setEntityId(entityId);
        check.setDueAt(dueAt);
        check.setTimeoutAt(timeoutAt);
        check.setCreatedAt(Instant.now());
        check.setStatus(ScheduledCheckStatus.PENDING);
        ScheduledCheck saved = repository.save(check);
        log.debug("Scheduled check registered: type={}, entity={}#{}, dueAt={}",
                checkType, entityType, entityId, dueAt);
        return saved;
    }

    /**
     * Отметить проверку выполненной.
     */
    @Transactional
    public void markDone(Long checkId) {
        repository.findById(checkId).ifPresent(check -> {
            check.setStatus(ScheduledCheckStatus.COMPLETED);
            repository.save(check);
            log.debug("Scheduled check completed: id={}, type={}, entity={}#{}",
                    checkId, check.getCheckType(), check.getEntityType(), check.getEntityId());
        });
    }

    /**
     * Закрыть все отложенные проверки заданного типа/сущности.
     * Используется, когда бизнес-действие уже выполнено другим путём
     * (например, Poll закрыт вручную) — отложенная проверка становится
     * неактуальной.
     *
     * @return количество закрытых проверок
     */
    @Transactional
    public int cancelPending(String checkType, String entityType, Long entityId) {
        List<ScheduledCheck> pending = repository
                .findByCheckTypeAndEntityTypeAndEntityIdAndStatus(
                        checkType, entityType, entityId, ScheduledCheckStatus.PENDING);
        for (ScheduledCheck check : pending) {
            check.setStatus(ScheduledCheckStatus.COMPLETED);
            repository.save(check);
        }
        if (!pending.isEmpty()) {
            log.debug("Cancelled {} pending scheduled check(s): type={}, entity={}#{}",
                    pending.size(), checkType, entityType, entityId);
        }
        return pending.size();
    }
}
