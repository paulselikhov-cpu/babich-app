package com.platform.chat.dto.response;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.Message;
import com.platform.chat.enums.MessageActionType;
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
public class ChatMessageResponse {

    private Long id;
    private MessageActionType actionType = MessageActionType.CHAT;
    private String content;
    private LocalDateTime timestamp;
    private Boolean isDeleted = false;
    private CharacterInfo character;
    private ReplyInfo replyTo; // ← добавлено, null если не реплай

    @Data
    public static class CharacterInfo {
        private Long userId;
        private String characterName;
        private String avatarUrl;
    }

    /**
     * Компактное превью сообщения-родителя для отображения цитаты
     * внутри бабла (как в Telegram). Если родитель удалён — content
     * не отдаём, фронт покажет "Сообщение удалено".
     */
    @Data
    public static class ReplyInfo {
        private Long id;
        private String content;
        private String senderName;
        private Boolean isDeleted;
    }

    /**
     * Единая точка сборки DTO из entity — используется и для истории,
     * и для realtime-рассылки через WS. ВАЖНО: вызывать только внутри
     * активной Hibernate-сессии (т.е. из @Transactional-метода сервиса),
     * иначе обращение к message.getReplyTo().getCharacter() упадёт
     * с LazyInitializationException.
     */
    public static ChatMessageResponse fromMessage(Message message) {
        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setId(message.getId());
        dto.setActionType(MessageActionType.CHAT);
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getCreatedAt());
        dto.setIsDeleted(message.getIsDeleted());

        if (message.getCharacter() != null) {
            CharacterInfo info = new CharacterInfo();
            info.setUserId(message.getCharacter().getId());
            info.setCharacterName(message.getCharacter().getCharacterName());
            info.setAvatarUrl(message.getCharacter().getAvatarUrl());
            dto.setCharacter(info);
        }

        if (message.getReplyTo() != null) {
            Message rt = message.getReplyTo();
            ReplyInfo replyInfo = new ReplyInfo();
            replyInfo.setId(rt.getId());
            replyInfo.setIsDeleted(rt.getIsDeleted());
            replyInfo.setContent(Boolean.TRUE.equals(rt.getIsDeleted()) ? null : rt.getContent());
            if (rt.getCharacter() != null) {
                replyInfo.setSenderName(rt.getCharacter().getCharacterName());
            }
            dto.setReplyTo(replyInfo);
        }

        return dto;
    }

    public static ChatMessageResponse getResponseByActionType(MessageActionType actionType, ChatUser user) {
        ChatMessageResponse messageResponse = new ChatMessageResponse();
        CharacterInfo info = new CharacterInfo();
        info.setUserId(user.getId());
        info.setCharacterName(user.getCharacterName());
        info.setAvatarUrl(user.getAvatarUrl());
        messageResponse.setCharacter(info);
        messageResponse.setActionType(actionType);
        messageResponse.setTimestamp(LocalDateTime.now());
        return messageResponse;
    }

    public static ChatMessageResponse deleted(Long messageId, Long roomId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(messageId);
        response.setActionType(MessageActionType.DELETE);
        response.setIsDeleted(true);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}