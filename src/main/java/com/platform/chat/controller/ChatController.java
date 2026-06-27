package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    // клиент отправляет на /app/chat.send
    // сервер рассылает всем подписчикам /topic/messages
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessage message, Principal principal) {
        message.setSender(principal.getName()); // берём username из JWT
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    // клиент подключается — уведомляем всех
    @MessageMapping("/chat.join")
    @SendTo("/topic/users")
    public ChatMessage joinChat(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        message.setType(ChatMessage.MessageType.JOIN);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    // клиент отключается — уведомляем всех
    @MessageMapping("/chat.leave")
    @SendTo("/topic/users")
    public ChatMessage leaveChat(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        message.setType(ChatMessage.MessageType.LEAVE);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}