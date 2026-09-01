package com.platform.chat.enums;

/**
 * Уровень персонажа — гейт для доступа к функциональности.
 * Вычисляется из xpUser по пороговой шкале:
 * LV1: 0 XP (базовый доступ)
 * LV2: 200 XP
 * LV3: 600 XP (выборы губернатора)
 * LV4: 1500 XP
 * LV5: 3500 XP
 */
public enum PersonLevel {
    LEVEL_1(0),
    LEVEL_2(200),
    LEVEL_3(600),
    LEVEL_4(1500),
    LEVEL_5(3500);

    private final int xpThreshold;

    PersonLevel(int xpThreshold) {
        this.xpThreshold = xpThreshold;
    }

    public int getXpThreshold() {
        return xpThreshold;
    }

    /**
     * Определить уровень персонажа по его XP.
     * Идём от высшего к низшему — возвращаем первый подходящий.
     */
    public static PersonLevel fromXp(long xp) {
        PersonLevel result = LEVEL_1;
        for (PersonLevel level : values()) {
            if (xp >= level.xpThreshold) {
                result = level;
            }
        }
        return result;
    }
}