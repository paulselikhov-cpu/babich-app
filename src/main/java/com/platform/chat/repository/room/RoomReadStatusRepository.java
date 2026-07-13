package com.platform.chat.repository.room;

import com.platform.chat.entity.RoomReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoomReadStatusRepository extends JpaRepository<RoomReadStatus, Long> {
    Optional<RoomReadStatus> findByCharacterIdAndRoomId(Long characterId, Long roomId);
}