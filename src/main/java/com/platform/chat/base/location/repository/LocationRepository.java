package com.platform.chat.base.location.repository;

import com.platform.chat.base.location.entity.Location;
import com.platform.chat.base.location.enums.PublicLocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с локациями.
 * Локация может быть как пользовательской (созданной из шаблона), так и системной.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    // все публичные пользовательские локации — показываем в общем списке
    List<Location> findByIsPublicTrue();

    // все системные локации
    List<Location> findByIsSystemTrue();

    // все системные локации района
    List<Location> findByDistrictIdAndIsSystemTrue(Long districtId);

    // найти системную локацию района по типу (например, мэрию)
    Optional<Location> findByDistrictIdAndIsSystemTrueAndType(Long districtId, PublicLocationType type);

    // все локации конкретного владельца
    // используется в профиле персонажа — "мои локации"
    List<Location> findByOwnerId(Long ownerId);

    // найти локацию по invite_code — для вступления по ссылке
    // например /chat/join/abc123xy
    Optional<Location> findByInviteCode(String inviteCode);

    // проверить существует ли такой invite_code
    // при генерации кода проверяем уникальность
    boolean existsByInviteCode(String inviteCode);

    // все локации созданные из конкретного шаблона
    // полезно для статистики — сколько "хрущевок" создано
    List<Location> findByTemplateId(Long templateId);

    /**
     * Все локации в которых состоит персонаж (через location_users).
     * @Query = пишем HQL вручную когда Spring не может сгенерировать по имени метода.
     * HQL похож на SQL но использует названия Java классов и полей,
     * а не названия таблиц и колонок.
     *
     * JOIN FETCH = загрузить локацию вместе с шаблоном за один запрос
     * чтобы избежать проблемы N+1 (когда для каждой локации делается
     * отдельный запрос за шаблоном).
     */
    @Query("""
        SELECT l FROM Location l
        JOIN FETCH l.template
        JOIN LocationUser lm ON lm.location = l
        WHERE lm.character.id = :characterId
    """)
    List<Location> findAllByMemberCharacterId(@Param("characterId") Long characterId);
}