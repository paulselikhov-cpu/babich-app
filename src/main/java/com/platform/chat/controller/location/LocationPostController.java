package com.platform.chat.controller.location;

import com.platform.chat.dto.request.LocationPostViewRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.LocationPost;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.enums.LocationPostType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.service.location.LocationPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Контроллер для управления должностями в системных локациях.
 * Назначение/снятие — только для персонажей с ролью ADMIN (роль берём
 * вызывающего из JWT principal, а не из target characterId).
 */
@RestController
@RequestMapping("/api/location-posts")
@RequiredArgsConstructor
public class LocationPostController {

    private final LocationPostService locationPostService;
    private final ChatUserRepository chatUserRepository;

    /**
     * Назначить персонажа на должность. Только ADMIN.
     * Идемпотентно: повторный вызов для уже занимающего эту же должность
     * возвращает существующую запись (не ошибку).
     */
    @PostMapping("/appoint")
    public ResponseEntity<LocationPost> appoint(
            @RequestParam Long locationId,
            @RequestParam Long characterId,
            @RequestParam LocationPostType post,
            Principal principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        LocationPost locationPost = locationPostService.appoint(locationId, characterId, post);
        return ResponseEntity.ok(locationPost);
    }

    /**
     * Снять персонажа с должности. Только ADMIN.
     */
    @DeleteMapping("/{postId}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable Long postId, Principal principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        locationPostService.dismiss(postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить все должности в системной локации (с именем занимающего).
     */
    @GetMapping("/by-location/{locationId}")
    public ResponseEntity<List<LocationPostViewRequest>> getPostsByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(locationPostService.getPostViewsByLocation(locationId));
    }

    /**
     * Получить все должности персонажа.
     */
    @GetMapping("/by-character/{characterId}")
    public ResponseEntity<List<LocationPost>> getPostsByCharacter(@PathVariable Long characterId) {
        return ResponseEntity.ok(locationPostService.getPostsByCharacter(characterId));
    }

    /** Вызывающий — персонаж с ролью ADMIN (паттерн DistrictSettingsController). */
    private boolean isAdmin(Principal principal) {
        if (principal == null) {
            return false;
        }
        ChatUser character = chatUserRepository.findByUsername(principal.getName()).orElse(null);
        return character != null && character.getRole() == ChatRole.ADMIN;
    }
}