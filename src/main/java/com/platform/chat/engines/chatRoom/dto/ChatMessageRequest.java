package com.platform.chat.core.modules.chatRoom.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String roomId;
    private String locationId;
    private String content;
    private Long replyToId;
}
