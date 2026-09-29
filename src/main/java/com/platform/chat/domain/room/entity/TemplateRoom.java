package com.platform.chat.base.room.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.platform.chat.base.location.entity.LocationTemplate;
import com.platform.chat.base.room.enums.RoomCode;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Базовая комната шаблона локации.
 * Например для шаблона "Хрущевка" будет 5 записей:
 * - Кухня (order_index=1)
 * - Спальня (order_index=2)
 * - Зал (order_index=3)
 * - Туалет (order_index=4)
 * - Прихожая (order_index=5)
 *
 * Когда пользователь создаёт локацию из шаблона — эти записи
 * копируются в таблицу rooms как реальные комнаты локации.
 */
@Data
@Entity
@Table(name = "template_rooms")
public class TemplateRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomCode code = RoomCode.DEFAULT;

    /**
     * Шаблон которому принадлежит эта комната.
     * ManyToOne = много комнат принадлежат одному шаблону.
     * FetchType.LAZY = шаблон не загружается автоматически — только по запросу.
     */
    @JsonBackReference // эта сторона НЕ сериализуется — разрываем цикл
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private LocationTemplate template;

    /**
     * Название комнаты.
     * Например: "Кухня", "Спальня", "Главный зал"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание комнаты — атмосфера, особенности.
     * Показывается пользователям при входе в комнату.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Порядок отображения комнаты в списке.
     * Меньше = выше в списке.
     * Например: кухня=1, спальня=2, зал=3...
     */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;
}