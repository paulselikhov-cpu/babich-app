package com.platform.chat.engines.chatRoom.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String roomId;
    private String locationId;
    private String content;
    private Long replyToId;
}
