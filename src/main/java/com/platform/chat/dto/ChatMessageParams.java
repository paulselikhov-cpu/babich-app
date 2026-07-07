package com.platform.chat.dto;

import com.platform.chat.enums.MessageActionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageParams {
    private MessageActionType actionType = MessageActionType.CHAT; // null для JOIN/LEAVE/TYPING
    private String roomId;
    private String content;
}
