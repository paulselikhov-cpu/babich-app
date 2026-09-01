package com.platform.chat.core.scheduler;

/**
 * Статус отложенной проверки {@link ScheduledCheck}.
 */
public enum ScheduledCheckStatus {
    /** Проверка ещё не выполнена. */
    PENDING,
    /** Проверка выполнена (обработчик отработал или проверка закрыта). */
    COMPLETED
}