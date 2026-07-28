package com.platform.chat.controller.location;

import com.platform.chat.entity.LocationPost;
import com.platform.chat.enums.LocationPostType;
import com.platform.chat.service.location.LocationPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления должностями в системных локациях.
 */
@RestController
@RequestMapping("/api/location-posts")
@RequiredArgsConstructor
public class LocationPostController {

    private final LocationPostService locationPostService;

    /**
     * Назначить персонажа на должность.
     */
    @PostMapping("/appoint")
    public ResponseEntity<LocationPost> appoint(
            @RequestParam Long locationId,
            @RequestParam Long characterId,
            @RequestParam LocationPostType post) {
        LocationPost locationPost = locationPostService.appoint(locationId, characterId, post);
        return ResponseEntity.ok(locationPost);
    }

    /**
     * Снять персонажа с должности.
     */
    @DeleteMapping("/{postId}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable Long postId) {
        locationPostService.dismiss(postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получить все должности в системной локации.
     */
    @GetMapping("/by-location/{locationId}")
    public ResponseEntity<List<LocationPost>> getPostsByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(locationPostService.getPostsByLocation(locationId));
    }

    /**
     * Получить все должности персонажа.
     */
    @GetMapping("/by-character/{characterId}")
    public ResponseEntity<List<LocationPost>> getPostsByCharacter(@PathVariable Long characterId) {
        return ResponseEntity.ok(locationPostService.getPostsByCharacter(characterId));
    }
}