package com.platform.chat.dto.response;

import com.platform.chat.entity.PublicLocation;
import com.platform.chat.enums.PublicLocationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для передачи информации об общественной локации на frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicLocationResponse {
    private Long id;
    private PublicLocationType type;
    private String name;
    private String description;

    public static PublicLocationResponse fromEntity(PublicLocation location) {
        PublicLocationResponse response = new PublicLocationResponse();
        response.setId(location.getId());
        response.setType(location.getType());
        response.setName(location.getName());
        response.setDescription(location.getDescription());
        return response;
    }
}
