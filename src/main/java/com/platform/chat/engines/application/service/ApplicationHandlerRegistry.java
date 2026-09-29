package com.platform.chat.engines.application.service;

import com.platform.chat.engines.application.enums.ApplicationType;
import com.platform.chat.engines.application.exception.ApplicationHandlerMissingException;
import com.platform.chat.engines.application.exception.DuplicateApplicationHandlerException;
import com.platform.chat.engines.application.model.ApplicationHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр стратегий обработки заявок: {@code ApplicationType → ApplicationHandler}.
 *
 * <p>Spring собирает все реализации {@link ApplicationHandler} в список и передаёт
 * сюда. Карта строится один раз на старте ({@code @PostConstruct}) и валидируется:
 * два обработчика на один тип — ошибка конфигурации, приложение не поднимется
 * (fail fast вместо неоднозначной обработки в рантайме). Реализация повторяет
 * канон {@code PollResultDispatcher}.</p>
 */
@Service
@RequiredArgsConstructor
public class ApplicationHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(ApplicationHandlerRegistry.class);

    private final List<ApplicationHandler> handlers;
    private final Map<ApplicationType, ApplicationHandler> handlerByType = new EnumMap<>(ApplicationType.class);

    @PostConstruct
    void buildHandlerMap() {
        for (ApplicationHandler handler : handlers) {
            ApplicationType type = handler.getType();
            ApplicationHandler previous = handlerByType.put(type, handler);
            if (previous != null) {
                throw new DuplicateApplicationHandlerException(
                        type,
                        previous.getClass().getSimpleName(),
                        handler.getClass().getSimpleName());
            }
        }
        log.info("ApplicationHandlerRegistry готов: типов заявок с обработчиком = {}", handlerByType.size());
    }

    /**
     * Стратегия для типа заявки.
     *
     * @throws ApplicationHandlerMissingException если для типа нет стратегии
     */
    public ApplicationHandler get(ApplicationType type) {
        ApplicationHandler handler = handlerByType.get(type);
        if (handler == null) {
            throw new ApplicationHandlerMissingException(type);
        }
        return handler;
    }

    /** Есть ли стратегия для типа заявки. */
    public boolean supports(ApplicationType type) {
        return handlerByType.containsKey(type);
    }
}
