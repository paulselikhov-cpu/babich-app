package com.platform.chat.modules.poll.enums;

/**
 * Стратегия расчёта веса голоса участника.
 */
public enum WeightStrategy {
    ONE_VOTE,
    XP_USER_LEVEL,
    BY_POST_RANK
}