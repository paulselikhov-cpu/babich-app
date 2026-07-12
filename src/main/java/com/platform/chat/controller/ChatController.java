package com.platform.chat.controller;

import com.platform.chat.dto.ChatActionMessageParams;
import com.platform.chat.dto.ChatMessageParams;
import com.platform.chat.dto.ChatMessageResponse;
import com.platform.chat.enums.MessageActionType;
import com.platform.chat.service.ChatUserService;
import com.platform.chat.service.MessageService;
import com.platform.chat.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import com.platform.chat.entity.ChatUser;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatUserService chatUserService;
    private final PresenceService presenceService; // ← новая зависимость

    // ─── отправить сообщение ──────────────────────────────────────────────
    @MessageMapping("/chat.history.send")
    public void sendMessage(ChatMessageParams messageParams, Principal principal) {

        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        Long messageId = messageService.saveByUser(chatUser, messageParams.getContent(), messageParams.getRoomId());
        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseWithMessageId(chatUser, messageParams.getContent(), messageId);

        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── вошёл в комнату ─────────────────────────────────────────────────
    // добавлен вызов presenceService.enterRoom — одно STOMP-сообщение теперь
    // и шлёт системное сообщение "X вошёл в комнату" (как раньше),
    // и обновляет CharacterPresence (где юзер сейчас online)
    @MessageMapping("/chat.join")
    public void joinChat(
            ChatActionMessageParams messageParams,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor // ← добавлен параметр, Spring сам его инжектит
    ) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());

        // presence: обновляем "где юзер онлайн" + рассылаем событие в
        // /topic/location.{id}.presence для LocationUsersMenu
        presenceService.enterRoom(
                headerAccessor.getSessionId(),
                chatUser.getId(),
                Long.parseLong(messageParams.getLocationId()),
                Long.parseLong(messageParams.getRoomId())
        );

        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.JOIN, principal.getName());
        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── индикатор печати ────────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void typing(ChatActionMessageParams message, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.TYPING, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                messageResponse
        );
    }

    // ─── heartbeat для presence ─────────────────────────────────────────────
    // новый обработчик — фронт шлёт это раз в 10 сек, пока WS открыт.
    // Не требует ChatMessageParams вообще — только sessionId из заголовков
    @MessageMapping("/presence.heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        presenceService.heartbeat(headerAccessor.getSessionId());
    }
}