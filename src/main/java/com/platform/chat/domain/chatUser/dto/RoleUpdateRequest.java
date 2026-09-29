package com.platform.chat.base.chatUser.dto;

import com.platform.chat.base.chatUser.enums.ChatRole;

/** Запрос смены роли персонажа (админская панель района). */
public record RoleUpdateRequest(ChatRole role) {
}
