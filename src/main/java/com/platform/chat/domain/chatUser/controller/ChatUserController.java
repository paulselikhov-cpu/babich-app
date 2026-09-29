package com.platform.chat.base.chatUser.controller;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.service.ChatUserService;
import com.platform.chat.base.chatUser.dto.CharacterUpdateRequest;
import com.platform.chat.base.chatUser.dto.CreateChatUserRequest;
import com.platform.chat.base.chatUser.dto.RoleUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}/level")
    public Long getChatUserLevelById(@PathVariable Long id) { 
        return chatUserService.getChatUserLevelById(id);
    }

    @GetMapping("/{userId}/characters")
    public ResponseEntity<ChatUser> getChatUserByUserIdAndDistrict(
            @PathVariable Long userId,
            @RequestParam Long districtId) {
        return chatUserService.findChatUserByUserIdAndDistrictId(userId, districtId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top")
    public List<ChatUser> getTopChatUsers() {
        return chatUserService.getTopChatUsers();
    }

    /** Порог XP следующего уровня персонажа*/
    @GetMapping("/{id}/xp-threshold")
    public Long getChatUserXpThreshold(@PathVariable Long id) {
        return chatUserService.getChatUserXpThresholdById(id);
    }

    /** Список персонажей района — для админской таблицы. */
    @GetMapping("/district/{districtId}")
    public ResponseEntity<List<ChatUser>> listDistrictCharacters(
            @PathVariable Long districtId,
            Principal principal) {
        if (isNotAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(chatUserService.listByDistrict(districtId));
    }

    /** Создать персонажа для авторизованного пользователя в указанном районе. Только ADMIN. */
    @PostMapping("/{userId}/characters")
    public ResponseEntity<ChatUser> createChatUser(
            @PathVariable Long userId,
            @Valid @RequestBody CreateChatUserRequest request) {
        return ResponseEntity.ok(chatUserService.createChatUser(userId, request));
    }

    /** Сменить роль персонажа. Только ADMIN. */
    @PutMapping("/{characterId}/role")
    public ResponseEntity<?> updateRole(
            @PathVariable Long characterId,
            @RequestBody RoleUpdateRequest request,
            Principal principal) {
        if (isNotAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (request.role() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Роль не указана"));
        }
        try {
            return ResponseEntity.ok(chatUserService.updateRole(characterId, request.role()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Обновить поля персонажа. Только ADMIN. null в теле — не менять поле. */
    @PutMapping("/{characterId}/settings")
    public ResponseEntity<?> updateCharacterSettings(
            @PathVariable Long characterId,
            @RequestBody CharacterUpdateRequest request,
            Principal principal) {
        if (isNotAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(chatUserService.updateCharacterSettings(characterId, request));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private boolean isNotAdmin(Principal principal) {
        return principal == null || !chatUserService.isAdmin(principal.getName());
    }
}