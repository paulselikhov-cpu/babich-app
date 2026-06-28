package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Шаблон локации — это "чертёж" из которого создаются реальные локации.
 * Например шаблон "Хрущевка" содержит 5 комнат: кухня, спальня, зал, туалет, прихожая.
 * Когда пользователь создаёт свою локацию — он выбирает шаблон и получает копию
 * со всеми базовыми комнатами.
 *
 * Аналогия: шаблон = класс в Java, локация = объект (экземпляр класса).
 */
@Data
@Entity
@Table(name = "location_templates")
public class LocationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный код шаблона для программного использования.
     * Например: "KHRUSHCHEVKA", "CASTLE", "TAVERN"
     * Используется в коде вместо id для читаемости.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    /**
     * Отображаемое название шаблона.
     * Например: "Хрущевка", "Замок", "Таверна"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание атмосферы и особенностей локации.
     * Показывается пользователю при выборе шаблона.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * URL картинки шаблона — показывается в списке выбора локаций.
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Стоимость шаблона в Coin.
     * 0 = бесплатный шаблон (например Хрущевка).
     * >0 = платный шаблон (например Особняк).
     */
    @Column(name = "price_coin", nullable = false)
    private Long priceCoin = 0L;

    /**
     * Можно ли добавлять свои комнаты сверх базовых.
     * true = расширяемая (например Частный дом — можно достраивать).
     * false = фиксированная (например Хрущевка — только базовые комнаты).
     */
    @Column(name = "is_extendable", nullable = false)
    private Boolean isExtendable = false;

    /**
     * Стоимость добавления одной новой комнаты в Coin.
     * Актуально только если isExtendable = true.
     * null = нельзя добавлять комнаты.
     */
    @Column(name = "extend_price_coin")
    private Long extendPriceCoin;

    /**
     * Список базовых комнат этого шаблона.
     * При создании локации из шаблона — эти комнаты копируются в реальную локацию.
     * mappedBy = "template" означает что связь описана в TemplateRoom.
     * CascadeType.ALL = при удалении шаблона удаляются и его комнаты.
     */
    @JsonManagedReference // эта сторона сериализуется
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TemplateRoom> templateRooms;
}