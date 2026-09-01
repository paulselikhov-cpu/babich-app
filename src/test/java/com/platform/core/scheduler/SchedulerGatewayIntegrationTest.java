package com.platform.core.scheduler;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.notification.entity.Notification;
import com.platform.notification.repository.NotificationRepository;
import com.platform.poll.entity.Poll;
import com.platform.poll.scheduler.PollCloseCheckHandler;
import com.platform.poll.service.ElectionFacade;
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
 * PollCloseCheckHandler → ElectionFacade.closeElection → ElectionClosedEvent →
 * NotificationEventListener → Notification жителю.
 */
@SpringBootTest
class SchedulerGatewayIntegrationTest {

    @Autowired private ElectionFacade electionFacade;
    @Autowired private ChatUserRepository chatUserRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ScheduledCheckRepository scheduledCheckRepository;
    @Autowired private SchedulerGateway schedulerGateway;

    private Long characterId;

    @BeforeEach
    void setUp() {
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

    private List<Notification> awaitNotifications() {
        long deadline = System.currentTimeMillis() + 5_000;
        List<Notification> notifications = List.of();
        while (System.currentTimeMillis() < deadline) {
            notifications = notificationRepository
                    .findByRecipientCharacterIdOrderByCreatedAtDesc(characterId);
            if (!notifications.isEmpty()) return notifications;
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
        Poll poll = electionFacade.initiateElection(1L, characterId);
        Long pollId = poll.getId();

        List<ScheduledCheck> pending = scheduledCheckRepository
                .findByCheckTypeAndEntityTypeAndEntityIdAndStatus(
                        PollCloseCheckHandler.CHECK_TYPE, "Poll", pollId, ScheduledCheckStatus.PENDING);
        assertThat(pending).hasSize(1);
        ScheduledCheck check = pending.get(0);

        // 2. Смоделировать истечение срока
        check.setDueAt(Instant.now().minusSeconds(60));
        scheduledCheckRepository.save(check);

        // 3. Дёрнуть тик вручную — без ожидания реального fixedDelay-интервала
        schedulerGateway.onTick();

        // 4. Сквозной путь: обработчик → closeElection → ElectionClosedEvent → Notification
        List<Notification> notifications = awaitNotifications();
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
        assertThat(electionFacade.getStatus(pollId).getStatus().name()).isEqualTo("CLOSED");
    }
}