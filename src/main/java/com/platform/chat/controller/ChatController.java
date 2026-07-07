package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessageParams;
import com.platform.chat.dto.ChatMessageResponse;
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
    public void sendMessage(ChatMessageParams messageParams, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.from(messageParams.getActionType(), principal.getName());

        // сохраняем в БД и обогащаем message id + character
        if (messageParams.getActionType() == MessageActionType.CHAT) {
            messageResponse = messageService.enrichAfterSave(messageResponse, principal.getName(), messageParams.getRoomId(), messageParams);
        }

        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── вошёл в комнату ─────────────────────────────────────────────────
    @MessageMapping("/chat.join")
    public void joinChat(ChatMessageParams messageParams, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.from(MessageActionType.JOIN, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── покинул комнату ─────────────────────────────────────────────────
    @MessageMapping("/chat.leave")
    public void leaveChat(ChatMessageParams message, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.from(MessageActionType.LEAVE, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                messageResponse
        );
    }

    // ─── индикатор печати ────────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void typing(ChatMessageParams message, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.from(MessageActionType.TYPING, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                messageResponse
        );
    }
}