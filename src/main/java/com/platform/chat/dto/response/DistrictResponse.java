package com.platform.chat.dto.response;

import com.platform.chat.entity.District;
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
    private List<PublicLocationResponse> publicLocations;

    public static DistrictResponse fromEntity(District district) {
        DistrictResponse response = new DistrictResponse();
        response.setId(district.getId());
        response.setCode(district.getCode());
        response.setName(district.getName());
        response.setDescription(district.getDescription());
        
        if (district.getPublicLocations() != null) {
            response.setPublicLocations(
                district.getPublicLocations().stream()
                    .map(PublicLocationResponse::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        
        return response;
    }
}
