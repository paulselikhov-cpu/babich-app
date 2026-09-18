package com.platform.chat.core.modules.webSocket.dto;

import com.platform.chat.core.modules.message.enums.MessageActionType;
import lombok.Data;

@Data
public class ChatActionMessageRequest {
    private MessageActionType actionType = MessageActionType.CHAT; // null для JOIN/LEAVE/TYPING
    private String roomId;
    private String locationId;
}
