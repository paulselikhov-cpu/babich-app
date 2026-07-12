package com.platform.chat.repository;

import com.platform.chat.entity.CharacterPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterPresenceRepository extends JpaRepository<CharacterPresence, Long> {

    // у персонажа ровно одна запись или ни одной — Optional, а не List
    Optional<CharacterPresence> findByCharacterId(Long characterId);

    List<CharacterPresence> findByRoomId(Long roomId);

    List<CharacterPresence> findByLocationId(Long locationId);

    long countByLocationId(Long locationId); // для счётчика на кнопке 👥

    // главный ключ для лайфцикла соединения — по нему находим "чья это была сессия"
    Optional<CharacterPresence> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);

    // "протухшие" сессии — backstop для случаев, когда close-фрейм не пришёл вообще
    List<CharacterPresence> findByLastHeartbeatAtBefore(LocalDateTime threshold);

    // bulk-update без загрузки сущности — легче для очень частого вызова (раз в 10 сек на юзера)
    @Modifying
    @Query("UPDATE CharacterPresence p SET p.lastHeartbeatAt = :now WHERE p.sessionId = :sessionId")
    int touchHeartbeat(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    // чистка при рестарте сервера — все presence-записи после него мгновенно
    // указывают на несуществующие сессии
    @Modifying
    @Query("DELETE FROM CharacterPresence")
    void deleteAllPresences();

    @Query("SELECT p.character.id FROM CharacterPresence p WHERE p.location.id = :locationId")
    List<Long> findCharacterIdsByLocationId(@Param("locationId") Long locationId);
}