package com.platform.chat.dto;

import com.platform.chat.enums.LocationPostType;

import java.time.LocalDateTime;

/**
 * Должность персонажа в системной локации с именем занимающего —
 * для отображения в интерфейсе (мэрия/губернатор и т.п.). Сам LocationPost
 * помечает character/location как {@code @JsonIgnore}, поэтому без этого
 * проекции клиент не увидит, кто занимает должность.
 */
public record LocationPostView(
        Long id,
        Long characterId,
        String characterName,
        LocationPostType post,
        LocalDateTime appointedAt
) {
}
