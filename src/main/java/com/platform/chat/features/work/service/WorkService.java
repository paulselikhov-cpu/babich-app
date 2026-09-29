package com.platform.chat.modules.work.service;
import com.platform.chat.modules.work.dto.WorkResultResponse;

import com.platform.chat.base.chatUser.service.EnergyService;
import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.base.chatUser.enums.Profession;

import com.platform.chat.modules.coin.service.CoinService;
import com.platform.chat.core.modules.transactionLog.enums.CoinUpdateReason;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Тап-фарм работы. В MVP реализована только профессия дворника.
 * Rate limit — в памяти процесса (достаточно для одного инстанса MVP).
 */
@Service
@RequiredArgsConstructor
public class WorkService {

    private static final long COOLDOWN_SECONDS = 3;
    private static final int ENERGY_COST = 5;
    private static final long COINS_PER_SHIFT = 2;
    private static final long XP_PER_SHIFT = 1;

    private final ChatUserRepository chatUserRepository;
    private final CoinService coinService;
    private final EnergyService energyService;

    /** userId -> момент последнего тапа, для rate limit. */
    private final ConcurrentHashMap<Long, Instant> lastWorkAt = new ConcurrentHashMap<>();

    @Transactional
    public WorkResultResponse workAsDvornik(Long userId) {
        ChatUser user = chatUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + userId));

        if (user.getProfession() != Profession.DVORNIK) {
            throw new IllegalStateException("У персонажа нет профессии дворника");
        }

        checkCooldown(userId);

        energyService.consumeEnergy(userId, ENERGY_COST);
        coinService.giveCoins(userId, COINS_PER_SHIFT, CoinUpdateReason.WORK_SHIFT, null);
        coinService.giveXp(userId, XP_PER_SHIFT, CoinUpdateReason.WORK_SHIFT);

        lastWorkAt.put(userId, Instant.now());

        ChatUser refreshed = chatUserRepository.findById(userId).orElseThrow();
        return new WorkResultResponse(
                COINS_PER_SHIFT,
                XP_PER_SHIFT,
                refreshed.getCoinBalance(),
                refreshed.getXpUser(),
                energyService.getCurrentEnergy(userId)
        );
    }

    private void checkCooldown(Long userId) {
        Instant last = lastWorkAt.get(userId);
        if (last != null) {
            long secondsPassed = Instant.now().getEpochSecond() - last.getEpochSecond();
            if (secondsPassed < COOLDOWN_SECONDS) {
                throw new IllegalStateException(
                        "Слишком часто. Подождите " + (COOLDOWN_SECONDS - secondsPassed) + " сек.");
            }
        }
    }
}
