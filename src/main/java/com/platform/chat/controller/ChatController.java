package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessage;
import com.platform.chat.entity.ChatUser;
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

    /**
     * Получить имя персонажа по username из JWT.
     * Если персонаж не найден — возвращаем username как fallback.
     */
    private String getCharacterName(String username) {
        return chatUserRepository.findByUsername(username)
                .map(ChatUser::getCharacterName)
                .orElse(username); // fallback на username если нет персонажа
    }

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message, Principal principal) {
        message.setSender(getCharacterName(principal.getName()));
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );

        messageService.saveMessage(message, principal.getName());
    }

    @MessageMapping("/chat.join")
    public void joinChat(ChatMessage message, Principal principal) {
        message.setSender(getCharacterName(principal.getName()));
        message.setType(ChatMessage.MessageType.JOIN);
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(ChatMessage message, Principal principal) {
        message.setSender(getCharacterName(principal.getName()));
        message.setType(ChatMessage.MessageType.LEAVE);
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
    }

    @MessageMapping("/chat.typing")
    public void typing(ChatMessage message, Principal principal) {
        message.setSender(getCharacterName(principal.getName()));
        message.setType(ChatMessage.MessageType.TYPING);
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/typing." + message.getRoomId(),
                message
        );
    }
}