package com.platform.chat.core.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Репозиторий отложенных проверок {@link ScheduledCheck}.
 */
@Repository
public interface ScheduledCheckRepository extends JpaRepository<ScheduledCheck, Long> {

    /**
     * Найти просроченные (dueAt <= now) и ещё не выполненные проверки.
     *
     * @param status  статус, по которому фильтруем (обычно PENDING)
     * @param now     текущий момент времени
     * @return список просроченных проверок, отсортированный по dueAt
     */
    List<ScheduledCheck> findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(
            ScheduledCheckStatus status, Instant now);

    /**
     * Найти непогашенные проверки конкретного типа для конкретной сущности.
     * Используется, чтобы закрыть отложенные проверки, когда бизнес-действие
     * уже выполнено другим путём (например, Poll закрыт вручную).
     */
    List<ScheduledCheck> findByCheckTypeAndEntityTypeAndEntityIdAndStatus(
            String checkType, String entityType, Long entityId, ScheduledCheckStatus status);
}
