package com.platform.chat.domain.district.service;

import com.platform.chat.domain.district.entity.District;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.location.enums.LocationType;
import com.platform.chat.domain.district.repository.DistrictRepository;
import com.platform.chat.domain.location.repository.LocationRepository;
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
        return locationRepository.findByDistrictIdAndType(districtId, LocationType.SYSTEM);
    }

    /**
     * Получить конкретную системную локацию района по её code
     * (например, мэрию или полицейский участок конкретного района).
     *
     * <p>Адресация по {@code code}, а не по {@link LocationType}: тип различает
     * лишь SYSTEM/DEFAULT/BUSINESS и не указывает, какая это локация.
     * Ожидается одна из констант {@link com.platform.chat.domain.location.enums.SystemLocationCode}.</p>
     */
    @Transactional(readOnly = true)
    public Location getSystemLocationByCode(Long districtId, String code) {
        return locationRepository.findByDistrictIdAndCode(districtId, code)
                .filter(l -> l.getType() == LocationType.SYSTEM)
                .orElseThrow(() -> new RuntimeException(
                        "Системная локация " + code + " в районе " + districtId + " не найдена"
                ));
    }
}