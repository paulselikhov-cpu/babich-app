package com.platform.chat.repository.chatUser;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.ChatRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {

    // найти по имени персонажа
    Optional<ChatUser> findByCharacterName(String characterName);

    // проверить уникальность имени персонажа
    boolean existsByCharacterName(String characterName);

    // все персонажи по роли
    List<ChatUser> findByRole(ChatRole role);

    // топ по рейтингу
    List<ChatUser> findAllByOrderByRatingDesc();

    // найти персонажа по userId (он же PK через наследование)
    @Query("SELECT c FROM ChatUser c WHERE c.id = :userId")
    Optional<ChatUser> findByUserId(Long userId);

    /**
     * Найти персонажа по username пользователя.
     * Используется при сохранении сообщения —
     * из JWT получаем username, находим ChatUser.
     */
    @Query("""
        SELECT c FROM ChatUser c
        WHERE c.username = :username
    """)
    Optional<ChatUser> findByUsername(@Param("username") String username);
}