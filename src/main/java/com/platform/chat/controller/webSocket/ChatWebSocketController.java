package com.platform.chat.controller.webSocket;

import com.platform.chat.dto.request.ChatActionMessageRequest;
import com.platform.chat.dto.request.ChatMessageRequest;
import com.platform.chat.dto.response.ChatMessageResponse;
import com.platform.chat.enums.MessageActionType;
import com.platform.chat.service.chatUser.ChatUserService;
import com.platform.chat.service.message.MessageService;
import com.platform.chat.service.onlineSessions.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import com.platform.chat.entity.ChatUser;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatUserService chatUserService;
    private final PresenceService presenceService;

    // ─── отправить сообщение ──────────────────────────────────────────────
    @MessageMapping("/chat.history.send")
    public void sendMessage(ChatMessageRequest messageParams, Principal principal) {

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
            ChatActionMessageRequest messageParams,
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
    public void typing(ChatActionMessageRequest message, Principal principal) {
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