package com.platform.chat.controller;

import com.platform.chat.dto.CreateChatUserRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.service.ChatUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat/users")
@RequiredArgsConstructor
public class ChatUserController {

    private final ChatUserService chatUserService;

    @GetMapping
    public List<ChatUser> getAllChatUsers() {
        return chatUserService.getAllChatUsers();
    }

    @GetMapping("/{id}")
    public ChatUser getChatUserById(@PathVariable Long id) {
        return chatUserService.getChatUserById(id);
    }

    @GetMapping("/top")
    public List<ChatUser> getTopChatUsers() {
        return chatUserService.getTopChatUsers();
    }

    // создать персонажа для авторизованного пользователя
    @PostMapping("/{userId}")
    public ResponseEntity<ChatUser> createChatUser(
            @PathVariable Long userId,
            @Valid @RequestBody CreateChatUserRequest request) {
        return ResponseEntity.ok(chatUserService.createChatUser(userId, request));
    }

    // обновить рейтинг
    @PatchMapping("/{userId}/rating")
    public ResponseEntity<ChatUser> updateRating(
            @PathVariable Long userId,
            @RequestParam Integer delta) {
        return ResponseEntity.ok(chatUserService.updateRating(userId, delta));
    }
}