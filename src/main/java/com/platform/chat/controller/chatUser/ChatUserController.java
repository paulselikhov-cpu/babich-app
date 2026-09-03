package com.platform.chat.controller.chatUser;

import com.platform.chat.dto.request.CharacterUpdateRequest;
import com.platform.chat.dto.request.CreateChatUserRequest;
import com.platform.chat.dto.request.RoleUpdateRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.service.chatUser.ChatUserService;
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


    /**
     * Порог XP следующего уровня персонажа — для прогресс-бара XP на UI.
     * GET /api/chat/users/{id}/xp-threshold
     */
    @GetMapping("/{id}/xp-threshold")
    public Long getChatUserXpThreshold(@PathVariable Long id) {
        return chatUserService.getChatUserXpThresholdById(id);
    }

    /**
     * Получить персонажа пользователя в конкретном районе.
     * GET /api/chat/users/{userId}/characters?districtId={districtId}
     */
    @GetMapping("/{userId}/characters")
    public ResponseEntity<ChatUser> getChatUserByUserIdAndDistrict(
            @PathVariable Long userId,
            @RequestParam Long districtId) {
        return chatUserService.findChatUserByUserIdAndDistrictId(userId, districtId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить всех персонажей пользователя.
     * GET /api/chat/users/{userId}/characters/all
     */
    @GetMapping("/{userId}/characters/all")
    public List<ChatUser> getAllChatUsersByUserId(@PathVariable Long userId) {
        return chatUserService.getAllChatUsersByUserId(userId);
    }

    /**
     * Получить персонажа по userId (первый найденный) — для обратной совместимости.
     * GET /api/chat/users/by-user/{userId}
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ChatUser> getChatUserByUserId(@PathVariable Long userId) {
        return chatUserService.findChatUserByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top")
    public List<ChatUser> getTopChatUsers() {
        return chatUserService.getTopChatUsers();
    }

    /**
     * Создать персонажа для авторизованного пользователя в указанном районе.
     * POST /api/chat/users/{userId}/characters
     */
    @PostMapping("/{userId}/characters")
    public ResponseEntity<ChatUser> createChatUser(
            @PathVariable Long userId,
            @Valid @RequestBody CreateChatUserRequest request) {
        return ResponseEntity.ok(chatUserService.createChatUser(userId, request));
    }

    /**
     * Обновить рейтинг персонажа в районе.
     * PATCH /api/chat/users/{userId}/characters/rating?districtId={districtId}&delta={delta}
     */
    @PatchMapping("/{userId}/characters/rating")
    public ResponseEntity<ChatUser> updateRating(
            @PathVariable Long userId,
            @RequestParam Long districtId,
            @RequestParam Integer delta) {
        return ResponseEntity.ok(chatUserService.updateRating(userId, districtId, delta));
    }

    // Для обратной совместимости — старый эндпоинт
    @PatchMapping("/{userId}/rating")
    public ResponseEntity<ChatUser> updateRatingLegacy(
            @PathVariable Long userId,
            @RequestParam Integer delta) {
        return ResponseEntity.ok(chatUserService.updateRating(userId, delta));
    }

    /**
     * Список персонажей района — для админской таблицы. Только ADMIN.
     * GET /api/chat/users/district/{districtId}
     */
    @GetMapping("/district/{districtId}")
    public ResponseEntity<List<ChatUser>> listDistrictCharacters(
            @PathVariable Long districtId,
            Principal principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(chatUserService.listByDistrict(districtId));
    }

    /**
     * Сменить роль персонажа. Только ADMIN.
     * PUT /api/chat/users/{characterId}/role
     */
    @PutMapping("/{characterId}/role")
    public ResponseEntity<?> updateRole(
            @PathVariable Long characterId,
            @RequestBody RoleUpdateRequest request,
            Principal principal) {
        if (!isAdmin(principal)) {
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

    /**
     * Обновить поля персонажа (роль, профессия, статус, монеты, энергия, рейтинг).
     * Только ADMIN. null в теле — не менять поле.
     * PUT /api/chat/users/{characterId}/settings
     */
    @PutMapping("/{characterId}/settings")
    public ResponseEntity<?> updateCharacterSettings(
            @PathVariable Long characterId,
            @RequestBody CharacterUpdateRequest request,
            Principal principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(chatUserService.updateCharacterSettings(characterId, request));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private boolean isAdmin(Principal principal) {
        return principal != null && chatUserService.isAdmin(principal.getName());
    }
}