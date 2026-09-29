package com.platform.chat.domain.chatUser.dto;

import com.platform.chat.domain.chatUser.enums.ChatRole;

/** Запрос смены роли персонажа (админская панель района). */
public record RoleUpdateRequest(ChatRole role) {
}
