package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.enums.CivicRole;
import com.platform.chat.modules.work.enums.Profession;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Персонаж чата — независимая сущность, не наследующая User.
 * Один User может иметь несколько ChatUser (по одному на район).
 * Связь с User — через поле userId.
 */
@Data
@Entity
@Table(name = "chat_users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"character_name", "district_id"},
        name = "uq_chat_user_character_name_district"),
    @UniqueConstraint(columnNames = {"user_id", "district_id"},
        name = "uq_chat_user_user_district")
})
public class ChatUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ссылка на владельца (User).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Имя персонажа в игре — отличается от username.
     * Уникально в рамках района (в разных районах могут быть одинаковые имена).
     */
    @Column(name = "character_name", nullable = false)
    private String characterName;

    /**
     * Username владельца (из User) — продублировано для WS-уведомлений,
     * чтобы не делать JOIN каждый раз при отправке сообщения.
     */
    @Column(nullable = false)
    private String username;

    @Column(name = "passport_id")
    private String passportId;

    @Column(name = "work_licence_id")
    private String workLicenceId;

    /**
     * Район к которому привязан персонаж.
     * Один User = максимум один персонаж в одном районе.
     */
    @Column(name = "district_id", nullable = false)
    private Long districtId;

    /**
     * Глобальная роль персонажа на локации.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role = ChatRole.USER;

    /**
     * Cоциальная роль персонажа на всей платформе.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CivicRole civicRole = CivicRole.UNEMPLOYED;

    /**
     * Рейтинг персонажа.
     */
    @Column(nullable = false)
    private Integer rating = 0;

    /**
     * Баланс внутренней валюты Coin.
     */
    @Column(name = "coin_balance", nullable = false)
    private Long coinBalance = 0L;

    // Поля профиля персонажа
    private String gender;
    private Double weight;
    private Double height;

    @Column(name = "bad_habits")
    private String badHabits;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Когда персонаж последний раз был онлайн.
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    /**
     * Накопительный XP персонажа (личный прогресс).
     */
    @Column(name = "xp_user", nullable = false)
    private Long xpUser = 0L;

    /**
     * XP персонажа в группировке (личный прогресс).
     */
    @Column(name = "xp_gang", nullable = false)
    private Long xpGang = 0L;

    /**
     * Текущая энергия персонажа (0–100).
     */
    @Column(name = "energy", nullable = false)
    private Integer energy = 100;

    /**
     * Время последнего обновления энергии.
     */
    @Column(name = "energy_last_updated")
    private LocalDateTime energyLastUpdated;

    /**
     * Игровой статус персонажа.
     * HOMELESS = бомж (начальный статус, нет своей локации).
     * RESIDENT = житель (есть хотя бы одна своя локация).
     */
    @Column(name = "game_status", nullable = false, length = 20) ///TODO - вырезать
    private String gameStatus = "HOMELESS";

    /**
     * Профессия персонажа.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "profession", nullable = false, length = 30)
    private Profession profession = Profession.DVORNIK;

    /**
     * Локации которыми владеет персонаж.
     * mappedBy указывает на поле owner в Location — @JoinColumn только на той стороне.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Location> ownedLocations;

    /**
     * Локации в которых состоит персонаж.
     * mappedBy указывает на поле character в LocationUser — @JoinColumn только на той стороне.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "character", fetch = FetchType.LAZY)
    private List<LocationUser> locationMemberships;
}