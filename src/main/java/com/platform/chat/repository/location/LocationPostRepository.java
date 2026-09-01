package com.platform.chat.repository.location;

import com.platform.chat.entity.LocationPost;
import com.platform.chat.enums.LocationPostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с должностями в системных локациях.
 */
@Repository
public interface LocationPostRepository extends JpaRepository<LocationPost, Long> {

    /**
     * Все должности в конкретной системной локации.
     */
    List<LocationPost> findByLocationId(Long locationId);

    /**
     * Все должности конкретного персонажа.
     */
    List<LocationPost> findByCharacterId(Long characterId);

    /**
     * Конкретная должность в конкретной локации (для единоличных должностей).
     */
    Optional<LocationPost> findByLocationIdAndPost(Long locationId, LocationPostType post);

    /**
     * Проверка: есть ли уже персонаж на этой должности в этой локации.
     */
    Optional<LocationPost> findByLocationIdAndCharacterId(Long locationId, Long characterId);

    /**
     * Все персонажи на конкретной должности в конкретной локации.
     */
    List<LocationPost> findByLocationIdAndPostAndCharacterIdNot(
            Long locationId, LocationPostType post, Long characterId);
}