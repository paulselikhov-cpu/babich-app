package com.platform.chat.engines.application.model;

import com.platform.chat.engines.application.entity.Application;
import com.platform.chat.engines.application.enums.ApplicationType;

/**
 * Стратегия обработки заявки конкретного типа.
 * Новый тип заявки = новая реализация этого интерфейса,
 * без изменения ApplicationService (Open/Closed principle).
 *
 * <p><b>Контракт:</b></p>
 * <ul>
 *   <li>реализация сама применяет эффект к своему модулю (например, выдаёт
 *       паспорт через {@code ChatUserService}): эффект обязан произойти
 *       в той же транзакции, что и решение заявки, поэтому это синхронный
 *       вызов, а не событие;</li>
 *   <li>реализация НЕ меняет и НЕ сохраняет {@code Application}: решение
 *       применяет ядро ({@code ApplicationService}) — единственная точка
 *       перевода заявки в APPROVED/REJECTED;</li>
 *   <li>реализация НЕ публикует события сама: события об изменениях в чужих
 *       сущностях (например, {@code CharacterUpdatedEvent}) возвращаются в
 *       {@link ApplicationDecision#events()} и публикуются ядром; процессное
 *       событие {@code ApplicationResolvedEvent} публикует только ядро.</li>
 * </ul>
 */
public interface ApplicationHandler {

    ApplicationType getType();

    /**
     * Принимает решение по заявке и применяет его эффект к своему модулю.
     *
     * @param application заявка в статусе PENDING
     * @return решение: статус + ключ текста результата с аргументами + события-последствия
     */
    ApplicationDecision resolve(Application application);
}
