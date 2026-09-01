/**
 * Модуль «civic»: гражданские механики (выборы, депутаты, инициативы).
 *
 * <p>Легализованный предсуществующий технический долг границ (см.
 * {@code documentation/architecture/event/event-migration-plan.md}, Этап 1.5):
 * прямая зависимость на {@code chat} (ChatUser, ChatUserService,
 * ChatUserRepository, Location*, LocationService, LocationTemplate*).
 * Зависимость разрешена БЕЗ named interface — это осознанная запись
 * существующей связи, чтобы тест {@code ModuleBoundaryTest} ловил только
 * НОВЫЕ нарушения границ.</p>
 *
 * <p>Зависимость на {@code core} — публикация доменных событий
 * (ApplicationService → DomainEventPublisher, ApplicationResolvedEvent)
 * в рамках миграции на событийную шину (этап 4). Модуль core помечен
 * {@code OPEN}, поэтому цикл civic ⇄ core не детектится Modulith —
 * это тот же паттерн, что у существующего цикла economy ⇄ core.</p>
 */
@ApplicationModule(allowedDependencies = { "chat", "core" })
package com.platform.chat.modules.civic;

import org.springframework.modulith.ApplicationModule;