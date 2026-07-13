package com.platform.chat.controller;

import com.platform.chat.dto.*;
import com.platform.chat.entity.*;
import com.platform.chat.service.LocationService;
import com.platform.chat.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST контроллер для локаций.
 *
 * Эндпоинты:
 * GET  /api/locations              — публичные локации
 * GET  /api/locations/my/{characterId} — мои локации
 * POST /api/locations              — создать локацию
 * POST /api/locations/join/{code}  — вступить по ссылке
 * POST /api/locations/{id}/rooms   — добавить комнату
 * GET  /api/locations/{id}/rooms   — комнаты локации
 * PUT  /api/locations/rooms/{roomId}/history — настройка истории
 * POST /api/locations/{id}/moderator — назначить модератора
 */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final RoomService roomService;

    /**
     * GET /api/locations
     * Список всех публичных локаций.
     * Показываем на главной странице чата.
     */
    @GetMapping
    public List<Location> getPublicLocations() {
        return locationService.getPublicLocations();
    }

    /**
     * GET /api/locations/my/{characterId}
     * Локации в которых состоит персонаж.
     * Показываем в боковом меню — "мои локации".
     */
    @GetMapping("/my/{characterId}")
    public List<LocationResponse> getMyLocations(@PathVariable Long characterId) {
        return locationService.getMyLocations(characterId);
    }

    /**
     * POST /api/locations
     * Создать новую локацию из шаблона.
     *
     * Тело запроса:
     * {
     *   "templateId": 1,
     *   "name": "Квартира Ивана",
     *   "description": "Уютная хрущевка",
     *   "isPublic": true,
     *   "characterId": 1
     * }
     */
    @PostMapping
    public ResponseEntity<Location> createLocation(
            @Valid @RequestBody CreateLocationRequest request) {
        Location location = locationService.createLocation(
                request.getTemplateId(),
                request.getCharacterId(),
                request.getName(),
                request.getDescription(),
                request.getIsPublic()
        );
        return ResponseEntity.ok(location);
    }

    /**
     * POST /api/locations/join/{inviteCode}
     * Вступить в локацию по invite_code из ссылки.
     *
     * Тело запроса:
     * { "characterId": 1 }
     *
     * Ссылка для приглашения выглядит так:
     * localhost/chat/join/abc123xy
     * где abc123xy = invite_code
     */
    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<LocationMember> joinLocation(
            @PathVariable String inviteCode,
            @RequestParam Long characterId) {
        LocationMember member = locationService.joinByInviteCode(
                inviteCode,
                characterId
        );
        return ResponseEntity.ok(member);
    }

    /**
     * GET /api/locations/{locationId}/rooms
     * Список комнат локации отсортированных по порядку.
     * Вызывается при открытии локации.
     */
    @GetMapping("/{locationId}/rooms")
    public List<Room> getRooms(@PathVariable Long locationId) {
        return roomService.getRoomsByLocation(locationId);
    }

    /**
     * POST /api/locations/{locationId}/rooms
     * Добавить кастомную комнату в расширяемую локацию.
     * Только для владельца. Стоит Coin.
     *
     * Тело запроса:
     * {
     *   "name": "Гараж",
     *   "description": "Место для машины",
     *   "characterId": 1
     * }
     */
    @PostMapping("/{locationId}/rooms")
    public ResponseEntity<Room> addRoom(
            @PathVariable Long locationId,
            @Valid @RequestBody AddRoomRequest request) {
        Room room = locationService.addCustomRoom(
                locationId,
                request.getCharacterId(),
                request.getName(),
                request.getDescription()
        );
        return ResponseEntity.ok(room);
    }

    /**
     * POST /api/locations/{locationId}/moderator/{targetCharacterId}
     * Назначить модератора ("прописать") в локации.
     * Только для владельца.
     *
     * @param ownerId — id персонажа который назначает (владелец)
     * @param targetCharacterId — id персонажа которого назначают
     */
    @PostMapping("/{locationId}/moderator/{targetCharacterId}")
    public ResponseEntity<LocationMember> appointModerator(
            @PathVariable Long locationId,
            @PathVariable Long targetCharacterId,
            @RequestParam Long ownerId) {
        LocationMember member = locationService.appointModerator(
                locationId,
                ownerId,
                targetCharacterId
        );
        return ResponseEntity.ok(member);
    }

    // покинуть локацию
    @DeleteMapping("/{locationId}/leave")
    public ResponseEntity<Void> leaveLocation(
            @PathVariable Long locationId,
            @RequestParam Long characterId) {
        locationService.leaveLocation(locationId, characterId);
        return ResponseEntity.ok().build();
    }

    // удалить локацию (только владелец)
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable Long locationId,
            @RequestParam Long characterId) {
        locationService.deleteLocation(locationId, characterId);
        return ResponseEntity.ok().build();
    }
}