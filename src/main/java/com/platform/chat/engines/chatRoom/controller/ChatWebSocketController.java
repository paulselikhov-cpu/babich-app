package com.platform.chat.engines.chatRoom.controller;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.service.ChatUserService;
import com.platform.chat.engines.chatRoom.dto.ChatActionMessageRequest;
import com.platform.chat.engines.chatRoom.dto.ChatMessageRequest;
import com.platform.chat.engines.chatRoom.dto.MessageResponse;
import com.platform.chat.engines.chatRoom.enums.MessageActionType;
import com.platform.chat.engines.chatRoom.service.MessageService;
import com.platform.chat.engines.chatRoom.service.OnlineSessionService;
import com.platform.chat.engines.chatRoom.service.UnreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatUserService chatUserService;
    private final OnlineSessionService onlineSessionService;
    private final UnreadService unreadService;

    // ─── отправить сообщение ──────────────────────────────────────────────
    @MessageMapping("/chat.history.send")
    public void sendMessage(ChatMessageRequest messageParams, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());

        MessageResponse messageResponse = messageService.saveByUser(
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
    // и обновляет OnlineSession (presence: где юзер сейчас online)
    @MessageMapping("/chat.join")
    public void joinChat(
            ChatActionMessageRequest messageParams,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());

        onlineSessionService.enterRoom(
                headerAccessor.getSessionId(),
                chatUser.getId(),
                Long.parseLong(messageParams.getLocationId()),
                Long.parseLong(messageParams.getRoomId())
        );

        MessageResponse messageResponse = MessageResponse.getResponseByActionType(MessageActionType.JOIN, chatUser);
        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── индикатор печати ────────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void typing(ChatActionMessageRequest message, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        MessageResponse messageResponse = MessageResponse.getResponseByActionType(MessageActionType.TYPING, chatUser);

        messagingTemplate.convertAndSend(
                "/topic/typing." + message.getRoomId(),
                messageResponse
        );
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(ChatActionMessageRequest messageParams, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        onlineSessionService.leaveRoom(
                chatUser.getId(),
                Long.parseLong(messageParams.getLocationId()),
                Long.parseLong(messageParams.getRoomId())
        );
    }

    // ─── heartbeat для presence ─────────────────────────────────────────────
    @MessageMapping("/presence.heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        onlineSessionService.heartbeat(headerAccessor.getSessionId());
    }

    @MessageMapping("/chat.read")
    public void markRead(ChatActionMessageRequest messageParams, Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        unreadService.markRead(chatUser.getId(), Long.parseLong(messageParams.getRoomId()));
    }
}
