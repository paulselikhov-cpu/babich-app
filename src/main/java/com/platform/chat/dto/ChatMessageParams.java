package com.platform.chat.dto;
import lombok.Data;

@Data
public class ChatMessageParams {
    private String roomId;
    private String locationId;
    private String content;
}
