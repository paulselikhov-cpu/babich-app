package com.platform.chat.engines.application.service;

import com.platform.chat.engines.application.entity.Application;
import com.platform.chat.engines.application.enums.ApplicationMessageKey;
import com.platform.chat.engines.application.enums.ApplicationType;
import com.platform.chat.engines.application.exception.ApplicationHandlerMissingException;
import com.platform.chat.engines.application.exception.DuplicateApplicationHandlerException;
import com.platform.chat.engines.application.model.ApplicationDecision;
import com.platform.chat.engines.application.model.ApplicationHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Реестр стратегий обработки заявок (замена ленивой карты внутри
 * ApplicationService): карта строится один раз на старте, дубликат типа —
 * ошибка конфигурации, отсутствие стратегии — ApplicationHandlerMissingException.
 */
class ApplicationHandlerRegistryTest {

    private static ApplicationHandler handler(ApplicationType type) {
        return new ApplicationHandler() {
            @Override
            public ApplicationType getType() {
                return type;
            }

            @Override
            public ApplicationDecision resolve(Application application) {
                return ApplicationDecision.approved(ApplicationMessageKey.PASSPORT_ISSUED);
            }
        };
    }

    @Test
    void buildsMapFromInjectedHandlers() {
        ApplicationHandlerRegistry registry = new ApplicationHandlerRegistry(
                List.of(handler(ApplicationType.REGISTER_PASSPORT)));

        registry.buildHandlerMap();

        assertThat(registry.supports(ApplicationType.REGISTER_PASSPORT)).isTrue();
        assertThat(registry.get(ApplicationType.REGISTER_PASSPORT).getType())
                .isEqualTo(ApplicationType.REGISTER_PASSPORT);
    }

    @Test
    void duplicateHandlerForSameTypeFailsFastOnStartup() {
        ApplicationHandlerRegistry registry = new ApplicationHandlerRegistry(
                List.of(handler(ApplicationType.REGISTER_PASSPORT),
                        handler(ApplicationType.REGISTER_PASSPORT)));

        assertThatThrownBy(registry::buildHandlerMap)
                .isInstanceOf(DuplicateApplicationHandlerException.class)
                .hasMessageContaining("REGISTER_PASSPORT");
    }

    @Test
    void missingHandlerForTypeIsReportedExplicitly() {
        ApplicationHandlerRegistry registry = new ApplicationHandlerRegistry(
                List.of(handler(ApplicationType.REGISTER_PASSPORT)));
        registry.buildHandlerMap();

        assertThatThrownBy(() -> registry.get(ApplicationType.BUY_FIRST_LOCATION))
                .isInstanceOf(ApplicationHandlerMissingException.class)
                .hasMessageContaining("BUY_FIRST_LOCATION");
    }
}
