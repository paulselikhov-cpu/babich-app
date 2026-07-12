package com.platform.chat.dto;

import java.time.LocalDateTime;

/** DTO, уходящее на фронт. */
public record LocationUserDto(
        Long characterId,
        String characterName,
        boolean online,
        Long roomId,
        String roomName,
        LocalDateTime lastSeenAt // null, если online
) {
    public static LocationUserDto fromProjection(LocationUserProjection p) {
        boolean online = p.isOnline();
        return new LocationUserDto(
                p.characterId(), p.characterName(), online,
                online ? p.roomId() : null,
                online ? p.roomName() : null,
                online ? null : p.lastSeenAt()
        );
    }
}