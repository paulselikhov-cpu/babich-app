package com.platform.chat.core.modules.scheduledCheck.models;

import com.platform.chat.core.modules.scheduledCheck.service.SchedulerGateway;

/**
 * Периодическая задача единого планировщика {@link SchedulerGateway}.
 *
 * <p>Каждая фоновая задача вместо собственной {@code @Scheduled}-аннотации
 * реализует этот интерфейс — Spring собирает {@code List<TickHandler>}, а
 * {@link SchedulerGateway} (единственный {@code @Scheduled} в приложении)
 * вызывает {@link #onTick()} у всех зарегистрированных обработчиков.</p>
 *
 * <p>Интервал по умолчанию — 60 секунд; обработчик может переопределить
 * {@link #intervalMillis()}, если ему нужен другой период (например,
 * presence-cleanup — 15 секунд).</p>
 */
public interface TickHandler {

    /**
     * Выполнить периодическую фоновую работу.
     * Исключения перехватываются gateway'ем (логируются, не роняют остальные тики).
     */
    void onTick();

    /**
     * Период срабатывания обработчика в миллисекундах.
     * По умолчанию 60 секунд.
     */
    default long intervalMillis() {
        return 60_000L;
    }
}