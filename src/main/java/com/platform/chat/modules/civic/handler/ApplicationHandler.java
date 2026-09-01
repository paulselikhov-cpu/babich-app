package com.platform.civic.handler;

import com.platform.civic.entity.Application;
import com.platform.civic.enums.ApplicationType;

/**
 * Стратегия обработки заявки конкретного типа.
 * Новый тип заявки = новая реализация этого интерфейса,
 * без изменения ApplicationService (Open/Closed principle).
 */
public interface ApplicationHandler {

    ApplicationType getType();

    /**
     * Обрабатывает заявку. Реализация сама решает — одобрить (APPROVED)
     * или отклонить (REJECTED), и выставляет resultMessage.
     * Метод обязан сам изменить статус и resultMessage на переданном
     * объекте Application — сохранение делает вызывающий сервис.
     */
    void handle(Application application);
}
