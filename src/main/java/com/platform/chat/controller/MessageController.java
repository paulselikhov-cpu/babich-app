package com.platform.chat.controller;

import com.platform.chat.dto.ChatMessageResponse;
import com.platform.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
    public List<ChatMessageResponse> getHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return messageService.getHistory(roomId, page, size);
    }

    /**
     * DELETE /api/messages/{id}
     * Удалить одно сообщение (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long id,
            Principal principal) {
        messageService.deleteMessage(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/messages/batch
     * Удалить несколько сообщений сразу.
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteMessages(
            @RequestBody List<Long> messageIds,
            Principal principal) {
        messageService.deleteMessages(messageIds, principal.getName());
        return ResponseEntity.ok().build();
    }
}