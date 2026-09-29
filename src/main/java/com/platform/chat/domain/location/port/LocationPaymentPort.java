package com.platform.chat.domain.location.port;

/**
 * Порт оплаты операций с локациями (создание, расширение комнат).
 *
 * <p>Базовый модуль локаций {@code chat.domain.location} объявляет этот порт,
 * чтобы не зависеть напрямую от модуля экономики {@code chat.engines.economy}
 * и реестра аудита {@code chat.engines.transactionLog}.
 * Реализация предоставляется адаптером в {@code chat.engines.economy}.</p>
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
