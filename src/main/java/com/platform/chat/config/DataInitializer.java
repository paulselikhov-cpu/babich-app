package com.platform.chat.config;

import com.platform.chat.domain.district.entity.District;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.room.entity.Room;
import com.platform.chat.domain.location.enums.PublicLocationType;
import com.platform.chat.domain.district.repository.DistrictRepository;
import com.platform.chat.domain.location.repository.LocationRepository;
import com.platform.chat.domain.room.repository.RoomRepository;
import com.platform.chat.domain.district.service.DistrictSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Инициализатор дефолтных данных при первом запуске.
 * Создаёт район "Центральный" и 8 системных локаций с базовыми комнатами.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final DistrictRepository districtRepository;
    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final DistrictSettingsService districtSettingsService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultData() {
        if (districtRepository.count() > 0) {
            log.info("Данные уже инициализированы, пропускаем инициализацию");
            return;
        }

        log.info("Инициализация дефолтных данных...");

        // Создаём дефолтный район
        District central = new District();
        central.setCode("CENTRAL");
        central.setName("Центральный");
        central.setDescription("Центральный район города — сердце деловой и культурной жизни");
        central = districtRepository.save(central);

        // Настройки района (дефолты для админской панели)
        districtSettingsService.getOrCreate(central.getId());

        // Создаём 8 системных локаций
        createSystemLocation(central, PublicLocationType.CITY_HALL, "Мэрия",
                "Здание городской администрации", List.of("Приёмная", "Зал заседаний", "Кабинет губернатора"));
        createSystemLocation(central, PublicLocationType.LENIN_SQUARE, "Площадь Ленина",
                "Центральная площадь города, место народных собраний", List.of("Площадь", "Тёплые трубы"));
        createSystemLocation(central, PublicLocationType.POLICE_STATION, "Полицейский участок",
                "Опорный пункт правопорядка района", List.of("Дежурная часть", "Кабинет следователя", "Обходной лист"));
        createSystemLocation(central, PublicLocationType.PRISON, "Тюрьма",
                "Место изоляции нарушителей порядка", List.of("Приёмный изолятор", "Камера", "Комната свиданий"));
        createSystemLocation(central, PublicLocationType.BANK, "Банк",
                "Финансовое учреждение района", List.of("Операционный зал", "Хранилище", "Кабинет директора"));
        createSystemLocation(central, PublicLocationType.WAREHOUSE, "Склад",
                "Товарный склад для бизнеса", List.of("Приёмка", "Стеллажи", "Офис"));
        createSystemLocation(central, PublicLocationType.GENERAL_MARKET, "Рынок",
                "Общий рынок — площадка для свободной торговли", List.of("Торговый зал", "Подсобка"));
        createSystemLocation(central, PublicLocationType.REAL_ESTATE_MARKET, "Рынок недвижимости",
                "Площадка для купли-продажи недвижимости", List.of("Зал ожидания", "Кабинет риелтора"));

        log.info("Дефолтные данные успешно инициализированы");
    }

    private void createSystemLocation(District district, PublicLocationType type,
                                       String name, String description, List<String> roomNames) {
        Location location = new Location();
        location.setDistrict(district);
        location.setName(name);
        location.setDescription(description);
        location.setIsSystem(true);
        location.setType(type);
        location = locationRepository.save(location);

        // Создаём базовые комнаты для системной локации
        for (int i = 0; i < roomNames.size(); i++) {
            Room room = new Room();
            room.setLocation(location);
            room.setName(roomNames.get(i));
            room.setOrderIndex(i);
            room.setIsCustom(false);
            room.setStoreHistory(true);
            roomRepository.save(room);
        }
    }
}