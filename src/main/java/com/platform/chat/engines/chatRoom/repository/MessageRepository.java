package com.platform.chat.engines.chatRoom.repository;

import com.platform.chat.engines.chatRoom.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

    long countByRoomIdAndCreatedAtAfterAndCharacter_IdNotAndIsDeletedFalse(
            Long roomId, LocalDateTime after, Long characterId
    );

    /**
     * Физическое удаление сообщений комнат — вызывается при удалении комнаты/локации
     * через порт {@code base.room.port.RoomContentPurger}.
     *
     * <p>Одним bulk-запросом, а не перебором сущностей: во-первых, это быстрее,
     * во-вторых, FK {@code messages.reply_to_id → messages.id} (реплаи внутри
     * той же комнаты) проверяется в конце оператора — перебор по одной строке
     * мог бы упасть на порядке удаления «родитель раньше реплая».</p>
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.room.id IN :roomIds")
    int deleteByRoomIds(@Param("roomIds") List<Long> roomIds);
}