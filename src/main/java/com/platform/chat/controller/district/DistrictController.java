package com.platform.chat.controller.district;

import com.platform.chat.dto.response.DistrictResponse;
import com.platform.chat.dto.response.LocationResponse;
import com.platform.chat.entity.District;
import com.platform.chat.entity.Location;
import com.platform.chat.service.district.DistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API для получения информации о районах и их системных локациях.
 * Согласно концепции, район создаётся автоматически
 * администратором платформы, вмещает жителей и содержит фиксированный
 * набор системных локаций (мэрия, площадь Ленина, полицейский участок и т.д.).
 */
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictService districtService;

    /**
     * Получить список всех районов
     */
    @GetMapping
    public List<DistrictResponse> getAllDistricts() {
        List<District> districts = districtService.getAllDistricts();
        return districts.stream()
                .map(DistrictResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить район по ID (со списком его системных локаций)
     */
    @GetMapping("/{id}")
    public DistrictResponse getDistrictById(@PathVariable Long id) {
        District district = districtService.getDistrictById(id);
        return DistrictResponse.fromEntity(district);
    }

    /**
     * Получить все системные локации конкретного района
     */
    @GetMapping("/{id}/system-locations")
    public List<LocationResponse> getSystemLocations(@PathVariable Long id) {
        List<Location> locations = districtService.getSystemLocationsByDistrict(id);
        return locations.stream()
                .map(LocationResponse::fromEntity)
                .collect(Collectors.toList());
    }
}