package com.platform.chat.engines.chatRoom.dto;

public record UnreadUpdateResponse(
        Long roomId,
        Long locationId,
        long roomUnreadCount
) {}