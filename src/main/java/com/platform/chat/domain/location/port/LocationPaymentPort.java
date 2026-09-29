package com.platform.chat.base.location.port;

/**
 * Порт оплаты операций с локациями (создание, расширение комнат).
 *
 * <p>Базовый модуль локаций {@code chat.base.location} объявляет этот порт,
 * чтобы не зависеть напрямую от модуля экономики {@code chat.modules.coin}
 * и реестра аудита {@code chat.core.modules.transactionLog}.
 * Реализация предоставляется адаптером в {@code chat.modules.coin}.</p>
 */
public interface LocationPaymentPort {

    /**
     * Списать монеты за создание локации.
     */
    void chargeLocationCreation(Long characterId, long amount, Long locationId);

    /**
     * Списать монеты за добавление комнаты в локацию.
     */
    void chargeRoomExtension(Long characterId, long amount, Long locationId);
}
