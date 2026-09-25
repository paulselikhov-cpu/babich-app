package com.platform.civic.service;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.core.modules.application.service.ApplicationService;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.core.modules.application.enums.ApplicationType;
import com.platform.chat.modules.domainEvent.events.ApplicationResolvedEvent;
import com.platform.chat.core.modules.notification.entity.Notification;
import com.platform.chat.core.modules.notification.repository.NotificationRepository;
import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationStatus;
import com.platform.chat.modules.domainEvent.events.CharacterUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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
 * <p>Сквозной тест {@link #approvePassportApplication_issuesPassportAndPublishesEvents()}
 * дополнительно проверяет, что эффект применила стратегия (паспорт выдан,
 * персонаж стал жителем), а событие-последствие CharacterUpdatedEvent приехало
 * в подписчиков из решения стратегии — ядро его типа не знает.</p>
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

    @Autowired
    private CharacterUpdatedEventCaptor characterUpdatedCaptor;

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
        characterUpdatedCaptor.clear();

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

    @Test
    @DisplayName("Сквозной путь: approve заявки на паспорт → документ выдан + уведомление + character-updated")
    void approvePassportApplication_issuesPassportAndPublishesEvents() {
        Application application = applicationService.createPendingApplication(
                externalUserId, characterId, ApplicationType.REGISTER_PASSPORT, null);

        Application approved = applicationService.approveApplication(application.getId());

        // 1. Решение применено ядром: статус + текст результата
        assertThat(approved.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(approved.getResultMessage()).contains("Паспорт выдан");
        assertThat(approved.getProcessedAt()).isNotNull();

        // 2. Эффект применён стратегией: паспорт выдан, персонаж стал жителем
        ChatUser updated = chatUserRepository.findById(characterId).orElseThrow();
        assertThat(updated.getPassportId()).isNotBlank();
        assertThat(updated.getGameStatus()).isEqualTo("RESIDENT");

        // 3. ApplicationResolvedEvent → NotificationEventListener → уведомление заявителю
        List<Notification> notifications = awaitNotifications();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getNotificationType()).isEqualTo("APPLICATION_RESULT");

        // 4. CharacterUpdatedEvent приехал из решения стратегии (ядро его типа не знает)
        assertThat(awaitCharacterUpdatedEvents())
                .anyMatch(event -> characterId.equals(event.getCharacterId()));
    }

    /**
     * Ожидает CharacterUpdatedEvent с таймаутом: событие доставляется подписчикам
     * асинхронно через outbox, сразу после approve его ещё может не быть в списке.
     */
    private List<CharacterUpdatedEvent> awaitCharacterUpdatedEvents() {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            List<CharacterUpdatedEvent> events = characterUpdatedCaptor.getEvents();
            if (!events.isEmpty()) {
                return events;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Прервано ожидание character-updated", e);
            }
        }
        return List.of();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        CharacterUpdatedEventCaptor characterUpdatedEventCaptor() {
            return new CharacterUpdatedEventCaptor();
        }
    }
}