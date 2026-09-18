package com.platform.chat.base.chatUser.enums;

public enum PersonLevel {
    LEVEL_1(1, 0),
    LEVEL_2(2, 200),
    LEVEL_3(3, 600),
    LEVEL_4(4, 1500),
    LEVEL_5(5, 3500);

    private final int levelNumber;
    private final int xpThreshold;

    PersonLevel(int levelNumber, int xpThreshold) {
        this.levelNumber = levelNumber;
        this.xpThreshold = xpThreshold;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getXpThreshold() {
        return xpThreshold;
    }

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