package com.platform.chat.controller.webSocket;

import com.platform.chat.dto.request.ChatActionMessageRequest;
import com.platform.chat.dto.request.ChatMessageRequest;
import com.platform.chat.dto.response.ChatMessageResponse;
import com.platform.chat.entity.Room;
import com.platform.chat.enums.MessageActionType;
import com.platform.chat.repository.room.RoomRepository;
import com.platform.chat.service.chatUser.ChatUserService;
import com.platform.chat.service.message.MessageService;
import com.platform.chat.service.message.UnreadService;
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
    private final UnreadService unreadService;

    // ─── отправить сообщение ──────────────────────────────────────────────
    @MessageMapping("/chat.history.send")
    public void sendMessage(ChatMessageRequest messageParams, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());

        ChatMessageResponse messageResponse = messageService.saveByUser(
                chatUser,
                messageParams.getContent(),
                messageParams.getRoomId(),
                messageParams.getReplyToId()
        );

        if (messageResponse == null) return;

        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );

        unreadService.notifyRoomUnreadChanged(Long.parseLong(messageParams.getRoomId()), chatUser.getId());
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

        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.JOIN, chatUser);
        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── индикатор печати ────────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void typing(ChatActionMessageRequest message, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.TYPING, chatUser);

        messagingTemplate.convertAndSend(
                "/topic/typing." + message.getRoomId(),
                messageResponse
        );
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(ChatActionMessageRequest messageParams, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        presenceService.leaveRoom(
                chatUser.getId(),
                Long.parseLong(messageParams.getLocationId()),
                Long.parseLong(messageParams.getRoomId())
        );
    }

    // ─── heartbeat для presence ─────────────────────────────────────────────
    // новый обработчик — фронт шлёт это раз в 10 сек, пока WS открыт.
    // Не требует ChatMessageParams вообще — только sessionId из заголовков
    @MessageMapping("/presence.heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        presenceService.heartbeat(headerAccessor.getSessionId());
    }

    @MessageMapping("/chat.read")
    public void markRead(ChatActionMessageRequest messageParams, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        unreadService.markRead(chatUser.getId(), Long.parseLong(messageParams.getRoomId()));
    }
}