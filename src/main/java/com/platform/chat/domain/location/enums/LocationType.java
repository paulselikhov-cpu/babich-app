package com.platform.chat.domain.location.enums;

/**
 * Тип локации. Единая классификация (бывший PublicLocationType + LocationCategoryType).
 *
 * Системные локации района (согласно концепции, раздел 8) — создаются автоматически
 * при инициализации района (isSystem = true): CITY_HALL…REAL_ESTATE_MARKET.
 *
 * Пользовательские локации (isSystem = false):
 * - {@link #BUSINESS} — бизнес-локация (заведение);
 * - {@link #HOME} — жилая локация (квартира).
 */
public enum LocationType {
    // === Системные локации района ===
    CITY_HALL,           // Мэрия — управление районом
    LENIN_SQUARE,        // Площадь Ленина — народные голосования + комната "Тёплые трубы" для бомжей
    POLICE_STATION,      // Полицейский участок — база civic_role
    PRISON,              // Тюрьма — требует мэрии
    BANK,                // Банк — системная локация, chatUser.civic_role= DIRECTOR (директор назначается мэрией)
    WAREHOUSE,           // Склад — работает только для бизнесменов (chatUser.civic_role = BUSINESSMAN)
    GENERAL_MARKET,      // Магазин
    REAL_ESTATE_MARKET,  // Рынок недвижимости — chatUser.civic_role= DIRECTOR (владелец назначается мэрией)

    // === Пользовательские локации ===
    BUSINESS,            // бизнес-локация (заведение)
    HOME                 // жилая локация (квартира)
}
