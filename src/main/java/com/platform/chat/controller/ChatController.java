package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessage;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.MessageActionType;
import com.platform.chat.repository.ChatUserRepository;
import com.platform.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatUserRepository chatUserRepository;

    // ─── отправить сообщение ──────────────────────────────────────────────
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message, Principal principal) {
        message.setCharacter(new ChatMessage.CharacterInfo());
        message.getCharacter().setCharacterName((getCharacterName(principal.getName())));
        message.setTimestamp(LocalDateTime.now());

        // сохраняем в БД и обогащаем message id + character
        if (message.getActionType() == MessageActionType.CHAT) {
            messageService.enrichAfterSave(message, principal.getName());
        }

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
    }

    // ─── вошёл в комнату ─────────────────────────────────────────────────
    @MessageMapping("/chat.join")
    public void joinChat(ChatMessage message, Principal principal) {
        message.setCharacter(new ChatMessage.CharacterInfo());
        message.getCharacter().setCharacterName((getCharacterName(principal.getName())));
        message.setActionType(MessageActionType.JOIN);
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
    }

    // ─── покинул комнату ─────────────────────────────────────────────────
    @MessageMapping("/chat.leave")
    public void leaveChat(ChatMessage message, Principal principal) {
        message.setCharacter(new ChatMessage.CharacterInfo());
        message.getCharacter().setCharacterName((getCharacterName(principal.getName())));
        message.setActionType(MessageActionType.LEAVE);
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
    }

    // ─── индикатор печати ────────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void typing(ChatMessage message, Principal principal) {
        message.setCharacter(new ChatMessage.CharacterInfo());
        message.getCharacter().setCharacterName((getCharacterName(principal.getName())));
        message.setActionType(MessageActionType.TYPING);

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
    }

    // ─── хелпер ──────────────────────────────────────────────────────────
    private String getCharacterName(String username) {
        return chatUserRepository.findByUsername(username)
                .map(ChatUser::getCharacterName)
                .orElse(username); // fallback на username если нет персонажа
    }
}