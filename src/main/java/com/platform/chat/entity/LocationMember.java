package com.platform.chat.entity;

import com.platform.chat.enums.LocationMemberRole;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Связь между персонажем и локацией — "членство в локации".
 * Это таблица-мост для связи многие-ко-многим между ChatUser и Location.
 *
 * Аналогия с реальной жизнью:
 * - Location = дом/клуб
 * - ChatUser = человек
 * - LocationMember = запись в домовой книге
 *
 * Здесь также хранится роль персонажа ВНУТРИ конкретной локации.
 * Это отличается от глобальной роли ChatUser.role:
 * - ChatUser.role = ADMIN → администратор всей платформы
 * - LocationMember.role = OWNER → хозяин только этой локации
 */
@Data
@Entity
@Table(name = "location_members",
        // составной первичный ключ: один персонаж не может быть
        // дважды в одной локации
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"location_id", "character_id"}
        )
)
public class LocationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Локация в которой состоит персонаж.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Персонаж который состоит в локации.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private ChatUser character;

    /**
     * Роль персонажа внутри этой конкретной локации.
     * OWNER = создатель локации, полный контроль
     * MODERATOR = "прописан", может кикать участников
     * MEMBER = обычный участник
     *
     * Это локальная роль — только для этой локации.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationMemberRole role = LocationMemberRole.MEMBER;

    /**
     * "Прописан" ли персонаж в этой локации.
     * true = владелец назначил его модератором/прописал
     * false = обычный участник
     * Прописанный персонаж отображается в "жильцах" локации.
     */
    @Column(name = "is_registered", nullable = false)
    private Boolean isRegistered = false;

    /**
     * Когда персонаж вступил в локацию.
     */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }
}