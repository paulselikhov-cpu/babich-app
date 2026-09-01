/**
 * Модуль «chat»: игровой чат, пользователи чата, локации.
 *
 * <p>Модуль помечен {@code type = OPEN}: это предсуществующий технический
 * долг границ (см. {@code documentation/architecture/event/event-migration-plan.md},
 * Этап 1.5) — прямые зависимости на {@code auth} (JwtService, User,
 * UserRepository), {@code economy} (Profession) и {@code core}
 * (с Этапа 5: PresenceService реализует {@code core.scheduler.TickHandler}
 * единого SchedulerGateway — фоновая задача вместо собственной
 * {@code @Scheduled}). Типы модуля экспонированы штатным механизмом Modulith,
 * чтобы тест {@code ModuleBoundaryTest} ловил только НОВЫЕ нарушения границ.
 * Когда техдолг будет убран — снять {@code OPEN} и оставить
 * {@code allowedDependencies}.</p>
 */
@ApplicationModule(allowedDependencies = { "auth", "economy", "core" }, type = ApplicationModule.Type.OPEN)
package com.platform.chat;

import org.springframework.modulith.ApplicationModule;