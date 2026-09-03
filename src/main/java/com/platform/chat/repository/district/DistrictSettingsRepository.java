package com.platform.chat.repository.district;

import com.platform.chat.entity.DistrictSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistrictSettingsRepository extends JpaRepository<DistrictSettings, Long> {

    Optional<DistrictSettings> findByDistrictId(Long districtId);
}
