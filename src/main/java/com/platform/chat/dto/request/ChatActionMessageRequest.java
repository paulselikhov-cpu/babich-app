package com.platform.chat.dto.request;

import com.platform.chat.enums.MessageActionType;
import lombok.Data;

@Data
public class ChatActionMessageRequest {
    private MessageActionType actionType = MessageActionType.CHAT; // null для JOIN/LEAVE/TYPING
    private String roomId;
    private String locationId;
}
