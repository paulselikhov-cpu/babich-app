package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.enums.LocationPostType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Должность персонажа в системной локации.
 * Связывает ChatUser (персонажа) с Location (системной локацией) через тип должности.
 *
 * Примеры:
 * - Иван назначен GOVERNOR в Мэрии
 * - Пётр назначен POLICE_OFFICER в Полицейском участке
 * - Анна назначена BANK_DIRECTOR в Банке
 */
@Data
@Entity
@Table(name = "location_posts",
        uniqueConstraints = {
                // Персонаж не может иметь две должности в одной локации
                @UniqueConstraint(columnNames = {"location_id", "character_id"}),
                // На единоличные должности (GOVERNOR, BANK_DIRECTOR) — только один человек
                @UniqueConstraint(columnNames = {"location_id", "post"})
        }
)
public class LocationPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Системная локация, в которой находится должность.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private com.platform.chat.entity.Location location;

    /**
     * Персонаж, занимающий должность.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private ChatUser character;

    /**
     * Тип должности (GOVERNOR, POLICE_OFFICER, BANK_DIRECTOR и т.д.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "post", nullable = false, length = 50)
    private LocationPostType post;

    /**
     * Когда персонаж был назначен на эту должность.
     */
    @Column(name = "appointed_at", nullable = false)
    private LocalDateTime appointedAt;

    @PrePersist
    public void prePersist() {
        this.appointedAt = LocalDateTime.now();
    }
}