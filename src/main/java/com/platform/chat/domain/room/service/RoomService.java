package com.platform.chat.domain.room.service;

import com.platform.chat.domain.room.entity.Room;
import com.platform.chat.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Сервис для работы с комнатами.
 * Основная логика создания/удаления комнат — в LocationService.
 * Здесь только чтение и обновление.
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    /**
     * Получить все комнаты локации отсортированные по порядку.
     * Вызывается при открытии локации — показываем список комнат.
     */
    public List<Room> getRoomsByLocation(Long locationId) {
        return roomRepository.findByLocationIdOrderByOrderIndex(locationId);
    }

    /**
     * Получить комнату по id.
     * Вызывается при входе персонажа в комнату.
     */
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException(
                        "Комната не найдена: " + roomId
                ));
    }
}