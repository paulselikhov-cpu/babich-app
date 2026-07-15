package com.platform.chat.service.onlineSessions;

import com.platform.chat.dto.events.LocationPresenceCountEvent;
import com.platform.chat.dto.events.PresenceEvent;
import com.platform.chat.entity.CharacterPresence;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.Location;
import com.platform.chat.entity.Room;
import com.platform.chat.repository.onlineSessions.CharacterPresenceRepository;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationRepository;
import com.platform.chat.repository.room.RoomRepository;
import com.platform.chat.service.message.UnreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final CharacterPresenceRepository presenceRepository;
    private final ChatUserRepository chatUserRepository;
    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final UnreadService unreadService;
    private final SimpMessagingTemplate messagingTemplate;

    // держим с запасом ~4.5x от интервала heartbeat клиента (10 сек) —
    // не хотим ложно разлогинивать из-за пары пропущенных пакетов при лаге сети
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 45;


    /**
     * Было: @PostConstruct — не работает с @Transactional, т.к. вызывается
     * ДО того, как вокруг бина появляется AOP-прокси (который и открывает
     * транзакцию). Метод вызывался напрямую на "сыром" объекте, отсюда
     * TransactionRequiredException.
     *
     * Стало: @EventListener(ApplicationReadyEvent) — срабатывает после того,
     * как ВЕСЬ контекст, включая прокси, полностью инициализирован. Вызов
     * идёт уже через прокси, @Transactional работает штатно.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void clearStalePresenceOnStartup() {
        presenceRepository.deleteAllPresences();
        log.info("Очищены все presence-записи после рестарта сервера");
    }

    /**
     * Главный метод: персонаж вошёл в комнату.
     * Вызывается и при явном клике по комнате, и автоматически при входе
     * в локацию (фронт сам выбирает дефолтную комнату — "прихожую").
     *
     * Делает 3 вещи одним вызовом:
     *  1) обновляет/создаёт единственную presence-запись персонажа;
     *  2) отмечает комнату как прочитанную (вход = прочтение);
     *  3) рассылает presence-событие всем, кто смотрит на эту локацию.
     */
    @Transactional
    public void enterRoom(String sessionId, Long characterId, Long locationId, Long roomId) {
        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Персонаж не найден: " + characterId));
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Локация не найдена: " + locationId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена: " + roomId));

        CharacterPresence presence = presenceRepository.findByCharacterId(characterId)
                .orElseGet(CharacterPresence::new);

        Long previousLocationId = presence.getLocation() != null ? presence.getLocation().getId() : null;
        // ключевой момент: смена комнаты внутри той же локации — это НЕ вход в новую локацию
        boolean enteringNewLocation = !locationId.equals(previousLocationId);

        presence.setSessionId(sessionId);
        presence.setCharacter(character);
        presence.setCharacterId(character.getId());
        presence.setLocation(location);
        presence.setRoom(room);
        presence.setLastHeartbeatAt(LocalDateTime.now());
        presenceRepository.save(presence);

        // детальное событие — шлём ВСЕГДА, участники локации должны видеть смену комнаты
        broadcastPresenceDetail(locationId, characterId, true, roomId);

        // счётчик обновляем только при реальном входе в локацию, не при смене комнаты внутри неё
        if (enteringNewLocation) {
            broadcastPresenceCount(locationId, true);
        }

        // ушёл из прошлой локации целиком — гасим и там, и там
        if (previousLocationId != null && enteringNewLocation) {
            broadcastPresenceDetail(previousLocationId, characterId, false, null);
            broadcastPresenceCount(previousLocationId, false);
        }
    }

    /** Heartbeat от клиента (раз в 10 сек) — просто продлевает "жизнь" сессии. */
    @Transactional
    public void heartbeat(String sessionId) {
        int updated = presenceRepository.touchHeartbeat(sessionId, LocalDateTime.now());
        if (updated == 0) {
            // не ошибка — просто эта сессия уже "перехвачена" другой вкладкой
            // того же персонажа, её heartbeat больше никого не продлевает
            log.debug("Heartbeat от сессии {} проигнорирован — presence уже перезахвачен", sessionId);
        }
    }

    /**
     * Быстрый путь дисконнекта — явное закрытие вкладки / разрыв соединения.
     */
    @Transactional
    public void handleDisconnect(String sessionId) {
        presenceRepository.findBySessionId(sessionId).ifPresent(presence -> {
            // защита от гонки двух вкладок: если у персонажа уже открыта вторая
            // вкладка, она успела перезаписать sessionId в этой же строке через
            // enterRoom. Тогда findBySessionId(старый_sessionId) ничего не найдёт —
            // и это правильно, мы не должны сносить presence второй вкладки.
            Long locationId = presence.getLocation().getId();
            Long characterId = presence.getCharacter().getId();

            presenceRepository.deleteBySessionId(sessionId);
            broadcastPresenceDetail(locationId, characterId, false, null);
            broadcastPresenceCount(locationId, false);
        });
    }

    /**
     * Backstop для "тихого" обрыва: сеть упала, процесс убит, ноутбук уснул —
     * close-фрейм в этих случаях никогда не приходит. Раз в 15 сек ищем
     * сессии без heartbeat дольше таймаута и чистим их.
     */
    @Scheduled(fixedRate = 15_000)
    @Transactional
    public void cleanupStalePresences() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        var stale = presenceRepository.findByLastHeartbeatAtBefore(threshold);
        if (stale.isEmpty()) return;

        for (CharacterPresence presence : stale) {
            Long locationId = presence.getLocation().getId();
            Long characterId = presence.getCharacter().getId();
            presenceRepository.delete(presence);
            broadcastPresenceDetail(locationId, characterId, false, null);
            broadcastPresenceCount(locationId, false);
        }
        log.info("Очищено {} протухших presence-сессий по heartbeat-таймауту", stale.size());
    }

    // ─── рассылка событий подписчикам /topic/location.{id}.presence ───────────
    private void broadcastPresenceDetail(Long locationId, Long characterId, boolean online, Long roomId) {
        messagingTemplate.convertAndSend(
                "/topic/location." + locationId + ".presence",
                new PresenceEvent(characterId, online, roomId)
        );
    }

    private void broadcastPresenceCount(Long locationId, boolean online) {
        messagingTemplate.convertAndSend(
                "/topic/locations.presence-counts",
                new LocationPresenceCountEvent(locationId, online)
        );
    }

    public long getOnlineCount(@PathVariable Long locationId) {
        return presenceRepository.countByLocationId(locationId);
    }

    public List<Long> getOnlineCharacterIds(@PathVariable Long locationId) {
        return presenceRepository.findCharacterIdsByLocationId(locationId);
    }

    //Получение активных пользователей для списка локаций в сайдбаре
    public Map<Long, Long> getOnlineCounts(List<Long> locationIds) {
        if (locationIds.isEmpty()) return Map.of();

        Map<Long, Long> result = new HashMap<>();
        locationIds.forEach(id -> result.put(id, 0L)); // локации без онлайна тоже должны быть в ответе с 0

        presenceRepository.countByLocationIdIn(locationIds)
                .forEach(row -> result.put(row.getLocationId(), row.getCount()));

        return result;
    }

}