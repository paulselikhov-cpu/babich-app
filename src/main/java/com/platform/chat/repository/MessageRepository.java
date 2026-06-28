package com.platform.chat.repository;

import com.platform.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сообщениями чата.
 *
 * Важно: сообщения сохраняются только если Room.storeHistory = true.
 * Если storeHistory = false — этот репозиторий не используется.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * История сообщений комнаты с пагинацией.
     * Пагинация критична — в активной комнате могут быть тысячи сообщений,
     * загружать всё сразу нельзя.
     *
     * Pageable — объект с номером страницы и размером.
     * Пример: PageRequest.of(0, 50) — первые 50 сообщений.
     *
     * Page<Message> — результат с метаданными:
     * сколько всего сообщений, сколько страниц и тд.
     */
    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.character
        WHERE m.room.id = :roomId
        AND m.isDeleted = false
        ORDER BY m.createdAt DESC
    """)
    Page<Message> findByRoomIdOrderByCreatedAtDesc(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    // количество непрочитанных сообщений в комнате
    // (сообщения после определённого времени)
    long countByRoomIdAndIsDeletedFalse(Long roomId);
}