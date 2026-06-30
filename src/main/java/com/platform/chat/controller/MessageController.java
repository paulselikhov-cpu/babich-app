package com.platform.chat.controller;

import com.platform.chat.entity.Message;
import com.platform.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST контроллер для истории сообщений.
 * Загружается при открытии комнаты — один раз через HTTP.
 * Новые сообщения приходят через WebSocket.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /api/messages/room/{roomId}?page=0&size=50
     * История сообщений комнаты с пагинацией.
     * page=0 = последние 50 сообщений.
     */
    @GetMapping("/room/{roomId}")
    public List<Message> getHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return messageService.getHistory(roomId, page, size);
    }
}