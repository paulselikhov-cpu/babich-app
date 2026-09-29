package com.platform.chat.domain.location.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.location.enums.LocationUserRole;
import com.platform.chat.domain.chatUser.enums.SystemRoleType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Связь между персонажем и локацией — «пользователь локации».
 * Это таблица-мост для связи многие-ко-многим между ChatUser и Location.
 *
 * Аналогия с реальной жизнью:
 * - Location = дом/клуб
 * - ChatUser = человек
 * - LocationUser = запись в домовой книге
 *
 * Здесь также хранится роль персонажа ВНУТРИ конкретной локации.
 * Это отличается от глобальной роли ChatUser.role:
 * - ChatUser.role = ADMIN → администратор всей платформы
 * - LocationUser.role = OWNER → хозяин только этой локации
 *
 * В системных локациях запись появляется только у штата: держатель должности
 * ({@code systemRole} не null) — это и есть «member» такой локации.
 */
@Data
@Entity
@Table(name = "location_users",
        // составной первичный ключ: один персонаж не может быть
        // дважды в одной локации
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"location_id", "character_id"}
        )
)
public class LocationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Локация в которой состоит персонаж.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Персонаж который состоит в локации.
     */
    @JsonIgnore
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
    private LocationUserRole role = LocationUserRole.MEMBER;

    /**
     * "Прописан" ли персонаж в этой локации.
     * true = владелец назначил его модератором/прописал
     * false = обычный участник
     * Прописанный персонаж отображается в "жильцах" локации.
     */
    @Column(name = "is_registered", nullable = false)
    private Boolean isRegistered = false;

    /**
     * Должность персонажа в системной локации. null = обычный участник.
     * Перенос из упразднённой таблицы location_posts (LocationPost).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", length = 50)
    private SystemRoleType systemRole;

    /**
     * Когда персонаж был назначен на должность (переезд из LocationPost.appointedAt).
     */
    @Column(name = "appointed_at")
    private LocalDateTime appointedAt;

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