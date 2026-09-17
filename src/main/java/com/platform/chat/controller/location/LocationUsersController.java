package com.platform.chat.controller.location;

import com.platform.chat.dto.request.LocationSystemRoleView;
import com.platform.chat.dto.response.LocationUserResponse;
import com.platform.chat.dto.response.LocationUsersPagedResponse;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.LocationUser;
import com.platform.chat.enums.SystemRoleType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.service.location.LocationUsersService;
import com.platform.chat.service.onlineSessions.PresenceService;
import com.platform.chat.service.message.UnreadService;
import com.platform.chat.service.message.UnreadService.LocationUnreadSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** Всё, что касается состава/присутствия/непрочитанного одной локации — под одним ресурсом. */
@RestController
@RequestMapping("/api/location-users")
@RequiredArgsConstructor
public class LocationUsersController {

    private final LocationUsersService locationUsersService;
    private final UnreadService unreadService;
    private final PresenceService presenceService;
    private final ChatUserRepository chatUserRepository;

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

    /** Все должности персонажа. */
    @GetMapping("/by-character/{characterId}/system-roles")
    public List<LocationSystemRoleView> getSystemRolesByCharacter(@PathVariable Long characterId) {
        return locationUsersService.getSystemRolesByCharacter(characterId);
    }

    // characterId — временно как query-параметр, заменить на извлечение из Principal,
    // когда будет готова авторизация персонажей на уровне REST
    @GetMapping("/{locationId}/unread")
    public LocationUnreadSummary getUnreadSummary(
            @PathVariable Long locationId, @RequestParam Long characterId
    ) {
        return unreadService.getLocationUnreadSummary(characterId, locationId);
    }

    // начальное число для бейджа 👥 N на кнопке-триггере — дальше обновляется
    // в реальном времени через WS-события, этот эндпоинт нужен только один раз при открытии
    @GetMapping("/{locationId}/online-count")
    public long getOnlineCount(@PathVariable Long locationId) {
        return presenceService.getOnlineCount(locationId);
    }

    @GetMapping("/{locationId}/online-ids")
    public List<Long> getOnlineCharacterIds(@PathVariable Long locationId) {
        return presenceService.getOnlineCharacterIds(locationId);
    }

    //Получение активных пользователей для списка локаций в сайдбаре
    @GetMapping("/online-counts")
    public Map<Long, Long> getOnlineCounts(@RequestParam List<Long> locationIds) {
        return presenceService.getOnlineCounts(locationIds);
    }
    
    @GetMapping("/unread-counts")
    public Map<Long, Long> getUnreadCounts(
            @RequestParam List<Long> locationIds, @RequestParam Long characterId
    ) {
        return unreadService.getUnreadTotalsForLocations(characterId, locationIds);
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