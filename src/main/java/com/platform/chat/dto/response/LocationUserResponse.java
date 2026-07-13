package com.platform.chat.dto.response;

import com.platform.chat.dto.LocationUserProjection;

import java.time.LocalDateTime;

public record LocationUserResponse(
        Long characterId,
        String characterName,
        boolean online,
        Long roomId,
        String roomName,
        LocalDateTime lastSeenAt // null, если online
) {
    public static LocationUserResponse fromProjection(LocationUserProjection p) {
        boolean online = p.isOnline();
        return new LocationUserResponse(
                p.characterId(), p.characterName(), online,
                online ? p.roomId() : null,
                online ? p.roomName() : null,
                online ? null : p.lastSeenAt()
        );
    }
}