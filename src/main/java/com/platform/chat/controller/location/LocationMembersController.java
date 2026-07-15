package com.platform.chat.controller.location;

import com.platform.chat.dto.response.LocationUserResponse;
import com.platform.chat.dto.response.LocationMembersPagedResponse;
import com.platform.chat.service.location.LocationMembersService;
import com.platform.chat.service.onlineSessions.PresenceService;
import com.platform.chat.service.message.UnreadService;
import com.platform.chat.service.message.UnreadService.LocationUnreadSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Всё, что касается состава/присутствия/непрочитанного одной локации — под одним ресурсом. */
@RestController
@RequestMapping("/api/location-members")
@RequiredArgsConstructor
public class LocationMembersController {

    private final LocationMembersService locationMembersService;
    private final UnreadService unreadService;
    private final PresenceService presenceService;

    @GetMapping("/{locationId}/users")
    public LocationMembersPagedResponse<LocationUserResponse> getLocationUsers(
            @PathVariable Long locationId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return locationMembersService.getLocationUsers(locationId, query, filter, page, size);
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
}