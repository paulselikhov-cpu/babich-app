package com.platform.chat.engines.application.i18n;

import com.platform.chat.engines.application.enums.ApplicationMessageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Инфраструктурный адаптер над {@link MessageSource} для текстов заявок.
 * Живёт в ядре процесса: решением handler'а является ключ сообщения
 * ({@link ApplicationMessageKey}), а человекочитаемый текст резолвит ядро
 * и сохраняет в {@code Application.resultMessage}.
 */
@Component
@RequiredArgsConstructor
public class ApplicationMessages {

    private final MessageSource messageSource;

    // TODO: сейчас захардкожена русская локаль.
    // Когда понадобится мультиязычность:
    // 1) добавить messages_en.properties и т.д.;
    // 2) заменить getRussianLocale() на Locale, который приходит извне
    //    (например, LocaleContextHolder.getLocale() или параметр из контроллера);
    // 3) рассмотреть переход на хранение кода+аргументов в Application вместо готовой строки,
    //    чтобы можно было резолвить сообщение под локаль конкретного читателя, а не того,
    public String resolve(ApplicationMessageKey key, Object... args) {
        return messageSource.getMessage(key.getCode(), args, getRussianLocale());
    }

    private Locale getRussianLocale() {
        return new Locale("ru");
    }
}
