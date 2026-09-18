package com.platform.chat.core.modules.onlineSession.repository;

import com.platform.chat.core.modules.onlineSession.entity.onlineSessions;
import com.platform.chat.core.modules.onlineSession.repository.projection.LocationOnlineCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterPresenceRepository extends JpaRepository<onlineSessions, Long> {

    // у персонажа ровно одна запись или ни одной — Optional, а не List
    Optional<onlineSessions> findByCharacterId(Long characterId);

    List<onlineSessions> findByRoomId(Long roomId);

    List<onlineSessions> findByLocationId(Long locationId);

    long countByLocationId(Long locationId); // для счётчика на кнопке 👥

    // главный ключ для лайфцикла соединения — по нему находим "чья это была сессия"
    Optional<onlineSessions> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);

    // "протухшие" сессии — backstop для случаев, когда close-фрейм не пришёл вообще
    List<onlineSessions> findByLastHeartbeatAtBefore(LocalDateTime threshold);

    // bulk-update без загрузки сущности — легче для очень частого вызова (раз в 10 сек на юзера)
    @Modifying
    @Query("UPDATE onlineSessions p SET p.lastHeartbeatAt = :now WHERE p.sessionId = :sessionId")
    int touchHeartbeat(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    // чистка при рестарте сервера — все presence-записи после него мгновенно
    // указывают на несуществующие сессии
    @Modifying
    @Query("DELETE FROM onlineSessions")
    void deleteAllPresences();

    @Query("SELECT p.character.id FROM onlineSessions p WHERE p.location.id = :locationId")
    List<Long> findCharacterIdsByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT p.location.id AS locationId, COUNT(p) AS count " +
            "FROM onlineSessions p WHERE p.location.id IN :locationIds " +
            "GROUP BY p.location.id")
    List<LocationOnlineCountProjection> countByLocationIdIn(@Param("locationIds") List<Long> locationIds);
}