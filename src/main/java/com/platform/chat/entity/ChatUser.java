package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.auth.entity.User;
import com.platform.chat.enums.ChatRole;
import com.platform.economy.enums.Profession;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Персонаж чата — расширяет базового User из модуля auth.
 * Один User = один ChatUser (персонаж).
 * Таблица chat_users связана с таблицей users через user_id (JOINED inheritance).
 * Это значит что при запросе Hibernate делает JOIN двух таблиц автоматически.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chat_users")
@PrimaryKeyJoinColumn(name = "user_id") // user_id = FK и PK одновременно
public class ChatUser extends User {

    /**
     * Имя персонажа в игре — отличается от username.
     * Username = логин для входа, characterName = имя в ролевой игре.
     * Например username="ivan123", characterName="Иван Петрович"
     */
    @Column(name = "character_name", nullable = false, unique = true)
    private String characterName;

    /**
     * Глобальная роль персонажа на всей платформе.
     * ADMIN — полный доступ ко всему
     * MODERATOR — может модерировать любую локацию
     * USER — обычный персонаж
     * GUEST — ограниченный доступ
     * Не путать с ролью внутри локации (location_members.role)
     */
    @Enumerated(EnumType.STRING) // хранить как строку "USER", не как число
    @Column(nullable = false)
    private ChatRole role = ChatRole.USER;

    /**
     * Рейтинг персонажа — растёт от активности и оценок других игроков.
     * В будущем будет влиять на получение Coin и доступные возможности.
     */
    @Column(nullable = false)
    private Integer rating = 0;

    /**
     * Баланс внутренней валюты Coin.
     * Зарабатывается за активность в чате.
     * Тратится на покупку локаций, комнат, функций персонажа.
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
     * Обновляется при каждом WebSocket подключении/отключении.
     * Используется модулем мониторинга.
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    /**
     * Накопительный XP персонажа (личный прогресс).
     * Растёт от работы, активности, выполнения действий.
     */
    @Column(name = "xp_user", nullable = false)
    private Long xpUser = 0L;

    /**
     * Текущая энергия персонажа (0–100).
     * Тратится на действия (работа, кражи, эскорт и т.д.).
     * Восстанавливается пассивно со временем.
     */
    @Column(name = "energy", nullable = false)
    private Integer energy = 100;

    /**
     * Время последнего обновления энергии.
     * Используется EnergyService для расчёта пассивного восстановления.
     */
    @Column(name = "energy_last_updated")
    private LocalDateTime energyLastUpdated;

    /**
     * Игровой статус персонажа.
     * HOMELESS = бомж (начальный статус, нет своей локации).
     * RESIDENT = житель (есть хотя бы одна своя локация).
     */
    @Column(name = "game_status", nullable = false, length = 20)
    private String gameStatus = "HOMELESS";

    /**
     * Профессия персонажа — определяет доступные механики тап-фарма.
     * По умолчанию DVORNIK для новых персонажей (бомжи метут улицы).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "profession", nullable = false, length = 30)
    private Profession profession = Profession.DVORNIK;

    /**
     * Локации которыми владеет персонаж (создал сам).
     * mappedBy = "owner" означает что связь описана в классе Location
     * через поле owner.
     * FetchType.LAZY = локации не загружаются пока явно не запросишь —
     * это важно для производительности.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Location> ownedLocations;

    /**
     * Локации в которых состоит персонаж (вступил по ссылке).
     * Связь многие-ко-многим через таблицу location_members.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "character", fetch = FetchType.LAZY)
    private List<LocationMember> locationMemberships;
}