package com.platform.chat.engines.chatRoom.controller;

import com.platform.chat.domain.chatUser.service.ChatUserService;
import com.platform.chat.engines.chatRoom.service.OnlineSessionService;
import com.platform.chat.engines.chatRoom.service.UnreadService;
import com.platform.chat.engines.chatRoom.service.UnreadService.LocationUnreadSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Состояние комнат локации: непрочитанное и присутствие (онлайн).
 *
 * <p>Ручки переехали сюда из {@code base.location.controller.LocationUsersController}:
 * данные принадлежат слайсу чата комнаты, поэтому и REST-фасад должен жить рядом
 * с сервисами, а не в базовом модуле локаций. URL-префикс сохранён
 * ({@code /api/location-users}), отдельные пути — те же, поэтому фронт не
 * затрагивается; контроллеры делят префикс по разным подпутям.</p>
 *
 * <p>Часть ответов дублирует то, что уже приходит по WS (присутствие и бейджи
 * обновляются в реальном времени); REST-ручки нужны для первичной загрузки —
 * фронт помечает {@code getOnlineCount}/{@code getOnlineCharacterIds} как
 * {@code @not_used}.</p>
 */
@RestController
@RequestMapping("/api/location-users")
@RequiredArgsConstructor
public class RoomStateController {

    private final UnreadService unreadService;
    private final OnlineSessionService onlineSessionService;
    private final ChatUserService chatUserService;

    /** Разбивка непрочитанных по комнатам локации. {@code characterId} — только свой. */
    @GetMapping("/{locationId}/unread")
    public LocationUnreadSummary getUnreadSummary(
            @PathVariable Long locationId, @RequestParam Long characterId, Principal principal
    ) {
        chatUserService.assertCharacterOwnedBy(principal.getName(), characterId);
        return unreadService.getLocationUnreadSummary(characterId, locationId);
    }

    // начальное число для бейджа 👥 N на кнопке-триггере — дальше обновляется
    // в реальном времени через WS-события, этот эндпоинт нужен только один раз при открытии
    @GetMapping("/{locationId}/online-count")
    public long getOnlineCount(@PathVariable Long locationId) {
        return onlineSessionService.getOnlineCount(locationId);
    }

    @GetMapping("/{locationId}/online-ids")
    public List<Long> getOnlineCharacterIds(@PathVariable Long locationId) {
        return onlineSessionService.getOnlineCharacterIds(locationId);
    }

    //Получение активных пользователей для списка локаций в сайдбаре
    @GetMapping("/online-counts")
    public Map<Long, Long> getOnlineCounts(@RequestParam List<Long> locationIds) {
        return onlineSessionService.getOnlineCounts(locationIds);
    }

    /** Суммарные непрочитанные по локациям (бейджи сайдбара). {@code characterId} — только свой. */
    @GetMapping("/unread-counts")
    public Map<Long, Long> getUnreadCounts(
            @RequestParam List<Long> locationIds, @RequestParam Long characterId, Principal principal
    ) {
        chatUserService.assertCharacterOwnedBy(principal.getName(), characterId);
        return unreadService.getUnreadTotalsForLocations(characterId, locationIds);
    }
}
