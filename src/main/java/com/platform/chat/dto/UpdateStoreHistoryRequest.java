package com.platform.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO для обновления настройки хранения истории комнаты.
 * Тугл в настройках комнаты — хранить историю или нет.
 */
@Data
public class UpdateStoreHistoryRequest {

    @NotNull
    private Boolean storeHistory;

    @NotNull(message = "Id персонажа обязателен")
    private Long characterId;
}