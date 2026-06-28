package com.platform.chat.service;

import com.platform.chat.entity.LocationTemplate;
import com.platform.chat.entity.TemplateRoom;
import com.platform.chat.repository.LocationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Сервис для работы с шаблонами локаций.
 * Шаблоны создаются администратором платформы — не пользователями.
 * Пользователи только выбирают шаблон при создании своей локации.
 */
@Service
@RequiredArgsConstructor
public class LocationTemplateService {

    private final LocationTemplateRepository locationTemplateRepository;

    /**
     * Получить все доступные шаблоны.
     * Показываем пользователю когда он нажимает "создать локацию".
     */
    public List<LocationTemplate> getAllTemplates() {
        return locationTemplateRepository.findAll();
    }

    /**
     * Получить только бесплатные шаблоны.
     * Для пользователей без Coin.
     */
    public List<LocationTemplate> getFreeTemplates() {
        // priceCoin = 0 → бесплатный шаблон
        return locationTemplateRepository.findByPriceCoin(0L);
    }

    /**
     * Получить только платные шаблоны.
     * Показываем с ценой в Coin.
     */
    public List<LocationTemplate> getPaidTemplates() {
        return locationTemplateRepository.findByPriceCoinGreaterThan(0L);
    }

    /**
     * Получить шаблон по id.
     * Используется при создании локации — передаём id выбранного шаблона.
     */
    public LocationTemplate getById(Long id) {
        return locationTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Шаблон локации не найден: " + id
                ));
    }

    /**
     * Создать новый шаблон локации.
     * Только для администратора платформы.
     * При создании шаблона сразу создаём базовые комнаты.
     *
     * @Transactional = если что-то пойдёт не так — откатить всё.
     * Например если шаблон создался но комнаты нет — это плохо.
     * Транзакция гарантирует что либо всё создалось либо ничего.
     */
    @Transactional
    public LocationTemplate createTemplate(LocationTemplate template,
                                           List<TemplateRoom> rooms) {
        // проверяем уникальность кода шаблона
        if (locationTemplateRepository.existsByCode(template.getCode())) {
            throw new RuntimeException(
                    "Шаблон с кодом " + template.getCode() + " уже существует"
            );
        }

        // сначала сохраняем шаблон чтобы получить id
        LocationTemplate saved = locationTemplateRepository.save(template);

        // привязываем комнаты к шаблону и сохраняем
        rooms.forEach(room -> {
            room.setTemplate(saved); // устанавливаем связь
        });
        saved.setTemplateRooms(rooms);

        return locationTemplateRepository.save(saved);
    }
}