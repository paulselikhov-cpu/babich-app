package com.platform.chat.domain.chatUser.repository;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.enums.ChatRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {

    // найти по имени персонажа (глобально — для админ-панели)
    Optional<ChatUser> findByCharacterName(String characterName);

    // проверить уникальность имени персонажа глобально (для админ-панели)
    boolean existsByCharacterName(String characterName);

    // все персонажи по роли
    List<ChatUser> findByRole(ChatRole role);

    // топ по рейтингу
    List<ChatUser> findAllByOrderByRatingDesc();

    // найти всех персонажей пользователя (теперь может быть несколько — по одному на район)
    @Query("SELECT c FROM ChatUser c WHERE c.userId = :userId")
    List<ChatUser> findAllByUserId(@Param("userId") Long userId);

    // найти персонажа пользователя в конкретном районе
    @Query("SELECT c FROM ChatUser c WHERE c.userId = :userId AND c.districtId = :districtId")
    Optional<ChatUser> findByUserIdAndDistrictId(@Param("userId") Long userId, @Param("districtId") Long districtId);

    // проверить существует ли персонаж у пользователя в районе
    @Query("SELECT COUNT(c) > 0 FROM ChatUser c WHERE c.userId = :userId AND c.districtId = :districtId")
    boolean existsByUserIdAndDistrictId(@Param("userId") Long userId, @Param("districtId") Long districtId);

    // проверить уникальность имени персонажа в рамках района
    @Query("SELECT COUNT(c) > 0 FROM ChatUser c WHERE c.characterName = :characterName AND c.districtId = :districtId")
    boolean existsByCharacterNameAndDistrictId(@Param("characterName") String characterName, @Param("districtId") Long districtId);

    // найти персонажа по username (дублируется из User для WS-уведомлений)
    Optional<ChatUser> findByUsername(String username);

    // все персонажи района
    List<ChatUser> findByDistrictId(Long districtId);

    // количество живых персонажей в районе (были онлайн последние 2 суток)
    long countByDistrictIdAndLastSeenAfter(Long districtId, LocalDateTime since);
}
