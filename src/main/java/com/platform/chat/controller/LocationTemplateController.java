package com.platform.chat.controller;

import com.platform.chat.entity.LocationTemplate;
import com.platform.chat.service.LocationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST контроллер для шаблонов локаций.
 * Шаблоны — это "меню" из которого пользователь выбирает
 * при создании своей локации.
 *
 * Все эндпоинты защищены JWT — нужен токен в заголовке:
 * Authorization: Bearer <token>
 */
@RestController
@RequestMapping("/api/location-templates")
@RequiredArgsConstructor
public class LocationTemplateController {

    private final LocationTemplateService locationTemplateService;

    /**
     * GET /api/location-templates
     * Все доступные шаблоны.
     * Вызывается когда пользователь открывает список шаблонов.
     */
    @GetMapping
    public List<LocationTemplate> getAllTemplates() {
        return locationTemplateService.getAllTemplates();
    }

    /**
     * GET /api/location-templates/free
     * Только бесплатные шаблоны.
     */
    @GetMapping("/free")
    public List<LocationTemplate> getFreeTemplates() {
        return locationTemplateService.getFreeTemplates();
    }

    /**
     * GET /api/location-templates/paid
     * Только платные шаблоны — показываем с ценой в Coin.
     */
    @GetMapping("/paid")
    public List<LocationTemplate> getPaidTemplates() {
        return locationTemplateService.getPaidTemplates();
    }

    /**
     * GET /api/location-templates/{id}
     * Конкретный шаблон по id.
     * Вызывается при просмотре деталей шаблона перед созданием локации.
     */
    @GetMapping("/{id}")
    public LocationTemplate getById(@PathVariable Long id) {
        return locationTemplateService.getById(id);
    }
}