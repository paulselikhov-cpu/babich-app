package com.platform.chat.dto.response;

import com.platform.chat.dto.LocationUserProjection;
import com.platform.chat.enums.SystemRoleType;

import java.time.LocalDateTime;

public record LocationUserResponse(
        Long characterId,
        String characterName,
        boolean online,
        Long roomId,
        String roomName,
        LocalDateTime lastSeenAt,     // null, если online
        SystemRoleType systemRole,    // должность в системной локации; null — обычный участник
        LocalDateTime appointedAt     // когда назначен на должность; null — нет должности
) {
    public static LocationUserResponse fromProjection(LocationUserProjection p) {
        boolean online = p.isOnline();
        return new LocationUserResponse(
                p.characterId(), p.characterName(), online,
                online ? p.roomId() : null,
                online ? p.roomName() : null,
                online ? null : p.lastSeenAt(),
                p.systemRole(),
                null
        );
    }
}