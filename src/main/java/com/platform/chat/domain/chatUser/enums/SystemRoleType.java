package com.platform.chat.base.chatUser.enums;

/**
 * Должность персонажа в системной локации (SystemRole). Бывший LocationPostType:
 * должность переехала из отдельной таблицы location_posts в поле LocationUser.systemRole.
 *
 * Каждая должность привязана к конкретному типу системной локации (PublicLocationType).
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