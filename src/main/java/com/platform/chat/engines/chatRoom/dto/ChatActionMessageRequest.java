package com.platform.chat.engines.chatRoom.dto;

import com.platform.chat.engines.chatRoom.enums.MessageActionType;
import lombok.Data;

@Data
public class ChatActionMessageRequest {
    private MessageActionType actionType = MessageActionType.CHAT; // null для JOIN/LEAVE/TYPING
    private String roomId;
    private String locationId;
}
