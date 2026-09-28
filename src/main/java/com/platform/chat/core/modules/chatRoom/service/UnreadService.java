package com.platform.chat.core.modules.chatRoom.service;

import com.platform.chat.base.location.entity.LocationUser;
import com.platform.chat.base.room.entity.Room;
import com.platform.chat.base.room.entity.RoomReadStatus;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.base.location.repository.LocationUserRepository;
import com.platform.chat.base.room.repository.RoomReadStatusRepository;
import com.platform.chat.base.room.repository.RoomRepository;

import com.platform.chat.core.modules.chatRoom.repository.MessageRepository;
import com.platform.chat.core.modules.chatRoom.repository.OnlineSessionRepository;
import com.platform.chat.core.modules.chatRoom.dto.UnreadUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UnreadService {

    private final RoomReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final LocationUserRepository locationUserRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineSessionRepository onlineSessionRepository;
    private final ChatUserRepository chatUserRepository;

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    public long getUnreadCount(Long characterId, Long roomId) {
        LocalDateTime lastReadAt = readStatusRepository.findByCharacterIdAndRoomId(characterId, roomId)
                .map(RoomReadStatus::getLastReadAt)
                .orElse(EPOCH);

        return messageRepository.countByRoomIdAndCreatedAtAfterAndCharacter_IdNotAndIsDeletedFalse(
                roomId, lastReadAt, characterId
        );
    }

    public LocationUnreadSummary getLocationUnreadSummary(Long characterId, Long locationId) {
        List<Room> rooms = roomRepository.findByLocationIdOrderByOrderIndex(locationId);
        Map<Long, Long> perRoom = new LinkedHashMap<>();
        long total = 0;
        for (Room room : rooms) {
            long count = getUnreadCount(characterId, room.getId());
            perRoom.put(room.getId(), count);
            total += count;
        }
        return new LocationUnreadSummary(perRoom, total);
    }

    /** Bulk-версия для сайдбара — по аналогии с getOnlineCounts. Может лучше сделать получение за один вызов или избыточно? */
    public Map<Long, Long> getUnreadTotalsForLocations(Long characterId, List<Long> locationIds) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Long locationId : locationIds) {
            result.put(locationId, getLocationUnreadSummary(characterId, locationId).total());
        }
        return result;
    }

    /**
     * Рассылает обновлённый unread-счётчик по комнате ВСЕМ участникам локации,
     * кроме автора нового сообщения — каждому в его личную очередь.
     * Вызывается сразу после сохранения сообщения.
     */
    // UnreadService.java

    @Transactional(readOnly = true)
    public void notifyRoomUnreadChanged(Long roomId, Long authorCharacterId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return;

        Long locationId = room.getLocation().getId();
        List<LocationUser> members = locationUserRepository.findByLocationId(locationId);

        for (LocationUser member : members) {
            Long characterId = member.getCharacter().getId();
            if (characterId.equals(authorCharacterId)) continue;

            // если участник прямо сейчас находится в этой же комнате — он и так видит
            // сообщение вживую через свою открытую ChatArea, бейдж ему прибавлять не нужно
            boolean isCurrentlyInThisRoom = onlineSessionRepository.findByCharacterId(characterId)
                    .map(p -> p.getRoom() != null && p.getRoom().getId().equals(roomId))
                    .orElse(false);
            if (isCurrentlyInThisRoom) {
                markRead(characterId, roomId); // молча двигаем lastReadAt, без события — бейдж и так уже 0 у них
                continue;
            }

            long count = getUnreadCount(characterId, roomId);
            String username = member.getCharacter().getUsername();

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/unread-updates",
                    new UnreadUpdateResponse(roomId, locationId, count)
            );
        }
    }

    /**
     * Уведомляет САМОГО пользователя (например, другие открытые вкладки),
     * что комната только что помечена прочитанной — счётчик сброшен в 0.
     */
    public void notifySelfRoomRead(Room room, Long characterId, String username) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/unread-updates",
                new UnreadUpdateResponse(room.getId(), room.getLocation().getId(), 0L)
        );
    }

    public record LocationUnreadSummary(Map<Long, Long> unreadByRoomId, long total) {}

    public void markRead(Long characterId, Long roomId) {
        RoomReadStatus status = readStatusRepository.findByCharacterIdAndRoomId(characterId, roomId)
                .orElseGet(() -> {
                    RoomReadStatus s = new RoomReadStatus();
                    s.setCharacter(chatUserRepository.getReferenceById(characterId));
                    s.setRoom(roomRepository.getReferenceById(roomId));
                    return s;
                });
        status.setLastReadAt(LocalDateTime.now());
        readStatusRepository.save(status);
    }
}