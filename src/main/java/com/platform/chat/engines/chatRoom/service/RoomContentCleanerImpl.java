package com.platform.chat.core.modules.chatRoom.service;

import com.platform.chat.base.room.port.RoomContentCleaner;
import com.platform.chat.core.modules.chatRoom.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Реализация порта {@link RoomContentCleaner}: чистит сообщения комнат.
 *
 * <p>Живёт в слайсе чата, потому что сообщения — его данные: репозиторий
 * сообщений наружу не отдаётся, база знает только контракт. Вызывается из
 * {@code LocationService.deleteLocation} синхронно перед удалением комнат.</p>
 */
@Service
@RequiredArgsConstructor
public class RoomContentCleanerImpl implements RoomContentCleaner {

    private final MessageRepository messageRepository;

    @Override
    public void cleanRooms(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }
        messageRepository.deleteByRoomIds(roomIds);
    }
}
