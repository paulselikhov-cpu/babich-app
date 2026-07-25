package com.platform.chat.repository.district;

import com.platform.chat.entity.PublicLocation;
import com.platform.chat.enums.PublicLocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicLocationRepository extends JpaRepository<PublicLocation, Long> {

    /**
     * Все общественные локации района
     */
    List<PublicLocation> findByDistrictId(Long districtId);

    /**
     * Найти конкретную системную локацию района по типу
     * (например, мэрию или полицейский участок конкретного района)
     */
    Optional<PublicLocation> findByDistrictIdAndType(Long districtId, PublicLocationType type);
}
