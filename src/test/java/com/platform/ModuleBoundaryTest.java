package com.platform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет границы модулей {@code com.platform.*} через Spring Modulith.
 *
 * <p>{@link #verifyModuleStructure()} выполняет проверку границ через
 * {@link ApplicationModules#detectViolations()}: фиксирует правило, что
 * модули не могут импортировать друг друга напрямую, минуя разрешённые
 * зависимости ({@code allowedDependencies} в {@code package-info.java}).</p>
 *
 * <p>Проверка идёт в двух плоскостях — одной Modulith-проверки мало:</p>
 * <ol>
 *   <li>{@link #verifyModuleStructure()} — байткодный анализ Modulith
 *       ({@code detectViolations()}): незаявленные направленные зависимости
 *       роняют тест. Исключающих фильтров больше нет: цикл, который раньше
 *       глушился вручную, устранён, и любой новый цикл должен падать;</li>
 *   <li>{@link #enginesMustNotImportFeatures()} и
 *       {@link #domainMustNotImportEnginesOrFeatures()} — проверка
 *       <b>исходников</b>. Нужна потому, что {@code public static final
 *       String}-константы javac инлайнит: импорт вида
 *       {@code chat.features.…ElectionClose} не оставляет ссылки в байткоде,
 *       и Modulith такую зависимость не видит (так проходил цикл
 *       {@code engines.poll / features.governorElection}).</li>
 * </ol>
 *
 * <p>Правило слоёв: {@code domain} не знает про {@code engines} и
 * {@code features}, {@code engines} не знает про {@code features}.</p>
 *
 * <p>Границы слайсов доменных событий (ядро шины ↔ реакции) описаны в
 * {@code documentation/architecture/event/domain-event-module-layers.md}.</p>
 *
 * <p>Не требует БД — это статический анализ структуры пакетов и исходников.</p>
 */
class ModuleBoundaryTest {

    /** Корень исходников chat-слоёв (тесты запускаются из каталога {@code babich-app}). */
    private static final Path CHAT_SOURCE_ROOT =
            Path.of("src", "main", "java", "com", "platform", "chat");

    @Test
    void moduleStructureIsRecognized() {
        ApplicationModules modules = ApplicationModules.of(PlatformApplication.class);

        assertThat(modules).isNotNull();
        assertThat(modules.getModuleByName("chat.domain.chatUser")).isPresent();
    }

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(PlatformApplication.class);

        // Без фильтров: исключений из правил границ быть не должно.
        modules.detectViolations().throwIfPresent();
    }

    /**
     * Слой движков не имеет права знать про прикладные фичи.
     */
    @Test
    void enginesMustNotImportFeatures() throws IOException {
        assertNoImports(CHAT_SOURCE_ROOT.resolve("engines"), "com.platform.chat.features.");
    }

    /**
     * Онтология мира ({@code domain}) не зависит ни от движков, ни от фич.
     */
    @Test
    void domainMustNotImportEnginesOrFeatures() throws IOException {
        assertNoImports(CHAT_SOURCE_ROOT.resolve("domain"), "com.platform.chat.engines.");
        assertNoImports(CHAT_SOURCE_ROOT.resolve("domain"), "com.platform.chat.features.");
    }

    /**
     * Ищет в исходниках каталога {@code root} импорты с запрещённым префиксом.
     * Проверяется текст файла, а не байткод: константы времени компиляции
     * javac инлайнит, поэтому в байткоде от такой зависимости не остаётся следа
     * и Modulith её не видит.
     */
    private void assertNoImports(Path root, String forbiddenPrefix) throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).trim().startsWith("import " + forbiddenPrefix)) {
                        violations.add(file + ":" + (i + 1) + " -> " + lines.get(i).trim());
                    }
                }
            }
        }

        assertThat(violations)
                .describedAs("Слой не должен зависеть вверх: запрещён импорт %s", forbiddenPrefix)
                .isEmpty();
    }
}
