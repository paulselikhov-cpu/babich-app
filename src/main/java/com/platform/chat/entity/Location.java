package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Реальная локация — экземпляр созданный пользователем из шаблона.
 * Например пользователь "Иван" создал свою "Хрущевку" → это Location.
 * У другого пользователя "Петр" тоже может быть своя "Хрущевка" — другой Location,
 * но из того же LocationTemplate.
 *
 * Аналогия с Java: LocationTemplate = класс, Location = new LocationTemplate()
 */
@Data
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Шаблон из которого создана эта локация.
     * Нужен чтобы знать базовые настройки (можно ли достраивать, цену и тд).
     * ManyToOne = много локаций создано из одного шаблона.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private LocationTemplate template;

    /**
     * Владелец локации — персонаж который её создал.
     * Он автоматически получает роль OWNER в location_members.
     * ManyToOne = у одного персонажа может быть много локаций.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private ChatUser owner;

    /**
     * Кастомное название локации — владелец сам придумывает.
     * Например: "Квартира Ивана", "Клуб 'Рюмочная'"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание локации — владелец пишет сам.
     * Показывается в списке публичных локаций.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Уникальный код для приглашения.
     * Генерируется автоматически при создании локации.
     * Пример: "abc123xy"
     * Ссылка для приглашения: localhost/chat/join/abc123xy
     */
    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    /**
     * Публичная или приватная локация.
     * true = видна всем в списке публичных локаций, можно вступить по ссылке.
     * false = только по прямой ссылке-приглашению.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    /**
     * Когда была создана локация.
     * Устанавливается автоматически через @PrePersist.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Список комнат этой локации.
     * При создании локации сюда копируются комнаты из TemplateRoom.
     * Если шаблон расширяемый — сюда же добавляются новые комнаты.
     * CascadeType.ALL = при удалении локации удаляются все её комнаты.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Room> rooms;

    /**
     * Список участников локации.
     * Через эту связь можно получить всех кто вступил в локацию.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationMember> members;

    /**
     * Автоматически устанавливает дату создания перед сохранением в БД.
     * @PrePersist = вызывается Hibernate один раз перед INSERT.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}