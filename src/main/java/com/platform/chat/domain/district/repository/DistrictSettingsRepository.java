package com.platform.chat.base.district.repository;

import com.platform.chat.base.district.entity.DistrictSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistrictSettingsRepository extends JpaRepository<DistrictSettings, Long> {

    Optional<DistrictSettings> findByDistrictId(Long districtId);
}
