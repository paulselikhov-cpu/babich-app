package com.platform.chat.domain.district.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.domain.location.entity.Location;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
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
     * Системные локации района (мэрия, банк, полиция и т.д.).
     *
     * <p>{@code mappedBy} сам по себе только связывает коллекцию с FK
     * {@code locations.district_id} и вернул бы ВСЕ локации района, включая
     * пользовательские. Реальный фильтр даёт {@link SQLRestriction}: коллекция
     * содержит только {@code is_system = true}. Ограничение применяется и к
     * {@code LEFT JOIN FETCH d.systemLocations} в {@code DistrictRepository}.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SQLRestriction("is_system = true")
    private List<Location> systemLocations;
}