package com.platform.chat.base.room.repository;

import com.platform.chat.base.room.entity.RoomReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomReadStatusRepository extends JpaRepository<RoomReadStatus, Long> {
    Optional<RoomReadStatus> findByCharacterIdAndRoomId(Long characterId, Long roomId);

    void deleteByRoomIdIn(List<Long> roomIds);
}
