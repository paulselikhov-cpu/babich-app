package com.platform.chat.dto;

import com.platform.chat.entity.Message;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO для отдачи сообщений на фронт.
 * Вместо Entity с LAZY связями — простой объект с нужными полями.
 * Так избегаем проблем с LazyInitializationException и @JsonIgnore.
 */
@Data
public class MessageResponse {

    private Long id;
    private String content;
    private String type;
    private Boolean isDeleted;
    private LocalDateTime createdAt;

    // только нужные поля персонажа — не весь ChatUser
    private CharacterInfo character;

    @Data
    public static class CharacterInfo {
        private Long userId;
        private String characterName;
        private String avatarUrl;
    }

    /**
     * Фабричный метод — создаёт DTO из Entity.
     * Вызывается в сервисе после загрузки из БД.
     */
    public static MessageResponse from(Message message) {
        MessageResponse dto = new MessageResponse();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setType(message.getType().name());
        dto.setIsDeleted(message.getIsDeleted());
        dto.setCreatedAt(message.getCreatedAt());

        // если персонаж есть — заполняем CharacterInfo
        if (message.getCharacter() != null) {
            CharacterInfo info = new CharacterInfo();
            info.setUserId(message.getCharacter().getId());
            info.setCharacterName(message.getCharacter().getCharacterName());
            info.setAvatarUrl(message.getCharacter().getAvatarUrl());
            dto.setCharacter(info);
        }

        return dto;
    }
}