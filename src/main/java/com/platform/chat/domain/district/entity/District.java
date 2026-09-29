package com.platform.chat.base.district.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.base.location.entity.Location;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Район города — географическая зона с системными и пользовательскими локациями.
 * Каждый персонаж живет в определенном районе.
 * В районе есть системные места (мэрия, банк, полиция и т.д.) и
 * пользовательские локации (жильё, созданное игроками).
 */
@Data
@Entity
@Table(name = "districts")
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название района.
     * Например: "Центральный", "Спальный", "Промышленный"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Описание района — атмосфера, особенности
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Уникальный код района для URL и идентификации
     */
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    /**
     * Системные локации района (мэрия, банк, полиция и т.д.)
     * Создаются автоматически при инициализации района.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Location> systemLocations;

    /**
     * Пользовательские локации в этом районе (жильё игроков)
     */
    @JsonIgnore
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Location> locations;
}