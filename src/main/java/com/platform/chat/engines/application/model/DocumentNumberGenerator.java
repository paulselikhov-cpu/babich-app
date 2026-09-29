package com.platform.chat.core.modules.application.model;

import com.platform.chat.core.modules.application.enums.ApplicationType;

/**
 * Порт генератора номеров документов, выдаваемых по заявке (паспорт, разрешение
 * на работу). Порт, а не статический util: стратегии получают его внедрением,
 * поэтому тест может подставить детерминированную реализацию, а формат/источник
 * номеров меняется без правки стратегий.
 */
public interface DocumentNumberGenerator {

    /**
     * Следующий номер документа для типа заявки
     * (например {@code PP-1234-5678}, {@code WRK-1234-5678}).
     */
    String next(ApplicationType type);
}
