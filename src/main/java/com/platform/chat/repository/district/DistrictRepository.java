package com.platform.chat.repository.district;

import com.platform.chat.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    
    /**
     * Найти район по уникальному коду
     */
    Optional<District> findByCode(String code);
    
    /**
     * Получить все районы с их системными локациями (JOIN FETCH для избежания LazyInitializationException)
     */
    @Query("SELECT DISTINCT d FROM District d LEFT JOIN FETCH d.systemLocations")
    List<District> findAllWithSystemLocations();
    
    /**
     * Получить район по ID с его системными локациями
     */
    @Query("SELECT d FROM District d LEFT JOIN FETCH d.systemLocations WHERE d.id = :id")
    Optional<District> findByIdWithSystemLocations(Long id);
}
