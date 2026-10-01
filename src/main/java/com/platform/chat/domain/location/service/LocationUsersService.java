package com.platform.chat.domain.location.service;

import com.platform.chat.domain.location.dto.LocationSystemRoleView;
import com.platform.chat.domain.location.dto.LocationUserProjection;
import com.platform.chat.domain.location.dto.LocationUserResponse;
import com.platform.chat.domain.location.dto.LocationUsersPagedResponse;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.location.entity.LocationUser;
import com.platform.chat.domain.location.enums.LocationUserRole;
import com.platform.chat.domain.location.enums.LocationType;
import com.platform.chat.domain.chatUser.enums.SystemRoleType;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import com.platform.chat.domain.location.repository.LocationRepository;
import com.platform.chat.domain.location.repository.LocationUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocationUsersService {

    /** Головные должности локаций — их держатель получает role = MODERATOR в этой локации. */
    private static final Set<SystemRoleType> HEAD_ROLES = EnumSet.of(
            SystemRoleType.GOVERNOR,
            SystemRoleType.MAYOR,
            SystemRoleType.BANK_DIRECTOR,
            SystemRoleType.REAL_ESTATE_DIRECTOR
    );

    /** Какие должности допустимы для каждого типа системной локации (карта из плана 2.3). */
    private static final Map<LocationType, Set<SystemRoleType>> ALLOWED_ROLES = Map.of(
            LocationType.CITY_HALL,
            EnumSet.of(SystemRoleType.GOVERNOR, SystemRoleType.MAYOR, SystemRoleType.DEPUTY),
            LocationType.POLICE_STATION,
            EnumSet.of(SystemRoleType.POLICE_OFFICER),
            LocationType.BANK,
            EnumSet.of(SystemRoleType.BANK_DIRECTOR, SystemRoleType.BANK_EMPLOYEE),
            LocationType.REAL_ESTATE_MARKET,
            EnumSet.of(SystemRoleType.REAL_ESTATE_DIRECTOR)
    );

    private final LocationUserRepository locationUserRepository;
    private final LocationRepository locationRepository;
    private final ChatUserRepository chatUserRepository;

    public LocationUsersPagedResponse<LocationUserResponse> getLocationUsers(
            Long locationId, String query, String filter, int page, int size
    ) {
        Boolean onlineFilter = switch (filter == null ? "all" : filter) {
            case "online" -> true;
            case "offline" -> false;
            default -> null;
        };

        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        Pageable pageable = PageRequest.of(page, size);

        Page<LocationUserProjection> result = locationUserRepository.search(
                locationId, normalizedQuery, onlineFilter, pageable
        );

        List<LocationUserResponse> items = result.getContent().stream()
                .map(LocationUserResponse::fromProjection)
                .toList();

        return new LocationUsersPagedResponse<>(items, result.hasNext());
    }

    /**
     * Все занимаемые должности в системной локации (с именем занимающего).
     */
    @Transactional(readOnly = true)
    public List<LocationSystemRoleView> getSystemRolesByLocation(Long locationId) {
        return locationUserRepository.findByLocationIdAndSystemRoleNotNull(locationId).stream()
                .map(lu -> new LocationSystemRoleView(
                        lu.getId(),
                        locationId,
                        lu.getCharacter().getId(),
                        lu.getCharacter().getCharacterName(),
                        lu.getSystemRole(),
                        lu.getAppointedAt()))
                .toList();
    }

    /**
     * Все должности персонажа.
     */
    @Transactional(readOnly = true)
    public List<LocationSystemRoleView> getSystemRolesByCharacter(Long characterId) {
        return locationUserRepository.findByCharacterIdAndSystemRoleNotNull(characterId).stream()
                .map(lu -> new LocationSystemRoleView(
                        lu.getId(),
                        lu.getLocation().getId(),
                        characterId,
                        lu.getCharacter().getCharacterName(),
                        lu.getSystemRole(),
                        lu.getAppointedAt()))
                .toList();
    }

    /**
     * Назначить персонажа на должность в системной локации.
     *
     * Проверки (карта 2.2 плана):
     * - локация системная (для системных должностей) — бизнес-должностей для USER локаций пока нет;
     * - должность допустима для типа локации;
     * - назначаемый — житель того же района;
     * - идемпотентность: та же должность — тихий успех, другая — 400;
     * - назначающий имеет право (ADMIN платформы или глава локации с head-должностью).
     *
     * Держатель головной должности автоматически получает role = MODERATOR.
     */
    @Transactional
    public LocationUser appointSystemRole(
            Long locationId, Long characterId, SystemRoleType systemRole, Long actorCharacterId
    ) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Локация не найдена: " + locationId));

        if (!location.getIsSystem()) {
            throw new RuntimeException("Системные должности можно назначать только в системных локациях");
        }

        // должность допустима для типа локации
        Set<SystemRoleType> allowed = ALLOWED_ROLES.get(location.getType());
        if (allowed == null || !allowed.contains(systemRole)) {
            throw new RuntimeException("Должность " + systemRole + " недопустима для локации типа "
                    + location.getType());
        }

        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + characterId));

        // назначаемый — житель того же района (замена прежней проверки membership)
        if (!character.getDistrictId().equals(location.getDistrict().getId())) {
            throw new RuntimeException("Персонаж живёт в другом районе — нельзя назначить на должность здесь");
        }

        // права назначающего
        if (actorCharacterId == null || !isActorAllowed(location, actorCharacterId)) {
            throw new RuntimeException("Недостаточно прав для назначения на должность");
        }

        // идемпотентность: уже занимает эту же должность — тихий успех
        LocationUser mine = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, characterId)
                .orElse(null);
        if (mine != null && mine.getSystemRole() == systemRole) {
            return mine;
        }
        if (mine != null && mine.getSystemRole() != null) {
            throw new RuntimeException("Персонаж уже занимает должность " + mine.getSystemRole()
                    + " в этой локации — нельзя занять ещё и " + systemRole);
        }

        // должность занята другим персонажем (все системные должности одноместные)
        locationUserRepository.findByLocationIdAndSystemRole(locationId, systemRole)
                .ifPresent(existing -> {
                    throw new RuntimeException("Должность " + systemRole + " в локации "
                            + locationId + " уже занята");
                });

        LocationUser user = (mine != null)
                ? mine
                : new LocationUser();

        if (user.getLocation() == null) user.setLocation(location);
        if (user.getCharacter() == null) user.setCharacter(character);

        user.setSystemRole(systemRole);
        user.setAppointedAt(LocalDateTime.now());
        if (user.getIsRegistered() == null) user.setIsRegistered(true);
        // головная должность → модератор локации
        if (HEAD_ROLES.contains(systemRole)) {
            user.setRole(LocationUserRole.MODERATOR);
        } else if (user.getRole() == null) {
            user.setRole(LocationUserRole.MEMBER);
        }

        return locationUserRepository.save(user);
    }

    /**
     * Снять должность с персонажа. Назначающий должен иметь право (см. {@link #isActorAllowed}).
     */
    @Transactional
    public void removeSystemRole(Long locationId, Long characterId, Long actorCharacterId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Локация не найдена: " + locationId));

        if (!location.getIsSystem()) {
            throw new RuntimeException("Системные должности существуют только в системных локациях");
        }

        if (actorCharacterId == null || !isActorAllowed(location, actorCharacterId)) {
            throw new RuntimeException("Недостаточно прав для снятия должности");
        }

        LocationUser user = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, characterId)
                .orElseThrow(() -> new RuntimeException("Персонаж не состоит в этой локации"));

        if (user.getSystemRole() == null) {
            return; // должности нет — ничего делать не надо
        }
        user.setSystemRole(null);
        user.setAppointedAt(null);
        locationUserRepository.save(user);
    }

    /** Имеет ли персонаж право назначать/снимать должности в локации: ADMIN платформы или глава локации. */
    private boolean isActorAllowed(Location location, Long actorCharacterId) {
        ChatUser actor = chatUserRepository.findById(actorCharacterId).orElse(null);
        if (actor == null) return false;

        // глобальный администратор платформы — полное право
        if (actor.getRole() != null && "ADMIN".equals(actor.getRole().name())) return true;

        // глава локации: держатель head-должности в этой локации
        LocationUser actorUser = locationUserRepository
                .findByLocationIdAndCharacterId(location.getId(), actorCharacterId)
                .orElse(null);
        return actorUser != null
                && actorUser.getSystemRole() != null
                && HEAD_ROLES.contains(actorUser.getSystemRole());
    }
}