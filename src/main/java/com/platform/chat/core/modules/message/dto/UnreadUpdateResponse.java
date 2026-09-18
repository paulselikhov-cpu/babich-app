package com.platform.chat.core.modules.message.dto;

public record UnreadUpdateResponse(
        Long roomId,
        Long locationId,
        long roomUnreadCount
) {}