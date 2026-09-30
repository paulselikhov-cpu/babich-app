package com.platform.chat.domain.location.enums;

/**
 * Типы системных общественных локаций района (согласно концепции, раздел 8).
 * Создаются автоматически администратором платформы при создании района.
 */
public enum PublicLocationType {
    CITY_HALL,           // Мэрия — управление районом
    LENIN_SQUARE,        // Площадь Ленина — народные голосования + комната "Тёплые трубы" для бомжей
    POLICE_STATION,      // Полицейский участок — база civic_role
    PRISON,              // Тюрьма — требует мэрии
    BANK,                // Банк — системная локация, chatUser.civic_role= DIRECTOR (директор назначается мэрией)
    WAREHOUSE,           // Склад — работает только для бизнесменов (chatUser.civic_role = BUSINESSMAN)
    GENERAL_MARKET,      // Магазин
    REAL_ESTATE_MARKET   // Рынок недвижимости — chatUser.civic_role= DIRECTOR (владелец назначается мэрией)
}
