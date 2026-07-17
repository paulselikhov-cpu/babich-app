package com.platform.chat.repository.location;

import com.platform.chat.entity.LocationMember;
import com.platform.chat.enums.LocationMemberRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.platform.chat.dto.LocationUserProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

/**
 * Репозиторий для работы с членством в локациях.
 * Через эту таблицу определяем кто в какой локации состоит
 * и какую роль имеет.
 */
@Repository
public interface LocationMemberRepository extends JpaRepository<LocationMember, Long> {

    // найти членство конкретного персонажа в конкретной локации
    // используется чтобы проверить — состоит ли персонаж в локации
    Optional<LocationMember> findByLocationIdAndCharacterId(
            Long locationId,
            Long characterId
    );

    // все участники конкретной локации
    @EntityGraph(attributePaths = {"character"})
    List<LocationMember> findByLocationId(Long locationId);

    // все локации конкретного персонажа
    @EntityGraph(attributePaths = {"location", "location.template"})
    List<LocationMember> findByCharacterId(Long characterId);

    // модераторы и владелец конкретной локации
    // используется для проверки прав на управление
    List<LocationMember> findByLocationIdAndRoleIn(
            Long locationId,
            List<LocationMemberRole> roles
    );

    /**
     * LEFT JOIN на CharacterPresence — если записи нет, presenceId будет null
     * (значит офлайн). Онлайн — всегда первыми в списке, дальше по алфавиту.
     */
    @Query("""
    SELECT new com.platform.chat.dto.LocationUserProjection(
        lm.character.id,
        lm.character.characterName,
        lm.character.lastSeen,
        cp.id,
        r.id,
        r.name
    )
    FROM LocationMember lm
    LEFT JOIN CharacterPresence cp
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