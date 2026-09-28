package com.platform.chat.core.modules.transactionLog.enums;

/**
 * Причина начисления/списания монет или XP.
 * Используется CoinService для записи в TransactionLog.
 * Список открыт для расширения — новые причины добавляются по мере
 * реализации новых механик (группировки, полиция, банк, рынок и т.д.).
 *
 * <p>Словарь живёт в ядре аудита ({@code core.modules.transactionLog}), а не в
 * фича-модуле кошелька: колонка {@code transaction_logs.reason} (entity
 * {@link com.platform.chat.core.modules.transactionLog.entity.TransactionLog})
 * типизирована этим enum, а события {@code CoinsChangedEvent}/
 * {@code XpChangedEvent} несут причину в payload. Иначе ядро зависело бы от
 * фичи (было {@code chat.modules.coin.enums.CoinUpdateReason}).</p>
 */
public enum CoinUpdateReason {
    WORK_SHIFT,             // заработок за смену/тап-фарм (профессия)
    BUY_FIRST_LOCATION,     // списание за покупку первой локации
    LOCATION_CREATE,        // списание за создание платной локации через REST
    LOCATION_EXTEND_ROOM,   // списание за добавление кастомной комнаты
    GANG_TRIBUTE,           // зарплата участнику группировки из общака
    GANG_TREASURY_INCOME,   // пополнение общака группировки (кража, эскорт и т.д.)
    POLICE_FINE,            // штраф от полицейского
    POLICE_GANG_CATCH,      // награда полицейскому за поимку члена группировки
    BANK_DEPOSIT_OPEN,      // списание при открытии вклада
    BANK_INTEREST,          // начисление процентов по вкладу
    BANK_FEE_TO_TREASURY,   // комиссия банка в бюджет мэрии
    MARKET_STALL_RENT,      // списание за аренду прилавка
    MARKET_SALE,            // доход от продажи на рынке (снятие кассы)
    ESCORT_SESSION,         // доход участницы/участника группировки "Проститутки"
    THEFT_SUCCESS           // личная доля от успешной кражи
}
