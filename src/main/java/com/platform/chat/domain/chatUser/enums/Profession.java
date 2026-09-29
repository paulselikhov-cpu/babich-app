package com.platform.chat.domain.chatUser.enums;

/**
 * Профессия персонажа — определяет доступные механики тап-фарма.
 * UNEMPLOYED = нет профессии (начальное состояние до выбора).
 * DVORNIK = дворник — первая доступная профессия для бомжей.
 * Список расширяется по мере добавления новых механик.
 */
public enum Profession {
    UNEMPLOYED,
    DVORNIK
}
