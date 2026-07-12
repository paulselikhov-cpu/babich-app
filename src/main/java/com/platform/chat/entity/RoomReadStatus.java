package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Отметка "персонаж X прочитал комнату Y до момента T".
 * Непрочитанные = сообщения комнаты с created_at > lastReadAt (и не от самого персонажа).
 * Обновляется автоматически при входе в комнату — см. PresenceService.enterRoom.
 */
@Data
@Entity
@Table(
        name = "room_read_status",
        uniqueConstraints = @UniqueConstraint(columnNames = {"character_id", "room_id"})
)
public class RoomReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private ChatUser character;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;
}