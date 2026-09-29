package com.platform.chat.core.modules.chatRoom.repository;

import com.platform.chat.core.modules.chatRoom.entity.OnlineSession;
import com.platform.chat.core.modules.chatRoom.repository.projection.LocationOnlineCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineSessionRepository extends JpaRepository<OnlineSession, Long> {

    // у персонажа ровно одна запись или ни одной — Optional, а не List
    Optional<OnlineSession> findByCharacterId(Long characterId);

    List<OnlineSession> findByRoomId(Long roomId);

    List<OnlineSession> findByLocationId(Long locationId);

    long countByLocationId(Long locationId); // для счётчика на кнопке 👥

    // главный ключ для лайфцикла соединения — по нему находим "чья это была сессия"
    Optional<OnlineSession> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);

    // "протухшие" сессии — backstop для случаев, когда close-фрейм не пришёл вообще
    List<OnlineSession> findByLastHeartbeatAtBefore(LocalDateTime threshold);

    // bulk-update без загрузки сущности — легче для очень частого вызова (раз в 10 сек на юзера)
    @Modifying
    @Query("UPDATE OnlineSession p SET p.lastHeartbeatAt = :now WHERE p.sessionId = :sessionId")
    int touchHeartbeat(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    // чистка при рестарте сервера — все presence-записи после него мгновенно
    // указывают на несуществующие сессии
    @Modifying
    @Query("DELETE FROM OnlineSession")
    void deleteAllPresences();

    @Query("SELECT p.character.id FROM OnlineSession p WHERE p.location.id = :locationId")
    List<Long> findCharacterIdsByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT p.location.id AS locationId, COUNT(p) AS count " +
            "FROM OnlineSession p WHERE p.location.id IN :locationIds " +
            "GROUP BY p.location.id")
    List<LocationOnlineCountProjection> countByLocationIdIn(@Param("locationIds") List<Long> locationIds);
}