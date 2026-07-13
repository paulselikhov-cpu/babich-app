package com.platform.chat.service.message;

import com.platform.chat.entity.RoomReadStatus;
import com.platform.chat.entity.Room;
import com.platform.chat.repository.message.MessageRepository;
import com.platform.chat.repository.room.RoomReadStatusRepository;
import com.platform.chat.repository.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    // если персонаж никогда не открывал комнату — считаем непрочитанным всё с начала времён
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    public long getUnreadCount(Long characterId, Long roomId) {
        LocalDateTime lastReadAt = readStatusRepository.findByCharacterIdAndRoomId(characterId, roomId)
                .map(RoomReadStatus::getLastReadAt)
                .orElse(EPOCH);

        return messageRepository.countByRoomIdAndCreatedAtAfterAndCharacter_IdNotAndIsDeletedFalse(
                roomId, lastReadAt, characterId
        );
    }

    /** Бейджи для всей локации: непрочитанные по каждой комнате + сумма для бейджа локации. */
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

    public record LocationUnreadSummary(Map<Long, Long> unreadByRoomId, long total) {}
}