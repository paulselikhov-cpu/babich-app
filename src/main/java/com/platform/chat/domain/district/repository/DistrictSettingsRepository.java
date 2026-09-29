package com.platform.chat.domain.district.repository;

import com.platform.chat.domain.district.entity.DistrictSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistrictSettingsRepository extends JpaRepository<DistrictSettings, Long> {

    Optional<DistrictSettings> findByDistrictId(Long districtId);
}
