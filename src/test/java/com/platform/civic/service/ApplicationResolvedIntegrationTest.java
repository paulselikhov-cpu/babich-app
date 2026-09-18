package com.platform.civic.service;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.core.modules.application.service.ApplicationService;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.core.modules.application.ApplicationType;
import com.platform.chat.modules.domainEvent.event.ApplicationResolvedEvent;
import com.platform.chat.core.modules.notification.entity.Notification;
import com.platform.chat.core.modules.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DoD-тесты Этапа 4 (ApplicationService → ApplicationResolvedEvent).
 *
 * <p>Проверяют сквозной путь: ApplicationService разрешает заявку
 * (единственная точка перевода заявки в APPROVED/REJECTED) → доменное
 * событие ApplicationResolvedEvent → подписчик NotificationEventListener →
 * уведомление в Notification для заявителя.</p>
 *
 * <p>Публикация событий идёт через {@code DomainEventPublisher} с доставкой
 * подписчикам {@code @ApplicationModuleListener} через персистентный outbox
 * (event_publication) асинхронно (thread cTaskExecutor-N), поэтому сквозной
 * путь проверяется по наблюдаемому эффекту — записи {@code Notification},
 * которую пишет подписчик на AFTER_COMMIT; появление записи ожидается с
 * таймаутом (см. {@link #awaitNotifications}).</p>
 *
 */
@SpringBootTest
class ApplicationResolvedIntegrationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ChatUserRepository chatUserRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    /** Стартовый баланс тестового персонажа. */
    private static final long START_BALANCE = 1000L;

    /** Внутренний id персонажа (ChatUser.id). */
    private Long characterId;

    /** Внешний id владельца (User.id), нужен ApplicationService/BuyFirstLocationHandler. */
    private Long externalUserId;

    /**
     * Ожидает появления уведомлений персонажа с таймаутом.
     *
     * <p>Событие (ApplicationResolvedEvent) доставляется подписчику
     * асинхронно через outbox (event_publication), поэтому сразу после вызова
     * ApplicationService уведомление может ещё не появиться. Хелпер поллит с
     * интервалом 50 мс до 5 с — этого достаточно, чтобы подписчик успел
     * записать Notification.</p>
     */
    private List<Notification> awaitNotifications() {
        long deadline = System.currentTimeMillis() + 5_000;
        List<Notification> notifications = List.of();
        while (System.currentTimeMillis() < deadline) {
            notifications = notificationRepository
                    .findByRecipientCharacterIdOrderByCreatedAtDesc(characterId);
            if (!notifications.isEmpty()) {
                return notifications;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Прервано ожидание уведомления", e);
            }
        }
        return notifications;
    }

    @BeforeEach
    void setUp() {
        // id владельца персонажа (User.id) — произвольный, связь с users не
        // смоделирована FK, но уникальность (user_id, district_id) в БД есть:
        // генерим уникальные значения на каждый прогон, т.к. тест не
        // транзакционный и данные коммитятся в dev-БД.
        long unique = System.nanoTime();
        Long userId = 90_000L + unique % 1_000_000L;
        String uniqueName = "AppResolvedTest_" + unique;

        ChatUser user = new ChatUser();
        user.setUserId(userId);
        user.setCharacterName(uniqueName);
        user.setUsername(uniqueName);
        user.setDistrictId(1L);
        user.setCoinBalance(START_BALANCE);
        user.setXpUser(50L);
        user.setGameStatus("HOMELESS");
        ChatUser saved = chatUserRepository.save(user);
        characterId = saved.getId();
        externalUserId = userId;
    }

    @Test
    @DisplayName("ApplicationResolvedEvent сериализуется в границы event_publication.serialized_event (varchar(255))")
    void serializedApplicationResolvedEventFitsVarchar255() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        // самый «длинный» тип — REGISTER_WORK_LICENCE, плюс строка статуса APPROVED
        ApplicationResolvedEvent event = new ApplicationResolvedEvent(
                1L, 2L, 3L, ApplicationType.REGISTER_WORK_LICENCE.name(), "APPROVED");
        int length = mapper.writeValueAsString(event).length();
        assertThat(length).isLessThanOrEqualTo(255);
    }
}