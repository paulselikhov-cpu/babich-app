package com.platform.chat.repository;

import com.platform.chat.entity.LocationMember;
import com.platform.chat.enums.LocationMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

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
    List<LocationMember> findByLocationId(Long locationId);

    // все локации конкретного персонажа
    List<LocationMember> findByCharacterId(Long characterId);

    // модераторы и владелец конкретной локации
    // используется для проверки прав на управление
    List<LocationMember> findByLocationIdAndRoleIn(
            Long locationId,
            List<LocationMemberRole> roles
    );

    // проверить состоит ли персонаж в локации
    boolean existsByLocationIdAndCharacterId(Long locationId, Long characterId);

    // количество участников локации
    long countByLocationId(Long locationId);
}