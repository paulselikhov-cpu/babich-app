package com.platform.chat.engines.poll.dto;

/**
 * Эффективные настройки голосования (выборы или обжалование) для района.
 * Возвращаются сервисом из пер-районных настроек; значений по умолчанию
 * в конфиге нет — они лежат в самой сущности DistrictSettings.
 */
public record PollSettingsDto(
        int durationHours,
        int aliveWindowDays,
        int quorumPercent,
        Integer maxCandidates // null — для recall (квоты кандидатов не применимы)
) {
}
