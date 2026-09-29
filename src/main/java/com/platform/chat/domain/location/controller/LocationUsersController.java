package com.platform.chat.domain.location.controller;

import com.platform.chat.domain.chatUser.service.ChatUserService;
import com.platform.chat.domain.location.dto.LocationSystemRoleView;
import com.platform.chat.domain.location.dto.LocationUserResponse;
import com.platform.chat.domain.location.dto.LocationUsersPagedResponse;
import com.platform.chat.domain.location.entity.LocationUser;
import com.platform.chat.domain.location.service.LocationUsersService;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.enums.SystemRoleType;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Состав локации и системные должности — под ресурсом {@code /api/location-users}.
 *
 * <p>Состояние комнат (непрочитанное и присутствие) здесь больше не раздаётся:
 * эти ручки переехали в слайс-владелец данных
 * ({@code chat.engines.chatRoom.controller.RoomStateController}) и живут под
 * тем же URL-префиксом, поэтому контракт с фронтом не менялся.</p>
 */
@RestController
@RequestMapping("/api/location-users")
@RequiredArgsConstructor
public class LocationUsersController {

    private final LocationUsersService locationUsersService;
    private final ChatUserRepository chatUserRepository;
    private final ChatUserService chatUserService;

    @GetMapping("/{locationId}/users")
    public LocationUsersPagedResponse<LocationUserResponse> getLocationUsers(
            @PathVariable Long locationId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return locationUsersService.getLocationUsers(locationId, query, filter, page, size);
    }

    /** Все занимаемые должности в системной локации. */
    @GetMapping("/{locationId}/system-roles")
    public List<LocationSystemRoleView> getSystemRolesByLocation(@PathVariable Long locationId) {
        return locationUsersService.getSystemRolesByLocation(locationId);
    }

    /** Все должности персонажа. Чужой персонаж недоступен (id приходит из запроса). */
    @GetMapping("/by-character/{characterId}/system-roles")
    public List<LocationSystemRoleView> getSystemRolesByCharacter(
            @PathVariable Long characterId,
            Principal principal) {
        chatUserService.assertCharacterOwnedBy(principal.getName(), characterId);
        return locationUsersService.getSystemRolesByCharacter(characterId);
    }

    /**
     * Назначить персонажа на системную должность.
     * Только ADMIN платформы или глава локации (проверка в сервисе).
     */
    @PostMapping("/appoint-system-role")
    public LocationUser appointSystemRole(
            @RequestParam Long locationId,
            @RequestParam Long characterId,
            @RequestParam SystemRoleType systemRole,
            Principal principal) {
        Long actorCharacterId = resolveActor(principal);
        return locationUsersService.appointSystemRole(locationId, characterId, systemRole, actorCharacterId);
    }

    /**
     * Снять должность с персонажа.
     */
    @DeleteMapping("/remove-system-role")
    public void removeSystemRole(
            @RequestParam Long locationId,
            @RequestParam Long characterId,
            Principal principal) {
        Long actorCharacterId = resolveActor(principal);
        locationUsersService.removeSystemRole(locationId, characterId, actorCharacterId);
    }

    /** id персонажа по username из principal (null, если principal пуст). */
    private Long resolveActor(Principal principal) {
        if (principal == null) return null;
        ChatUser character = chatUserRepository.findByUsername(principal.getName()).orElse(null);
        return character != null ? character.getId() : null;
    }
}