package com.platform.chat.modules.economy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Результат одного тап-действия работы (например смена дворника).
 * Отдаётся фронту чтобы обновить UI без лишнего GET-запроса.
 */
@Data
@AllArgsConstructor
public class WorkResultResponse {
    private long coinsEarned;
    private long xpEarned;
    private long newCoinBalance;
    private long newXp;
    private int newEnergy;
}
