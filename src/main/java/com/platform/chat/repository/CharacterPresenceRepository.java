package com.platform.chat.repository;

import com.platform.chat.entity.CharacterPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для отслеживания присутствия персонажей онлайн.
 * Эта таблица — "живая": записи добавляются при входе в комнату
 * и удаляются при выходе или отключении WebSocket.
 *
 * Именно здесь реализуется механика BASIC/PREMIUM:
 * BASIC персонаж → максимум 1 запись в этой таблице
 * PREMIUM персонаж → несколько записей (несколько локаций онлайн)
 */
@Repository
public interface CharacterPresenceRepository extends JpaRepository<CharacterPresence, Long> {

    // найти где сейчас находится конкретный персонаж
    // для BASIC — одна запись, для PREMIUM — несколько
    List<CharacterPresence> findByCharacterId(Long characterId);

    // все персонажи онлайн в конкретной комнате
    // используется для отображения списка "кто сейчас здесь"
    List<CharacterPresence> findByRoomId(Long roomId);

    // все персонажи онлайн в конкретной локации
    List<CharacterPresence> findByLocationId(Long locationId);

    // найти конкретное присутствие персонажа в локации
    // используется при смене комнаты внутри локации
    Optional<CharacterPresence> findByCharacterIdAndLocationId(
            Long characterId,
            Long locationId
    );

    // удалить все присутствия персонажа
    // вызывается при отключении WebSocket
    void deleteByCharacterId(Long characterId);

    // количество активных локаций персонажа
    // сравниваем с лимитом character_type.max_online
    long countByCharacterId(Long characterId);
}