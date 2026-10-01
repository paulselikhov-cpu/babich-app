package com.platform.chat.domain.location.dto;

import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.location.enums.LocationUserRole;
import com.platform.chat.domain.location.enums.LocationType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO для отдачи локации с ролью текущего пользователя.
 * Фронт должен знать роль чтобы показать правильное модальное окно:
 * OWNER → "удалить локацию"
 * MEMBER/MODERATOR → "покинуть локацию"
 */
@Data
public class LocationResponse {

    private Long id;
    /**
     * Код локации — то, чем локация адресуется в URL и в запросах
     * ({@code /chat/location/:code/...}). Уникален в пределах района.
     */
    private String code;
    private String name;
    private String description;
    private String inviteCode;
    private Boolean isPublic;
    private LocationType type;
    private LocalDateTime createdAt;
    private String templateName;

    // роль текущего пользователя в этой локации
    private String memberRole;

    public static LocationResponse from(Location location, LocationUserRole role) {
        LocationResponse dto = new LocationResponse();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setDescription(location.getDescription());
        dto.setInviteCode(location.getInviteCode());
        dto.setIsPublic(location.getIsPublic());
        dto.setCode(location.getCode());
        dto.setType(location.getType());
        dto.setCreatedAt(location.getCreatedAt());
        dto.setTemplateName(location.getTemplate() != null
                ? location.getTemplate().getName() : null);
        dto.setMemberRole(role != null ? role.name() : "MEMBER");
        return dto;
    }

    /**
     * Создать DTO из системной локации (без роли участника).
     * Используется для списка системных локаций района.
     */
    public static LocationResponse fromEntity(Location location) {
        LocationResponse dto = new LocationResponse();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setDescription(location.getDescription());
        dto.setCode(location.getCode());
        dto.setType(location.getType());
        dto.setCreatedAt(location.getCreatedAt());
        dto.setMemberRole("SYSTEM");
        return dto;
    }
}
