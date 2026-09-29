package com.platform.chat.core.modules.chatRoom.dto;

public record UnreadUpdateResponse(
        Long roomId,
        Long locationId,
        long roomUnreadCount
) {}