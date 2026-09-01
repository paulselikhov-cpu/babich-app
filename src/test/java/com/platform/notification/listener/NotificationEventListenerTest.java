package com.platform.notification.listener;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.notification.listener.NotificationEventListener;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.core.event.ElectionClosedEvent;
import com.platform.chat.modules.notification.dto.NotificationResponse;
import com.platform.chat.modules.notification.entity.Notification;
import com.platform.chat.modules.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Регрессия Этапа 2: NotificationEventListener реагирует на ElectionClosedEvent,
 * не требует прямого вызова из ElectionFacade.
 *
 * Проверяет: при получении события — сохраняется уведомление и шлётся WS-пуш
 * каждому жителю района. listener не падает на пустом районе.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ChatUserRepository chatUserRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationEventListener listener;

    @Captor
    private ArgumentCaptor<NotificationResponse> responseCaptor;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(
                notificationService, chatUserRepository, messagingTemplate);
    }

    @Test
    void shouldNotifyAndPushToAllResidents() {
        // given
        Long districtId = 1L;
        Long pollId = 10L;
        String winnerName = "Иван Иванов";

        ChatUser resident1 = createResident(100L, "user1", districtId);
        ChatUser resident2 = createResident(200L, "user2", districtId);
        List<ChatUser> residents = List.of(resident1, resident2);

        Notification notification1 = createNotification(resident1.getId(), pollId);
        Notification notification2 = createNotification(resident2.getId(), pollId);

        when(chatUserRepository.findByDistrictId(districtId)).thenReturn(residents);
        when(notificationService.notify(
                eq(resident1.getId()), anyString(), anyString(), eq("ELECTION_RESULT"), eq(pollId)))
                .thenReturn(notification1);
        when(notificationService.notify(
                eq(resident2.getId()), anyString(), anyString(), eq("ELECTION_RESULT"), eq(pollId)))
                .thenReturn(notification2);

        ElectionClosedEvent event = new ElectionClosedEvent(districtId, pollId, winnerName);

        // when
        listener.onElectionClosed(event);

        // then — уведомления сохранены для обоих жителей
        verify(notificationService, times(2)).notify(
                anyLong(), anyString(), anyString(), eq("ELECTION_RESULT"), eq(pollId));

        // and — WS-пуш отправлен каждому resident
        verify(messagingTemplate, times(2))
                .convertAndSendToUser(anyString(), eq("/queue/notifications"), any(NotificationResponse.class));

        // and — WS-пуш содержит правильные данные (проверяем первого)
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"), eq("/queue/notifications"), responseCaptor.capture());
        NotificationResponse response = responseCaptor.getValue();
        assertThat(response.title()).isEqualTo("Выборы губернатора завершены");
        assertThat(response.notificationType()).isEqualTo("ELECTION_RESULT");
        assertThat(response.relatedEntityId()).isEqualTo(pollId);
    }

    @Test
    void shouldSkipWhenNoResidents() {
        // given
        when(chatUserRepository.findByDistrictId(anyLong())).thenReturn(List.of());
        ElectionClosedEvent event = new ElectionClosedEvent(1L, 10L, "Иван");

        // when
        listener.onElectionClosed(event);

        // then — ни уведомлений, ни WS-пушей
        verify(notificationService, never()).notify(anyLong(), anyString(), anyString(), anyString(), anyLong());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    private static ChatUser createResident(Long id, String username, Long districtId) {
        ChatUser user = new ChatUser();
        user.setId(id);
        user.setUsername(username);
        user.setDistrictId(districtId);
        return user;
    }

    private static Notification createNotification(Long recipientId, Long pollId) {
        Notification n = new Notification();
        n.setId(1L);
        n.setRecipientCharacterId(recipientId);
        n.setTitle("Выборы губернатора завершены");
        n.setBody("Губернатором избран Иван Иванов. Результаты доступны на площади Ленина.");
        n.setNotificationType("ELECTION_RESULT");
        n.setRelatedEntityId(pollId);
        n.setCreatedAt(Instant.now());
        return n;
    }
}