package com.platform.chat.dto.response;

public record UnreadUpdateEvent(
        Long roomId,
        Long locationId,
        long roomUnreadCount
) {}