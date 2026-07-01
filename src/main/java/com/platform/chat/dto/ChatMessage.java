package com.platform.chat.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {

    public enum MessageType {
        CHAT,   // обычное сообщение
        JOIN,   // юзер зашёл
        LEAVE,   // юзер вышел
        TYPING // уведолмение о печати
    }

    private MessageType type = MessageType.CHAT;
    private String content;
    private String sender;       // заполняется на сервере из JWT
    private String roomId;       // для будущих комнат
    private LocalDateTime timestamp;
}