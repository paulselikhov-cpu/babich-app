package com.platform.chat.base.location.repository;

import com.platform.chat.base.location.dto.LocationUserProjection;
import com.platform.chat.base.location.entity.LocationUser;
import com.platform.chat.base.location.enums.LocationUserRole;
import com.platform.chat.base.chatUser.enums.SystemRoleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

/**
 * Репозиторий для работы с пользователями локаций.
 * Через эту таблицу определяем кто в какой локации состоит
 * и какую роль имеет.
 */
@Repository
public interface LocationUserRepository extends JpaRepository<LocationUser, Long> {

    // найти членство конкретного персонажа в конкретной локации
    // используется чтобы проверить — состоит ли персонаж в локации
    Optional<LocationUser> findByLocationIdAndCharacterId(
            Long locationId,
            Long characterId
    );

    // все участники конкретной локации
    @EntityGraph(attributePaths = {"character"})
    List<LocationUser> findByLocationId(Long locationId);

    // все локации конкретного персонажа
    @EntityGraph(attributePaths = {"location", "location.template"})
    List<LocationUser> findByCharacterId(Long characterId);

    // модераторы и владелец конкретной локации
    // используется для проверки прав на управление
    List<LocationUser> findByLocationIdAndRoleIn(
            Long locationId,
            List<LocationUserRole> roles
    );

    // персонаж на конкретной должности в конкретной локации (для единоличных должностей)
    Optional<LocationUser> findByLocationIdAndSystemRole(Long locationId, SystemRoleType systemRole);

    // штат системной локации (держатели должностей)
    List<LocationUser> findByLocationIdAndSystemRoleNotNull(Long locationId);

    // все должности конкретного персонажа
    List<LocationUser> findByCharacterIdAndSystemRoleNotNull(Long characterId);

    // все занимающие конкретную должность в локации (кроме указанного персонажа)
    List<LocationUser> findByLocationIdAndSystemRoleAndCharacterIdNot(
            Long locationId, SystemRoleType systemRole, Long characterId);

    /**
     * LEFT JOIN на OnlineSession (присутствие, слайс chat.core.modules.chatRoom) — если записи нет, presenceId будет null
     * (значит офлайн). Онлайн — всегда первыми в списке, дальше по алфавиту.
     */
    @Query("""
    SELECT new com.platform.chat.base.location.dto.LocationUserProjection(
        lm.character.id,
        lm.character.characterName,
        lm.character.lastSeen,
        cp.id,
        r.id,
        r.name,
        lm.systemRole
    )
    FROM LocationUser lm
    LEFT JOIN OnlineSession cp
        ON cp.character.id = lm.character.id AND cp.location.id = lm.location.id
    LEFT JOIN cp.room r
    WHERE lm.location.id = :locationId
      AND (:query IS NULL OR LOWER(lm.character.characterName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))
      AND (
            :onlineFilter IS NULL
            OR (:onlineFilter = true AND cp.id IS NOT NULL)
            OR (:onlineFilter = false AND cp.id IS NULL)
          )
    ORDER BY (CASE WHEN cp.id IS NOT NULL THEN 0 ELSE 1 END), lm.character.characterName
    """)
    Page<LocationUserProjection> search(
            @Param("locationId") Long locationId,
            @Param("query") String query,
            @Param("onlineFilter") Boolean onlineFilter,
            Pageable pageable
    );

    // проверить состоит ли персонаж в локации
    boolean existsByLocationIdAndCharacterId(Long locationId, Long characterId);

    // количество участников локации
    long countByLocationId(Long locationId);
}