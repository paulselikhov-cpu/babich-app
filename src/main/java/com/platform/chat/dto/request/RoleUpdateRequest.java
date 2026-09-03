package com.platform.chat.dto.request;

import com.platform.chat.enums.ChatRole;

/** Запрос смены роли персонажа (админская панель района). */
public record RoleUpdateRequest(ChatRole role) {
}
