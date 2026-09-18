package com.platform.notification.listener;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.modules.domainEvent.listener.NotificationEventListener;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.modules.domainEvent.event.ElectionClosedEvent;
import com.platform.chat.modules.domainEvent.event.ApplicationResolvedEvent;
import com.platform.chat.core.modules.notification.dto.NotificationResponse;
import com.platform.chat.core.modules.notification.dto.NotificationContextData;
import com.platform.chat.core.modules.notification.entity.Notification;
import com.platform.chat.core.modules.notification.service.NotificationService;
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
import java.util.Map;
import java.util.Optional;

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

    @Captor
    private ArgumentCaptor<Map<String, Object>> dataCaptor;

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

    @Test
    void shouldNotifyApplicantOnApprovedApplicationWithContextData() {
        // given — заявка одобрена для конкретного персонажа (addressivity по characterId)
        Long characterId = 5L;
        ChatUser character = createResident(characterId, "petrov", 1L);
        when(chatUserRepository.findById(characterId)).thenReturn(Optional.of(character));

        // сервис возвращает сохранённое уведомление с данными контекста
        Notification notification = createNotification(characterId, 10L);
        notification.setNotificationType("APPLICATION_RESULT");
        notification.setData(NotificationContextData.toText(
                Map.of("applicationType", "REGISTER_PASSPORT", "applicationStatus", "APPROVED")));
        when(notificationService.notify(
                eq(characterId), anyString(), anyString(), eq("APPLICATION_RESULT"), eq(10L), anyMap()))
                .thenReturn(notification);

        ApplicationResolvedEvent event = new ApplicationResolvedEvent(
                1L, characterId, 10L, "REGISTER_PASSPORT", "APPROVED");

        // when
        listener.onApplicationResolved(event);

        // then — notify вызван с context-данными и человеко-читаемым текстом
        // (тип + статус для фронта; никаких «Статус: PENDING»)
        verify(notificationService).notify(
                eq(characterId),
                argThat(t -> t.contains("одобрена")),
                argThat(b -> !b.contains("PENDING")),
                eq("APPLICATION_RESULT"),
                eq(10L),
                dataCaptor.capture());
        assertThat(dataCaptor.getValue())
                .containsEntry("applicationType", "REGISTER_PASSPORT")
                .containsEntry("applicationStatus", "APPROVED");

        // and — WS-пуш адресован именно этому персонажу, а ответ несёт data
        verify(messagingTemplate).convertAndSendToUser(
                eq("petrov"), eq("/queue/notifications"), responseCaptor.capture());
        NotificationResponse response = responseCaptor.getValue();
        assertThat(response.notificationType()).isEqualTo("APPLICATION_RESULT");
        assertThat(response.relatedEntityId()).isEqualTo(10L);
        assertThat(response.data())
                .containsEntry("applicationType", "REGISTER_PASSPORT")
                .containsEntry("applicationStatus", "APPROVED");
    }

    @Test
    void shouldSkipWhenCharacterNotFound() {
        // given — события нет персонажа (сломанная заявка/удалённый персонаж)
        when(chatUserRepository.findById(anyLong())).thenReturn(Optional.empty());
        ApplicationResolvedEvent event = new ApplicationResolvedEvent(1L, 99L, 10L, "REGISTER_PASSPORT", "APPROVED");

        // when
        listener.onApplicationResolved(event);

        // then — без notify и WS
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