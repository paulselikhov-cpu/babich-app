package com.platform.chat.base.location.dto;

import com.platform.chat.base.chatUser.enums.SystemRoleType;
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