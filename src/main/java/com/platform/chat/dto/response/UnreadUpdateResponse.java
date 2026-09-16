package com.platform.chat.dto.response;

public record UnreadUpdateResponse(
        Long roomId,
        Long locationId,
        long roomUnreadCount
) {}