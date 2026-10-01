package com.platform.chat.domain.district;

import com.platform.chat.domain.district.entity.District;
import com.platform.chat.domain.district.repository.DistrictRepository;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.location.enums.LocationType;
import com.platform.chat.domain.location.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регресс на баг: {@code District.systemLocations} — это {@code @OneToMany(mappedBy = "district")},
 * а {@code mappedBy} лишь связывает коллекцию с FK {@code locations.district_id} и НЕ фильтрует.
 * В итоге {@code GET /api/districts} в «системных локациях» отдавал ВСЕ локации района,
 * включая пользовательские и приватные чужие. Фильтр задан {@code @SQLRestriction("is_system = true")}.
 *
 * <p>Тест не транзакционный (как остальные интеграционные тесты проекта): данные коммитятся
 * в dev-БД, поэтому код района/имена уникальны на каждый прогон.</p>
 *
 * <p><b>Запускать только против изолированной БД</b> — иначе старт контекста с
 * {@code ddl-auto: create} из {@code application.yaml} пересоздаст схему dev-БД chatdb
 * и сотрёт данные пользователя:</p>
 * <pre>
 * docker exec babich-postgres psql -U chatuser -d postgres -c "CREATE DATABASE chatdb_test"
 * cd babich-app && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/chatdb_test \
 *   mvn -o test -Dtest=DistrictSystemLocationsIntegrationTest
 * </pre>
 */
@SpringBootTest
class DistrictSystemLocationsIntegrationTest {

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    @DisplayName("systemLocations района содержит только локации с is_system = true")
    void systemLocationsContainsOnlySystemLocations() {
        String code = "TEST-DIST-" + System.nanoTime();
        District district = new District();
        district.setCode(code);
        district.setName("Тестовый район " + code);
        district = districtRepository.save(district);

        Location systemLocation = new Location();
        systemLocation.setDistrict(district);
        systemLocation.setName("Мэрия (тест)");
        systemLocation.setIsSystem(true);
        systemLocation.setType(LocationType.CITY_HALL);
        systemLocation = locationRepository.save(systemLocation);

        Location userLocation = new Location();
        userLocation.setDistrict(district);
        userLocation.setName("Квартира игрока (тест)");
        userLocation.setIsSystem(false);
        userLocation.setType(LocationType.HOME);
        userLocation.setIsPublic(false);
        userLocation = locationRepository.save(userLocation);

        District loaded = districtRepository.findByIdWithSystemLocations(district.getId()).orElseThrow();

        List<Location> systemLocations = loaded.getSystemLocations();
        assertThat(systemLocations)
                .as("в systemLocations не должно быть пользовательской локации")
                .extracting(Location::getId)
                .containsExactly(systemLocation.getId());
        assertThat(systemLocations)
                .allMatch(l -> Boolean.TRUE.equals(l.getIsSystem()));
        assertThat(systemLocations)
                .extracting(Location::getId)
                .doesNotContain(userLocation.getId());
    }
}