package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessage;
import com.platform.chat.service.MessageService;
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
    private final MessageService messageService; // ← добавили

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        message.setTimestamp(LocalDateTime.now());

        // сначала рассылаем через WebSocket — чтобы не было задержки
        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );

        // потом сохраняем в БД (только CHAT тип)
        // если сохранение упадёт — сообщение уже ушло клиентам
        messageService.saveMessage(message, principal.getName());
    }

    @MessageMapping("/chat.join")
    public void joinChat(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        message.setType(ChatMessage.MessageType.JOIN);
        message.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
        // JOIN не сохраняем в историю
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        message.setType(ChatMessage.MessageType.LEAVE);
        message.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                message
        );
        // LEAVE не сохраняем в историю
    }
}