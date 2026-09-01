package com.platform.core.scheduler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * Реестр отложенных проверок единого планировщика.
 *
 * <p>Заменяет разрозненные «пройти по всем записям таблицы и найти
 * просроченные» на явную запись «что и когда проверить»: один индексный
 * запрос по статусу + dueAt вместо N полных сканов. Сущность —
 * инфраструктурная, не игровая; живёт в {@code core.scheduler}, чтобы
 * планировщик не зависел от бизнес-модулей.</p>
 */
@Data
@Entity
@Table(name = "scheduled_checks")
public class ScheduledCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Тип проверки — псевдоним обработчика (ScheduledCheckHandler#checkType()). */
    @Column(name = "check_type", nullable = false, length = 50)
    private String checkType;

    /** Тип бизнес-сущности, к которой относится проверка (Poll, Deposit, ...). */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** Идентификатор бизнес-сущности. */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** Момент, когда проверка должна сработать. */
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    /** Аварийный предел — проверка завершается после этой даты, если обработчик не отработал. */
    @Column(name = "timeout_at")
    private Instant timeoutAt;

    /** Статус: PENDING/COMPLETED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledCheckStatus status = ScheduledCheckStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}