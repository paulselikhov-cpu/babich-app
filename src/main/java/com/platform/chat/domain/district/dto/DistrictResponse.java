package com.platform.chat.domain.district.dto;

import com.platform.chat.domain.district.entity.District;
import com.platform.chat.domain.location.dto.LocationResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO для передачи информации о районе на frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistrictResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<LocationResponse> systemLocations;

    public static DistrictResponse fromEntity(District district) {
        DistrictResponse response = new DistrictResponse();
        response.setId(district.getId());
        response.setCode(district.getCode());
        response.setName(district.getName());
        response.setDescription(district.getDescription());
        
        if (district.getSystemLocations() != null) {
            response.setSystemLocations(
                district.getSystemLocations().stream()
                    .map(LocationResponse::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        
        return response;
    }
}
