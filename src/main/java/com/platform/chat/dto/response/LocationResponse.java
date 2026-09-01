package com.platform.chat.dto.response;

import com.platform.chat.entity.Location;
import com.platform.chat.enums.LocationMemberRole;
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
    private String name;
    private String description;
    private String inviteCode;
    private Boolean isPublic;
    private Boolean isSystem;
    private LocalDateTime createdAt;
    private String templateName;

    // роль текущего пользователя в этой локации
    private String memberRole;

    public static LocationResponse from(Location location, LocationMemberRole role) {
        LocationResponse dto = new LocationResponse();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setDescription(location.getDescription());
        dto.setInviteCode(location.getInviteCode());
        dto.setIsPublic(location.getIsPublic());
        dto.setIsSystem(location.getIsSystem());
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
        dto.setIsSystem(location.getIsSystem());
        dto.setCreatedAt(location.getCreatedAt());
        dto.setMemberRole("SYSTEM");
        return dto;
    }
}
