package com.platform.chat.dto.request;

import com.platform.chat.enums.ChatRole;
import com.platform.chat.modules.work.enums.Profession;

/**
 * Обновление полей персонажа из админской таблицы «Персонажи района».
 * Все поля опциональны: null — не менять.
 */
public record CharacterUpdateRequest(
        ChatRole role,
        Profession profession,
        String gameStatus,
        Long coinBalance,
        Integer energy,
        Integer rating,
        Long xpUser
) {
}
