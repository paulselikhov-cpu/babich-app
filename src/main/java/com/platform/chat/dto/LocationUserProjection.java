package com.platform.chat.dto;

import com.platform.chat.enums.SystemRoleType;
import java.time.LocalDateTime;

/** Сырая проекция из БД — presenceId null означает offline. */
public record LocationUserProjection(
        Long characterId,
        String characterName,
        LocalDateTime lastSeenAt,
        Long presenceId,
        Long roomId,
        String roomName,
        SystemRoleType systemRole
) {
    public boolean isOnline() {
        return presenceId != null;
    }
}