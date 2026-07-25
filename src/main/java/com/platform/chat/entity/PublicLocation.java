package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.enums.PublicLocationType;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Общественная локация района (магазин, парк, бар и т.д.).
 * В отличие от Location (приватное жилье пользователя),
 * PublicLocation принадлежит району и доступна всем его жителям
 * без необходимости приглашения.
 */
@Data
@Entity
@Table(name = "public_locations")
public class PublicLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Район, к которому относится локация.
     * ManyToOne = в одном районе много общественных локаций.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    /**
     * Тип общественной локации (магазин, парк, бар и т.д.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PublicLocationType type;

    /**
     * Название локации.
     * Например: "Продуктовый 'Пятерочка'", "Центральный парк"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание локации
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Комнаты этой общественной локации (используем ту же
     * систему Room, что и у приватных Location, для единого чата).
     */
    @JsonIgnore
    @OneToMany(mappedBy = "publicLocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Room> rooms;
}
