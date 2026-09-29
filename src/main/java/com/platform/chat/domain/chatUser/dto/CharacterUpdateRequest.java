package com.platform.chat.base.chatUser.dto;

import com.platform.chat.base.chatUser.enums.ChatRole;
import com.platform.chat.base.chatUser.enums.Profession;

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
