package com.platform.chat.core.modules.chatRoom.enums;

/**
 * Тип сообщения в чате.
 */
public enum MessageType {
    CHAT,    // обычное текстовое сообщение от персонажа
    ACTION,  // действие персонажа: /me идёт на кухню → "Иван идёт на кухню"
    SYSTEM   // системное: "Иван вошёл в комнату", "Мария покинула локацию"
}