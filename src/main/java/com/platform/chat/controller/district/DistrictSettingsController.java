package com.platform.chat.controller.district;

import com.platform.chat.dto.settings.DistrictSettingsDto;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.service.district.DistrictSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Админская панель настроек района (выборы губернатора, обжалование).
 * Доступ только для персонажей с ролью ADMIN или MODERATOR.
 */
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictSettingsController {

    private final DistrictSettingsService districtSettingsService;
    private final ChatUserRepository chatUserRepository;

    @GetMapping("/{districtId}/settings")
    public ResponseEntity<DistrictSettingsDto> getSettings(
            @PathVariable Long districtId,
            Principal principal) {
        if (!isAdminOrModerator(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(districtSettingsService.getSettings(districtId));
    }

    @PutMapping("/{districtId}/settings")
    public ResponseEntity<DistrictSettingsDto> updateSettings(
            @PathVariable Long districtId,
            @RequestBody DistrictSettingsDto dto,
            Principal principal) {
        if (!isAdminOrModerator(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(districtSettingsService.updateSettings(districtId, dto));
    }

    private boolean isAdminOrModerator(Principal principal) {
        if (principal == null) {
            return false;
        }
        ChatUser character = chatUserRepository.findByUsername(principal.getName()).orElse(null);
        if (character == null) {
            return false;
        }
        return character.getRole() == ChatRole.ADMIN
                || character.getRole() == ChatRole.MODERATOR;
    }
}

