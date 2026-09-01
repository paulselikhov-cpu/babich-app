/**
 * Модуль «economy»: экономика (профессии, зарплаты, балансы).
 *
 * <p>Модуль помечен {@code type = OPEN}: это предсуществующий технический
 * долг границ (см. {@code documentation/architecture/event/event-migration-plan.md},
 * Этап 1.5) — прямая зависимость на {@code chat} (ChatUserService,
 * ChatUser, ChatUserRepository) и на {@code core} (этап 3: доменные события
 * CoinsChangedEvent/XpChangedEvent и обёртка DomainEventPublisher вынесены в
 * {@code core.event}, economy их публикует и слушает через public API).
 * Типы модуля экспонированы штатным механизмом Modulith, чтобы тест
 * {@code ModuleBoundaryTest} ловил только НОВЫЕ нарушения границ. Обратная
 * зависимость {@code chat → economy} (Profession) тоже существует — они
 * образуют известный цикл {@code chat ↔ economy}, который явно отфильтрован
 * в {@code ModuleBoundaryTest} как технический долг. Когда техдолг будет
 * убран — снять {@code OPEN} и оставить {@code allowedDependencies}.</p>
 */
@ApplicationModule(allowedDependencies = { "chat", "core" }, type = ApplicationModule.Type.OPEN)
package com.platform.economy;

import org.springframework.modulith.ApplicationModule;
