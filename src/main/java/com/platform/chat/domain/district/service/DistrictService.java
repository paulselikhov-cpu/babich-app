package com.platform.chat.base.district.service;

import com.platform.chat.base.district.entity.District;
import com.platform.chat.base.location.entity.Location;
import com.platform.chat.base.location.enums.PublicLocationType;
import com.platform.chat.base.district.repository.DistrictRepository;
import com.platform.chat.base.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для работы с районами и их системными локациями
 */
@Service
@RequiredArgsConstructor
public class DistrictService {

    private final DistrictRepository districtRepository;
    private final LocationRepository locationRepository;

    /**
     * Получить все районы вместе с их системными локациями
     */
    @Transactional(readOnly = true)
    public List<District> getAllDistricts() {
        return districtRepository.findAllWithSystemLocations();
    }

    /**
     * Получить район по ID вместе с его системными локациями
     */
    @Transactional(readOnly = true)
    public District getDistrictById(Long id) {
        return districtRepository.findByIdWithSystemLocations(id)
                .orElseThrow(() -> new RuntimeException("Район с id=" + id + " не найден"));
    }

    /**
     * Получить район по коду
     */
    @Transactional(readOnly = true)
    public District getDistrictByCode(String code) {
        return districtRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Район с кодом '" + code + "' не найден"));
    }

    /**
     * Получить все системные локации района
     */
    @Transactional(readOnly = true)
    public List<Location> getSystemLocationsByDistrict(Long districtId) {
        return locationRepository.findByDistrictIdAndIsSystemTrue(districtId);
    }

    /**
     * Получить конкретную системную локацию района по типу
     * (например, мэрию или полицейский участок конкретного района)
     */
    @Transactional(readOnly = true)
    public Location getSystemLocationByType(Long districtId, PublicLocationType type) {
        return locationRepository.findByDistrictIdAndIsSystemTrueAndType(districtId, type)
                .orElseThrow(() -> new RuntimeException(
                        "Системная локация типа " + type + " в районе " + districtId + " не найдена"
                ));
    }
}