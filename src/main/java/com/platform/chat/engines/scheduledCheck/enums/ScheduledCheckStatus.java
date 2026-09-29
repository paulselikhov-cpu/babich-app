package com.platform.chat.engines.scheduledCheck.enums;

import com.platform.chat.engines.scheduledCheck.entity.ScheduledCheck;

/**
 * Статус отложенной проверки {@link ScheduledCheck}.
 */
public enum ScheduledCheckStatus {
    /** Проверка ещё не выполнена. */
    PENDING,
    /** Проверка выполнена (обработчик отработал или проверка закрыта). */
    COMPLETED
}