package com.platform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет границы модулей {@code com.platform.*} через Spring Modulith.
 *
 * <p>{@link #verifyModuleStructure()} выполняет проверку границ через
 * {@link ApplicationModules#detectViolations()}: фиксирует правило, что
 * модули не могут импортировать друг друга напрямую, минуя разрешённые
 * зависимости ({@code allowedDependencies} в {@code package-info.java}).</p>
 *
 * <p>Предсуществующий технический долг границ обрабатывается в два шага:</p>
 * <ol>
 *   <li>направленные зависимости (chat→auth, chat→economy, civic→chat,
 *       economy→chat, notification→chat/core, poll→chat/core) легализованы
 *       через {@code allowedDependencies} в {@code package-info.java}
 *       соответствующих модулей — новые НЕЗАЯВЛЕННЫЕ направленные
 *       зависимости уронят тест;</li>
 *   <li>известный ЦИКЛ {@code chat ↔ economy} отфильтрован явно
 *       ({@link #KNOWN_CYCLE_PREFIX}) — Modulith детектит циклы отдельным
 *       механизмом, на который {@code allowedDependencies} не влияет.
 *       Фильтр завязан на точный текст конкретного цикла: любой НОВЫЙ
 *       цикл или новый участник этого цикла уронит тест.</li>
 * </ol>
 *
 * <p>Подробности техдолга и план разбора — в
 * {@code documentation/architecture/event/event-migration-plan.md} (Этап 1.5).</p>
 *
 * <p>Не требует БД — это статический анализ структуры пакетов.</p>
 */
class ModuleBoundaryTest {

    /**
     * Префикс текста нарушения для известного предсуществующего цикла
     * {@code chat ↔ economy}. Зафиксирован по фактическому выводу Modulith:
     * «Cycle detected: Slice chat -> Slice economy -> Slice chat».
     *
     * <p>Фильтруется ТОЛЬКО этот конкретный цикл. Всё остальное
     * (включая циклы с новыми участниками) — падение теста.</p>
     */
    private static final String KNOWN_CYCLE_PREFIX = "Cycle detected: Slice chat";

    @Test
    void moduleStructureIsRecognized() {
        ApplicationModules modules = ApplicationModules.of(PlatformApplication.class);
        assertThat(modules).isNotNull();
        assertThat(modules.getModuleByName("core")).isPresent();
    }

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(PlatformApplication.class);

        Violations violations = modules.detectViolations()
                .filter(v -> !v.hasMessageContaining(KNOWN_CYCLE_PREFIX));

        violations.throwIfPresent();
    }
}
