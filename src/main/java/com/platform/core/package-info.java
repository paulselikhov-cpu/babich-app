/**
 * Модуль «core»: доменные события и Ядро платформы.
 *
 * <p>Модуль помечен {@code type = OPEN}: это предсуществующий технический
 * долг границ (см. {@code documentation/architecture/event/event-migration-plan.md},
 * Этап 1.5) — модули {@code notification} и {@code poll} напрямую используют
 * {@code ElectionClosedEvent} и другие типы событий. Типы модуля экспонированы
 * штатным механизмом Modulith, чтобы тест {@code ModuleBoundaryTest} ловил
 * только НОВЫЕ нарушения границ. Когда техдолг будет убран — снять
 * {@code OPEN} и перейти на named interface для точек входа.</p>
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.platform.core;

import org.springframework.modulith.ApplicationModule;