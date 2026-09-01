/**
 * Модуль «poll»: голосования, выборы, опросы.
 *
 * <p>Легализованный предсуществующий технический долг границ (см.
 * {@code documentation/architecture/event/event-migration-plan.md}, Этап 1.5):
 * прямые зависимости на {@code chat} (ChatUser, ElectionFacade пути) и на
 * {@code core} (DomainEvent/EventType). Обе разрешены БЕЗ named interface —
 * это осознанная запись существующих связей, чтобы тест
 * {@code ModuleBoundaryTest} ловил только НОВЫЕ нарушения границ.</p>
 */
@ApplicationModule(allowedDependencies = { "chat", "core" })
package com.platform.poll;

import org.springframework.modulith.ApplicationModule;