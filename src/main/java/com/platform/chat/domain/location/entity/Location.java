package com.platform.chat.domain.location.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.district.entity.District;
import com.platform.chat.domain.location.enums.LocationType;
import com.platform.chat.domain.room.entity.Room;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Локация — может быть как пользовательской (созданной игроком), так и системной
 * (принадлежит району, создаётся сидером автоматически).
 *
 * Пользовательская локация:
 * - Создаётся игроком из LocationTemplate по адресу /chat/location/create
 * - Имеет владельца (owner), придуманный игроком code, invite_code, is_public
 * - {@link LocationType#DEFAULT} (или BUSINESS под будущий механизм)
 * - Пример: "Квартира Ивана", "Клуб 'Рюмочная'"
 *
 * Системная локация ({@link LocationType#SYSTEM}):
 * - Создаётся `data.sql` при инициализации района
 * - Не имеет владельца, invite_code, is_public
 * - Её конкретный вид задаётся полем {@code code}
 *   (см. {@link com.platform.chat.domain.location.enums.SystemLocationCode})
 * - Доступна всем жителям района без приглашения
 * - Пример: "Мэрия" (CITY_HALL), "Площадь Ленина" (LENIN_SQUARE)
 *
 * <p>Отличие системной от пользовательской определяется полем {@link #type},
 * а не отдельным флагом {@code is_system} — флаг был удалён.</p>
 */
@Data
@Entity
@Table(
        name = "locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_locations_district_code",
                columnNames = {"district_id", "code"}
        )
)
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
     * Код локации — то, что определяет конкретную локацию.
     *
     * <p>Уникален в пределах района: {@code UNIQUE (district_id, code)}.
     * Район работает как «сервер» в онлайн-игре — у каждого района своя
     * «Площадь Ленина», и персонаж подключён ровно к одному району.
     * Поэтому код резолвится только внутри района текущего персонажа.</p>
     *
     * <p>У системных локаций — одна из констант {@link com.platform.chat.domain.location.enums.SystemLocationCode}
     * (задаётся сидером). У пользовательских — код, придуманный игроком при
     * создании (латиница, заглавные, цифры и «_»).</p>
     */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /**
     * Тип локации (классификация {@link LocationType}: SYSTEM / DEFAULT / BUSINESS).
     * Конкретная разновидность задаётся полем {@link #code}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private LocationType type;

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