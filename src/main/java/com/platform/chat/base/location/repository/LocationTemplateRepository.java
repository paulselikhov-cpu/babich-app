package com.platform.chat.base.location.repository;

import com.platform.chat.base.location.entity.LocationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с шаблонами локаций.
 * Spring автоматически генерирует SQL по названию метода.
 * Например findByPriceCoin(0) → SELECT * FROM location_templates WHERE price_coin = 0
 */
@Repository
public interface LocationTemplateRepository extends JpaRepository<LocationTemplate, Long> {

    // найти шаблон по уникальному коду
    // например findByCode("KHRUSHCHEVKA")
    Optional<LocationTemplate> findByCode(String code);

    // все бесплатные шаблоны (price_coin = 0)
    // показываем в списке при создании локации без фильтра
    List<LocationTemplate> findByPriceCoin(Long priceCoin);

    // все платные шаблоны (price_coin > 0)
    List<LocationTemplate> findByPriceCoinGreaterThan(Long priceCoin);

    // расширяемые шаблоны — те в которых можно достраивать комнаты
    List<LocationTemplate> findByIsExtendable(Boolean isExtendable);

    // проверить существует ли шаблон с таким кодом
    // используется при создании новых шаблонов чтобы не дублировать
    boolean existsByCode(String code);
}