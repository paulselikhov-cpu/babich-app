package com.platform.chat.modules.notification.listener;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.core.event.ApplicationResolvedEvent;
import com.platform.chat.core.event.ElectionClosedEvent;
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
     * <p>{@code userId} в событии — ВНЕШНИЙ User.id (поле
     * {@code Application.userId}), а уведомления адресуются внутреннему
     * ChatUser.id. Пользователь может иметь несколько персонажей
     * (по одному на район), поэтому находим всех через
     * {@link ChatUserRepository#findAllByUserId(Long)} и уведомляем каждого.</p>
     *
     * <p>AFTER_COMMIT: срабатывает после успешного коммита транзакции, в которой
     * ApplicationService разрешил заявку (см. javadoc {@link #onElectionClosed}).</p>
     */
    @ApplicationModuleListener
    public void onApplicationResolved(ApplicationResolvedEvent event) {
        List<ChatUser> characters = chatUserRepository.findAllByUserId(event.getUserId());
        if (characters.isEmpty()) {
            log.info("No characters for userId {}, skip application notification", event.getUserId());
            return;
        }

        String statusText = event.getStatus() != null ? event.getStatus() : "UNKNOWN";
        String title = switch (statusText) {
            case "APPROVED" -> "Заявка одобрена";
            case "REJECTED" -> "Заявка отклонена";
            default -> "Заявка обработана";
        };
        String body = "Ваша заявка №" + event.getApplicationId() + " обработана. Статус: " + statusText;
        Long applicationId = event.getApplicationId();

        for (ChatUser character : characters) {
            Notification notification = notificationService.notify(
                    character.getId(), title, body, "APPLICATION_RESULT", applicationId);

            messagingTemplate.convertAndSendToUser(
                    character.getUsername(),
                    "/queue/notifications",
                    NotificationResponse.from(notification)
            );
        }

        log.info("Application result delivered: applicationId={}, userId={}, status={}, recipients={}",
                applicationId, event.getUserId(), statusText, characters.size());
    }
}
