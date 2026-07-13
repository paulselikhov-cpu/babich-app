package com.platform.chat.repository.room;

import com.platform.chat.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Репозиторий для работы с комнатами локации.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // все комнаты конкретной локации отсортированные по порядку
    // используется при открытии локации — показываем список комнат
    List<Room> findByLocationIdOrderByOrderIndex(Long locationId);

    // только кастомные комнаты локации (добавленные владельцем)
    List<Room> findByLocationIdAndIsCustomTrue(Long locationId);

    // количество комнат в локации
    // нужно чтобы знать order_index для новой комнаты
    long countByLocationId(Long locationId);
}