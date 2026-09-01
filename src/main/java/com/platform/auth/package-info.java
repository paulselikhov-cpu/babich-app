/**
 * Модуль «auth»: аутентификация и авторизация (пользователи, JWT, роли).
 *
 * <p>Модуль помечен {@code type = OPEN}: это предсуществующий технический
 * долг границ (см. {@code documentation/architecture/event/event-migration-plan.md},
 * Этап 1.5) — модуль {@code chat} напрямую использует {@code JwtService},
 * {@code User}, {@code UserRepository}. Типы модуля экспонированы штатным
 * механизмом Modulith, чтобы тест {@code ModuleBoundaryTest} ловил только
 * НОВЫЕ нарушения границ. Когда техдолг будет убран — снять {@code OPEN}
 * и перейти на named interface для точек входа.</p>
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.platform.auth;

import org.springframework.modulith.ApplicationModule;