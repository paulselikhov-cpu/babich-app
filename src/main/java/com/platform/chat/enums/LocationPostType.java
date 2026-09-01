package com.platform.chat.enums;

/**
 * Типы должностей в системных локациях.
 * Каждая должность привязана к конкретному типу системной локации (PublicLocationType).
 */
public enum LocationPostType {
    // === Мэрия (CITY_HALL) ===
    GOVERNOR,           // Глава администрации района — 1 слот
    MAYOR,              // Мэр — N слотов
    DEPUTY,             // Депутат — N слотов

    // === Полицейский участок (POLICE_STATION) ===
    POLICE_OFFICER,     // Полицейский — N слотов (фиксированное число на район)

    // === Банк (BANK) ===
    BANK_DIRECTOR,      // Директор банка — 1 слот
    BANK_EMPLOYEE,      // Сотрудник банка — N слотов

    // === Рынок недвижимости (REAL_ESTATE_MARKET) ===
    REAL_ESTATE_DIRECTOR // Владелец рынка недвижимости — 1 слот
}