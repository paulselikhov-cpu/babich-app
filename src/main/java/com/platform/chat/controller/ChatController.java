package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessageParams;
import com.platform.chat.dto.ChatMessageResponse;
import com.platform.chat.enums.MessageActionType;
import com.platform.chat.repository.ChatUserRepository;
import com.platform.chat.service.ChatUserService;
import com.platform.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import com.platform.chat.entity.ChatUser;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatUserRepository chatUserRepository;
    private final ChatUserService chatUserService;

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
    @MessageMapping("/chat.join")
    public void joinChat(ChatMessageParams messageParams, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.JOIN, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + messageParams.getRoomId(),
                messageResponse
        );
    }

    // ─── покинул комнату ─────────────────────────────────────────────────
    @MessageMapping("/chat.leave")
    public void leaveChat(ChatMessageParams message, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.LEAVE, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                messageResponse
        );
    }

    // ─── индикатор печати ────────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void typing(ChatMessageParams message, Principal principal) {
        ChatMessageResponse messageResponse = ChatMessageResponse.getResponseByActionType(MessageActionType.TYPING, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId(),
                messageResponse
        );
    }
}