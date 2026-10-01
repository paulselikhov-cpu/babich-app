package com.platform.core.scheduler;

import com.platform.chat.engines.scheduledCheck.entity.ScheduledCheck;
import com.platform.chat.engines.scheduledCheck.repository.ScheduledCheckRepository;
import com.platform.chat.engines.scheduledCheck.enums.ScheduledCheckStatus;
import com.platform.chat.engines.scheduledCheck.service.SchedulerGateway;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.chatUser.enums.SystemRoleType;
import com.platform.chat.domain.location.enums.LocationType;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import com.platform.chat.domain.location.repository.LocationUserRepository;
import com.platform.chat.domain.location.repository.LocationRepository;
import com.platform.chat.engines.notification.entity.Notification;
import com.platform.chat.engines.notification.repository.NotificationRepository;
import com.platform.chat.engines.poll.entity.Poll;
import com.platform.chat.features.governorElection.sheduledCheck.ElectionClose;
import com.platform.chat.features.governorElection.service.ElectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DoD-тест Этапа 5 (SchedulerGateway).
 *
 * Сквозной путь: истечение dueAt в ScheduledCheck → onTick() (вручную) →
 * PollCloseCheckHandler → ElectionService.closeElection → ElectionClosedEvent →
 * NotificationEventListener → Notification жителю.
 */
@SpringBootTest
class SchedulerGatewayIntegrationTest {

    @Autowired private ElectionService electionService;
    @Autowired private ChatUserRepository chatUserRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ScheduledCheckRepository scheduledCheckRepository;
    @Autowired private SchedulerGateway schedulerGateway;
    @Autowired private LocationRepository locationRepository;
    @Autowired private LocationUserRepository locationUserRepository;

    private Long characterId;

    @BeforeEach
    void setUp() {
        clearGovernor(1L);
        long unique = System.nanoTime();
        Long userId = 90_000L + unique % 1_000_000L;
        String uniqueName = "SchedGatewayTest_" + unique;

        ChatUser user = new ChatUser();
        user.setUserId(userId);
        user.setCharacterName(uniqueName);
        user.setUsername(uniqueName);
        user.setDistrictId(1L);
        user.setCoinBalance(1000L);
        user.setXpUser(50L);
        user.setGameStatus("HOMELESS");
        characterId = chatUserRepository.save(user).getId();
    }

    /**
     * Ожидает уведомление нужного типа у тестового персонажа.
     *
     * <p>Важно ждать именно ЗАДАННЫЙ тип: до него персонаж получает
     * {@code ELECTION_STARTED} (событие инициации выборов доставляется
     * асинхронно через outbox) — поэтому «любое непустое» уведомление
     * возвращалось бы раньше, чем придёт {@code ELECTION_RESULT}, и тест
     * падал на разнице числа получателей в районе.</p>
     */
    private List<Notification> awaitNotifications(String notificationType) {
        long deadline = System.currentTimeMillis() + 5_000;
        List<Notification> notifications = List.of();
        while (System.currentTimeMillis() < deadline) {
            notifications = notificationRepository
                    .findByRecipientCharacterIdOrderByCreatedAtDesc(characterId);
            if (notifications.stream()
                    .anyMatch(n -> notificationType.equals(n.getNotificationType()))) {
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

    @Test
    @DisplayName("Scenario: истечение dueAt в ScheduledCheck закрывает выборы и публикует событие (уведомление жителю)")
    void expiredScheduledCheck_closesPoll_andPublishesEvent() {
        // 1. Инициировать выборы — PollService регистрирует ScheduledCheck (dueAt = endsAt)
        Poll poll = electionService.initiateElection(1L, characterId);
        Long pollId = poll.getId();

        List<ScheduledCheck> pending = scheduledCheckRepository
                .findByCheckTypeAndEntityTypeAndEntityIdAndStatus(
                        ElectionClose.CHECK_TYPE, "Poll", pollId, ScheduledCheckStatus.PENDING);
        assertThat(pending).hasSize(1);
        ScheduledCheck check = pending.get(0);

        // 2. Смоделировать истечение срока
        check.setDueAt(Instant.now().minusSeconds(60));
        scheduledCheckRepository.save(check);

        // 3. Дёрнуть тик вручную — без ожидания реального fixedDelay-интервала
        schedulerGateway.onTick();

        // 4. Сквозной путь: обработчик → closeElection → ElectionClosedEvent → Notification
        List<Notification> notifications = awaitNotifications("ELECTION_RESULT");
        assertThat(notifications).anySatisfy(n -> {
            assertThat(n.getRecipientCharacterId()).isEqualTo(characterId);
            assertThat(n.getNotificationType()).isEqualTo("ELECTION_RESULT");
            assertThat(n.getRelatedEntityId()).isEqualTo(pollId);
            assertThat(n.getTitle()).contains("Выборы губернатора");
        });

        // 5. Проверка закрыта — повторно не срабатывает
        assertThat(scheduledCheckRepository.findById(check.getId()).orElseThrow()
                .getStatus()).isEqualTo(ScheduledCheckStatus.COMPLETED);

        // 6. Poll реально закрыт
        assertThat(electionService.getStatus(pollId).getStatus().name()).isEqualTo("CLOSED");
    }

    /** Убирает действующего губернатора района, чтобы гейт «нет губернатора» не блокировал повторный прогон теста. */
    private void clearGovernor(Long districtId) {
        List<Location> systemLocations = locationRepository.findByDistrictIdAndIsSystemTrue(districtId);
        for (Location loc : systemLocations) {
            if (loc.getType() == LocationType.CITY_HALL) {
                locationUserRepository.findByLocationIdAndSystemRole(loc.getId(), SystemRoleType.GOVERNOR)
                        .ifPresent(locationUserRepository::delete);
            }
        }
    }
}