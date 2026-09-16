package com.platform.chat.modules.notification.listener;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.core.event.childs.ApplicationResolvedEvent;
import com.platform.chat.core.event.childs.ElectionClosedEvent;
import com.platform.chat.core.event.childs.ElectionStartedEvent;
import com.platform.chat.modules.notification.dto.NotificationResponse;
import com.platform.chat.modules.notification.entity.Notification;
import com.platform.chat.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Слой 4 (Доставка). Реагирует на доменные события и доставляет уведомления:
 * сохраняет в БД + шлёт WS-пуш в личную очередь.
 *
 * Принцип: подписчик не знает, КТО опубликовал событие — только реагирует на факт.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final ChatUserRepository chatUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Старт выборов губернатора — уведомить всех жителей района.
     * Срабатывает по ElectionStartedEvent после коммита транзакции инициации
     * (аналогично {@link #onElectionClosed}).
     */
    @ApplicationModuleListener
    public void onElectionStarted(ElectionStartedEvent event) {
        Long districtId = event.getScopeId();
        List<ChatUser> residents = chatUserRepository.findByDistrictId(districtId);
        if (residents.isEmpty()) {
            log.info("No residents in district {}, skip election-start notifications", districtId);
            return;
        }

        String initiatorName = event.getInitiatorCharacterId() == null ? null
                : chatUserRepository.findById(event.getInitiatorCharacterId())
                        .map(ChatUser::getCharacterName)
                        .orElse(null);
        Long pollId = event.getPollId();

        String title = "Выборы губернатора начались";
        String body = (initiatorName != null
                ? "Выборы губернатора начались! Кандидатуру выдвинул " + initiatorName + ". "
                : "Выборы губернатора начались! ")
                + "Придите на Площадь Ленина, чтобы проголосовать или выдвинуть свою кандидатуру.";

        for (ChatUser resident : residents) {
            Notification notification = notificationService.notify(
                    resident.getId(), title, body, "ELECTION_STARTED", pollId);

            messagingTemplate.convertAndSendToUser(
                    resident.getUsername(),
                    "/queue/notifications",
                    NotificationResponse.from(notification)
            );
        }

        log.info("Election start delivered: districtId={}, recipients={}, pollId={}",
                districtId, residents.size(), pollId);
    }

    /**
     * Закрытие выборов губернатора — уведомить всех жителей района.
     * AFTER_COMMIT: срабатывает после успешного коммита транзакции, в которой
     * ElectionFacade закрыл голосование и назначил губернатора.
     *
     * WS-пуш идёт в /user/{username}/queue/notifications — principal у нас
     * username (JwtChannelInterceptor), а не characterId. Все остальные
     * user-queue в проекте (unread-updates) работают по username.
     *
     * Без собственной @Transactional: Spring запрещает её на
     * @ApplicationModuleListener (кроме REQUIRES_NEW/NOT_SUPPORTED).
     * Каждое сохранение делает notificationService.notify() в своей транзакции.
     *
     * @ApplicationModuleListener — обёртка Modulith над AFTER_COMMIT
     * (тот же TransactionalEventApplicationListener), но событие при этом
     * пишется в event_publication в транзакции публикатора и может быть
     * доставлено повторно после рестарта, если процесс упал между коммитом
     * и обработкой (outbox-гарантии Modulith).
     */
    @ApplicationModuleListener
    public void onElectionClosed(ElectionClosedEvent event) {
        Long districtId = event.getScopeId();
        List<ChatUser> residents = chatUserRepository.findByDistrictId(districtId);
        if (residents.isEmpty()) {
            log.info("No residents in district {}, skip election notifications", districtId);
            return;
        }

        String title = "Выборы губернатора завершены";
        String body = "Губернатором избран " + event.getWinnerName() + ". Результаты доступны на площади Ленина.";
        Long pollId = event.getPollId();

        for (ChatUser resident : residents) {
            Notification notification = notificationService.notify(
                    resident.getId(), title, body, "ELECTION_RESULT", pollId);

            messagingTemplate.convertAndSendToUser(
                    resident.getUsername(),
                    "/queue/notifications",
                    NotificationResponse.from(notification)
            );
        }

        log.info("Election result delivered: districtId={}, recipients={}, pollId={}",
                districtId, residents.size(), pollId);
    }

    /**
     * Результат рассмотрения гражданской заявки (APPROVED/REJECTED) —
     * уведомить заявителя.
     *
     * <p>Адресация — по внутреннему {@code characterId} (поле
     * {@code Application.characterId}), чья заявка подана: уведомление уходит
     * конкретному персонажу (а не всем персонажам пользователя). В контекстные
     * данные уведомления ({@code Notification.data}) кладём {@code applicationType}
     * и {@code applicationStatus} — фронт по ним решает, что делать (например,
     * перезагрузить профиль после одобрения паспорта/лицензии).</p>
     *
     * <p>AFTER_COMMIT: срабатывает после успешного коммита транзакции, в которой
     * ApplicationService разрешил заявку (см. javadoc {@link #onElectionClosed}).</p>
     */
    @ApplicationModuleListener
    public void onApplicationResolved(ApplicationResolvedEvent event) {
        ChatUser character = event.getCharacterId() == null
                ? null
                : chatUserRepository.findById(event.getCharacterId()).orElse(null);
        if (character == null) {
            log.info("Character {} not found for application {}, skip notification",
                    event.getCharacterId(), event.getApplicationId());
            return;
        }

        String appType = event.getApplicationType() != null ? event.getApplicationType() : "UNKNOWN";
        String statusText = event.getStatus() != null ? event.getStatus() : "UNKNOWN";
        // родительный падеж: «Ваша заявка на получение паспорта одобрена»
        String typePhrase = switch (appType) {
            case "BUY_FIRST_LOCATION" -> "на покупку первого жилья";
            case "REGISTER_PASSPORT" -> "на получение паспорта";
            case "REGISTER_WORK_LICENCE" -> "на получение рабочей лицензии";
            default -> "№" + event.getApplicationId();
        };
        String title = switch (statusText) {
            case "APPROVED" -> "Заявка " + typePhrase + " одобрена";
            case "REJECTED" -> "Заявка " + typePhrase + " отклонена";
            default -> "Заявка " + typePhrase + " обработана";
        };
        String body = switch (statusText) {
            case "APPROVED" -> "Ваша заявка " + typePhrase + " одобрена.";
            case "REJECTED" -> "Ваша заявка " + typePhrase + " отклонена.";
            default -> "Ваша заявка " + typePhrase + " обработана.";
        };
        Long applicationId = event.getApplicationId();

        Map<String, Object> data = Map.of(
                "applicationType", appType,
                "applicationStatus", statusText
        );

        Notification notification = notificationService.notify(
                character.getId(), title, body, "APPLICATION_RESULT", applicationId, data);

        messagingTemplate.convertAndSendToUser(
                character.getUsername(),
                "/queue/notifications",
                NotificationResponse.from(notification)
        );

        log.info("Application result delivered: applicationId={}, characterId={}, type={}, status={}",
                applicationId, character.getId(), appType, statusText);
    }
}
