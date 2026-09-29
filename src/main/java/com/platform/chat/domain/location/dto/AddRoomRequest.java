package com.platform.chat.domain.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO для добавления кастомной комнаты в локацию.
 * Используется только для расширяемых локаций.
 */
@Data
public class AddRoomRequest {

    @NotBlank(message = "Название комнаты обязательно")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    /**
     * id персонажа который добавляет комнату.
     * Должен быть владельцем локации.
     */
    @NotNull(message = "Id персонажа обязателен")
    private Long characterId;
}