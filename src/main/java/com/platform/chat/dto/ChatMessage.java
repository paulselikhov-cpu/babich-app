package com.platform.chat.dto;
import com.platform.chat.entity.Message;
import com.platform.chat.enums.MessageActionType;
import com.platform.chat.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Единый DTO для всех типов сообщений в чате.
 * Используется и для WebSocket и как основа для сохранения.
 *
 * CHAT    → id и character заполняются после сохранения в БД
 * JOIN    → только sender, roomId, timestamp
 * LEAVE   → только sender, roomId, timestamp
 * TYPING  → только sender, roomId
 */
@Data
public class ChatMessage {

    private Long id;
    private MessageType messageType;
    private MessageActionType actionType = MessageActionType.CHAT; // null для JOIN/LEAVE/TYPING
    private String content;
    private String roomId;
    private LocalDateTime timestamp;
    private Boolean isDeleted = false;
    private CharacterInfo character;    // null для JOIN/LEAVE/TYPING

    @Data
    public static class CharacterInfo {
        private Long userId;
        private String characterName;
        private String avatarUrl;
    }

    public static ChatMessage from(Message message) {
        ChatMessage dto = new ChatMessage();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getType());
        dto.setIsDeleted(message.getIsDeleted());
        dto.setTimestamp(message.getCreatedAt());

        // если персонаж есть — заполняем CharacterInfo
        if (message.getCharacter() != null) {
            CharacterInfo info = new ChatMessage.CharacterInfo();
            info.setUserId(message.getCharacter().getId());
            info.setCharacterName(message.getCharacter().getCharacterName());
            info.setAvatarUrl(message.getCharacter().getAvatarUrl());
            dto.setCharacter(info);
        }

        return dto;
    }
}