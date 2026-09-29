package com.platform.chat.modules.gang.entity;

import jakarta.persistence.*;

/**
 * Минимальная заготовка группировки (Gang).
 * Появляется вместе с полем {@code Location.gangId}: связь пока держим
 * как необязательный {@code Long} без FK, чтобы не усложнять схему.
 * При появлении полноценной модели Gang — перевести на @ManyToOne.
 */
@Entity //TODO: УБРАТЬ ГЕТТЕРЫ СЕТТЕРЫ
@Table(name = "gangs")
public class Gang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название группировки. */
    @Column(nullable = false, length = 100)
    private String name;

    /** Район, к которому относится группировка. */
    @Column(name = "district_id", nullable = false)
    private Long districtId;

    public Gang() {
    }

    public Gang(String name, Long districtId) {
        this.name = name;
        this.districtId = districtId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }
}