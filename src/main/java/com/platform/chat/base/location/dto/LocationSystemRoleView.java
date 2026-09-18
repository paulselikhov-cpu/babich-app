package com.platform.chat.base.location.dto;

import com.platform.chat.base.chatUser.enums.SystemRoleType;

import java.time.LocalDateTime;

/**
 * Должность персонажа в системной локации с именем занимающего —
 * для отображения в интерфейсе (мэрия/губернатор и т.п.). Должность хранится
 * в поле LocationUser.systemRole (переезд из упразднённой таблицы location_posts).
 */
public record LocationSystemRoleView(
        Long id,
        Long locationId,
        Long characterId,
        String characterName,
        SystemRoleType systemRole,
        LocalDateTime appointedAt
) {
}