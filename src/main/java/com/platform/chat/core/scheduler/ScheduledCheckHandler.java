package com.platform.chat.core.scheduler;

/**
 * Обработчик отложенных проверок ({@link ScheduledCheck}) единого планировщика.
 *
 * <p>Слой 4 (автоматизация). Реализация обрабатывает строку из реестра
 * {@code scheduled_checks}: Spring собирает {@code List<ScheduledCheckHandler>},
 * а {@link SchedulerGateway} диспетчеризует проверку обработчику по
 * {@link #checkType()}.</p>
 */
public interface ScheduledCheckHandler {

    /**
     * Тип проверки, который обрабатывает эта реализация.
     * Значение должно совпадать с {@code ScheduledCheck.checkType}.
     */
    String checkType();

    /**
     * Выполнить бизнес-действие для отложенной проверки.
     * Исключения перехватываются gateway'ем (логируются, проверка не
     * закрывается и будет перепробована на следующем тике).
     *
     * @param check отложенная проверка
     */
    void handle(ScheduledCheck check);
}