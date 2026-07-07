package com.platform.chat.dto;

import com.platform.chat.enums.MessageActionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageParams {
    private String roomId;
    private String content;
}
