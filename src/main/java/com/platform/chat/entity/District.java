package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Район города — географическая зона с общественными локациями.
 * Каждый персонаж живет в определенном районе.
 * В районе есть дефолтные общественные места (магазины, парки, бары).
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
     * Общественные локации района (магазины, парки, бары и т.д.)
     * CascadeType.ALL = при удалении района удаляются его локации
     */
    @JsonIgnore
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PublicLocation> publicLocations;

    /**
     * Приватные локации пользователей в этом районе
     */
    @JsonIgnore
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Location> locations;
}
