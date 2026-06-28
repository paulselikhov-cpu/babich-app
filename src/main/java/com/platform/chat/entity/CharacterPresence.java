package com.platform.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Присутствие персонажа — в какой комнате он сейчас онлайн.
 * Обновляется при каждом переходе между комнатами через WebSocket.
 *
 * Именно здесь реализуется механика BASIC/PREMIUM:
 * - BASIC персонаж может иметь только 1 запись в этой таблице
 * - PREMIUM персонаж может иметь несколько записей (несколько локаций онлайн)
 *
 * При выходе из комнаты — запись удаляется.
 * При отключении WebSocket — запись удаляется автоматически.
 */
@Data
@Entity
@Table(name = "character_presence")
public class CharacterPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Персонаж который сейчас онлайн в этой комнате.
     * OneToOne с точки зрения BASIC персонажа,
     * OneToMany с точки зрения PREMIUM.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private ChatUser character;

    /**
     * Локация в которой находится персонаж.
     * Нужна для быстрого получения всех онлайн персонажей локации.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Конкретная комната в которой находится персонаж.
     * Персонаж может быть в локации, но не в конкретной комнате —
     * например в "холле" при входе.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    /**
     * Когда персонаж вошёл в эту комнату.
     * Нужно для статистики и мониторинга.
     */
    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @PrePersist
    public void prePersist() {
        this.enteredAt = LocalDateTime.now();
    }
}
