package com.platform.chat.base.location.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.district.entity.District;
import com.platform.chat.base.location.enums.LocationCategoryType;
import com.platform.chat.modules.gang.entity.Gang;
import com.platform.chat.base.location.enums.PublicLocationType;
import com.platform.chat.base.room.entity.Room;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Локация — может быть как пользовательской (созданной из шаблона), так и системной
 * (принадлежит району, создаётся автоматически).
 *
 * Пользовательская локация:
 * - Создаётся игроком из LocationTemplate
 * - Имеет владельца (owner), invite_code, is_public
 * - Пример: "Квартира Ивана", "Клуб 'Рюмочная'"
 *
 * Системная локация (isSystem = true):
 * - Создаётся системой при инициализации района
 * - Не имеет владельца, invite_code, is_public
 * - Имеет тип (PublicLocationType) — мэрия, банк, полиция и т.д.
 * - Доступна всем жителям района без приглашения
 * - Пример: "Центральная площадь", "Полицейский участок"
 */
@Data
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Шаблон из которого создана эта локация (только для пользовательских).
     * ManyToOne = много локаций создано из одного шаблона.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private LocationTemplate template;

    /**
     * Владелец локации — персонаж который её создал (только для пользовательских).
     * У системных локаций owner = null.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private ChatUser owner;

    /**
     * Район, в котором находится эта локация.
     * Каждая локация (и пользовательская, и системная) всегда привязана к району.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    /**
     * Название локации.
     * Пользовательские — владелец придумывает сам.
     * Системные — задаются системой (например "Мэрия", "Банк").
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание локации.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Флаг: системная локация (true) или пользовательская (false).
     * Системные создаются автоматически при инициализации района.
     */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /**
     * Тип системной локации (только для isSystem = true).
     * Для пользовательских локаций — null.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private PublicLocationType type;

    /**
     * Категория локации. Проставляется всем при создании:
     * BUSINESS / HOME — пользовательские, системным — при инициализации района.
     * Термин не смешивать с {@link #type} (PublicLocationType — только системные).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private LocationCategoryType category;

    /**
     * XP самой локации как игрового объекта (прокачка заведения).
     * Только для пользовательских локаций; системным не начисляется.
     */
    @Column(name = "xp_location", nullable = false)
    private Long xpLocation = 0L;

    /**
     * Идентификатор группировки, владеющей локацией. Без FK на {@link Gang}
     * (минимальная заготовка) — целостность на стороне сервиса.
     * При появлении полноценной модели Gang — перевести на связь.
     */
    @Column(name = "gang_id")
    private Long gangId;

    /**
     * Уникальный код для приглашения (только для пользовательских локаций).
     */
    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    /**
     * Публичная или приватная локация (только для пользовательских).
     * true = видна всем в списке публичных локаций.
     * false = только по прямой ссылке-приглашению.
     */
    @Column(name = "is_public")
    private Boolean isPublic = true;

    /**
     * Когда была создана локация.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Список комнат этой локации.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Room> rooms;

    /**
     * Список участников локации (только для пользовательских).
     */
    @JsonIgnore
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationUser> users;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}