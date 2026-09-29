package com.platform.chat.modules.coin.adapter;

import com.platform.chat.base.location.port.LocationPaymentPort;
import com.platform.chat.core.modules.transactionLog.enums.CoinUpdateReason;
import com.platform.chat.modules.coin.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Адаптер оплаты операций с локациями через {@link CoinService}.
 */
@Component
@RequiredArgsConstructor
public class LocationPaymentAdapter implements LocationPaymentPort {

    private final CoinService coinService;

    @Override
    public void chargeLocationCreation(Long characterId, long amount, Long locationId) {
        coinService.takeCoins(characterId, amount, CoinUpdateReason.LOCATION_CREATE, locationId);
    }

    @Override
    public void chargeRoomExtension(Long characterId, long amount, Long locationId) {
        coinService.takeCoins(characterId, amount, CoinUpdateReason.LOCATION_EXTEND_ROOM, locationId);
    }
}
