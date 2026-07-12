package com.platform.chat.dto;

import com.platform.chat.enums.MessageActionType;
import lombok.Data;

@Data
public class ChatActionMessageParams {
    private MessageActionType actionType = MessageActionType.CHAT; // null для JOIN/LEAVE/TYPING
    private String roomId;
    private String locationId;
}
