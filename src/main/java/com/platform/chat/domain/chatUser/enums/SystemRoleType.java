package com.platform.chat.domain.chatUser.enums;

/**
 * Должность персонажа в системной локации (SystemRole).
 * Хранится полем {@code LocationUser.systemRole} — отдельной таблицы
 * должностей нет.
 *
 * Каждая должность привязана к конкретному коду системной локации
 * (см. {@code SystemLocationCode}).
 */
public enum SystemRoleType {
    // === Мэрия (CITY_HALL) ===
    GOVERNOR,           // Глава администрации района — 1 слот
    MAYOR,              // Мэр — 1 слот
    DEPUTY,             // Депутат — N слотов

    // === Полицейский участок (POLICE_STATION) ===
    POLICE_OFFICER,     // Полицейский — N слотов (фиксированное число на район)

    // === Банк (BANK) ===
    BANK_DIRECTOR,      // Директор банка — 1 слот
    BANK_EMPLOYEE,      // Сотрудник банка — N слотов

    // === Рынок недвижимости (REAL_ESTATE_MARKET) ===
    REAL_ESTATE_DIRECTOR // Владелец рынка недвижимости — 1 слот
}