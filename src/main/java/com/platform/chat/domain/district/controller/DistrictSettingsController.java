package com.platform.chat.domain.district.controller;

import com.platform.chat.domain.district.service.DistrictSettingsService;
import com.platform.chat.domain.district.dto.DistrictSettingsDto;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.enums.ChatRole;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
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
        if (isNotAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(districtSettingsService.getSettings(districtId));
    }

    @PutMapping("/{districtId}/settings")
    public ResponseEntity<DistrictSettingsDto> updateSettings(
            @PathVariable Long districtId,
            @RequestBody DistrictSettingsDto dto,
            Principal principal) {
        if (isNotAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(districtSettingsService.updateSettings(districtId, dto));
    }

    private boolean isNotAdmin(Principal principal) {
        if (principal == null) {
            return true;
        }
        ChatUser character = chatUserRepository.findByUsername(principal.getName()).orElse(null);
        if (character == null) {
            return true;
        }
        return character.getRole() != ChatRole.ADMIN
                && character.getRole() != ChatRole.MODERATOR;
    }
}

