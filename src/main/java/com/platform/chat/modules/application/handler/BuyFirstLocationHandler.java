package com.platform.chat.modules.application.handler;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.Location;
import com.platform.chat.entity.LocationTemplate;
import com.platform.chat.modules.application.entity.Application;
import com.platform.chat.modules.application.enums.ApplicationType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationRepository;
import com.platform.chat.repository.location.LocationTemplateRepository;
import com.platform.chat.service.location.LocationService;

import com.platform.chat.modules.application.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает заявку "купить первую квартиру".
 * Условия одобрения:
 * - у персонажа статус HOMELESS (ещё нет своей локации);
 * - у персонажа хватает монет на выбранный шаблон.
 * При одобрении: списываются монеты (через LocationService.createLocation),
 * создаётся Location, персонаж переводится в статус RESIDENT.
 */
@Component
@RequiredArgsConstructor
public class BuyFirstLocationHandler implements ApplicationHandler {

    /** Код шаблона локации по умолчанию для первого жилья. */
    private static final String DEFAULT_TEMPLATE_CODE = "KHRUSHCHEVKA";

    private final ChatUserRepository chatUserRepository;
    private final LocationTemplateRepository locationTemplateRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;

    @Override
    public ApplicationType getType() {
        return ApplicationType.BUY_FIRST_LOCATION;
    }

    @Override
    public void handle(Application application) {
        // Адресуем персонажа по characterId (внутренний ChatUser.id), за которого
        // подана заявка — см. Application.characterId.
        ChatUser user = chatUserRepository.findById(application.getCharacterId())
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж не найден: " + application.getCharacterId()));

        if (!"HOMELESS".equals(user.getGameStatus())) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage("У вас уже есть жильё");
            return;
        }

        if (!locationRepository.findByOwnerId(user.getId()).isEmpty()) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage("У вас уже есть локация");
            return;
        }

        String templateCode = application.getPayload() != null && !application.getPayload().isBlank()
                ? application.getPayload()
                : DEFAULT_TEMPLATE_CODE;

        LocationTemplate template = locationTemplateRepository.findByCode(templateCode)
                .orElseThrow(() -> new RuntimeException("Шаблон локации не найден: " + templateCode));

        if (user.getCoinBalance() < template.getPriceCoin()) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage(
                    "Недостаточно монет. Нужно: " + template.getPriceCoin() +
                            ", есть: " + user.getCoinBalance());
            return;
        }

        Location location = locationService.createLocation(
                template.getId(),
                user.getId(),
                user.getCharacterName() + " — " + template.getName(),
                "Первое жильё персонажа",
                false
        );

        user.setGameStatus("RESIDENT");
        chatUserRepository.save(user);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setResultMessage("Локация создана: " + location.getName());
    }
}
