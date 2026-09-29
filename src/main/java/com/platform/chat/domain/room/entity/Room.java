package com.platform.chat.domain.room.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.room.enums.RoomCode;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Комната внутри локации — место где происходит чат.
 * Каждая локация содержит минимум одну комнату.
 * Базовые комнаты создаются автоматически из TemplateRoom при создании локации.
 * В расширяемых локациях владелец может добавлять свои комнаты за Coin.
 *
 * <p>История сообщений комнаты (entity {@code Message} слайса
 * {@code chat.engines.chatRoom}) здесь не отображается: обратной связи
 * {@code messages} нет сознательно — она не использовалась, а базовый модуль
 * комнат не должен зависеть от модуля чата. Сообщения удаляются вместе с
 * комнатой через cascade внутри самого слайса чата.</p>
 */
@Data
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomCode code = RoomCode.DEFAULT;

    /**
     * Локация, которой принадлежит комната.
     * Может быть как пользовательской, так и системной локацией.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Название комнаты.
     * Базовые комнаты получают название из TemplateRoom.
     * Кастомные комнаты — владелец называет сам.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание комнаты — атмосфера, что здесь происходит.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Хранить ли историю сообщений этой комнаты.
     * true = сообщения сохраняются в таблицу messages (можно читать историю).
     * false = сообщения только через WebSocket, в БД не пишутся.
     *         Если пользователь был оффлайн — он не увидит что пропустил.
     * Настраивается владельцем локации для каждой комнаты отдельно.
     */
    @Column(name = "store_history", nullable = false)
    private Boolean storeHistory = true;

    /**
     * Добавлена ли комната самим пользователем (true)
     * или создана автоматически из шаблона (false).
     * Нужно чтобы понимать какие комнаты можно удалить.
     * Базовые комнаты из шаблона удалить нельзя.
     */
    @Column(name = "is_custom", nullable = false)
    private Boolean isCustom = false;

    /**
     * Порядок отображения в списке комнат локации.
     * Меньше = выше в списке.
     */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}