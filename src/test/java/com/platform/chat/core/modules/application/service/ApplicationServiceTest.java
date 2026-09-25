package com.platform.chat.core.modules.application.service;

import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.core.modules.application.enums.ApplicationStatus;
import com.platform.chat.core.modules.application.enums.ApplicationType;
import com.platform.chat.core.modules.application.exception.ApplicationAlreadyResolvedException;
import com.platform.chat.core.modules.application.exception.ApplicationNotFoundException;
import com.platform.chat.core.modules.application.i18n.ApplicationMessages;
import com.platform.chat.core.modules.application.model.ApplicationDecision;
import com.platform.chat.core.modules.application.model.ApplicationHandler;
import com.platform.chat.core.modules.application.repository.ApplicationRepository;
import com.platform.chat.core.modules.domainEvent.DomainEvent;
import com.platform.chat.core.modules.domainEvent.service.DomainEventPublisher;
import com.platform.chat.modules.domainEvent.events.ApplicationResolvedEvent;
import com.platform.chat.modules.domainEvent.events.CharacterUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ядро процесса заявки: применяет решение стратегии (статус + текст) и публикует
 * два вида событий в одной транзакции — процессное {@link ApplicationResolvedEvent}
 * и события-последствия из решения ({@link CharacterUpdatedEvent}). Конкретные
 * типы событий ядро при этом не знает — проверяем это на мок-стратегии.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Long APPLICATION_ID = 1L;
    private static final Long CHARACTER_ID = 7L;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationHandlerRegistry handlerRegistry;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private ApplicationMessages messages;

    @Mock
    private ApplicationHandler handler;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applicationRepository, handlerRegistry, eventPublisher, messages);
    }

    private Application pendingApplication() {
        Application application = new Application();
        application.setId(APPLICATION_ID);
        application.setUserId(100L);
        application.setCharacterId(CHARACTER_ID);
        application.setType(ApplicationType.REGISTER_PASSPORT);
        application.setStatus(ApplicationStatus.PENDING);
        return application;
    }

    @Test
    void approveAppliesDecisionAndPublishesProcessEventWithEffects() {
        Application application = pendingApplication();
        ApplicationDecision decision = ApplicationDecision.approved(
                ApplicationMessageKey.PASSPORT_ISSUED,
                List.of(new CharacterUpdatedEvent(CHARACTER_ID)),
                "PP-1111-2222");

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(handlerRegistry.get(ApplicationType.REGISTER_PASSPORT)).thenReturn(handler);
        when(handler.resolve(application)).thenReturn(decision);
        when(applicationRepository.save(application)).thenReturn(application);
        when(messages.resolve(eq(ApplicationMessageKey.PASSPORT_ISSUED), any(Object[].class)))
                .thenReturn("Паспорт выдан: PP-1111-2222");

        Application resolved = service.approveApplication(APPLICATION_ID);

        assertThat(resolved.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(resolved.getResultMessage()).isEqualTo("Паспорт выдан: PP-1111-2222");
        assertThat(resolved.getProcessedAt()).isNotNull();

        ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(events.capture());
        assertThat(events.getAllValues())
                .hasAtLeastOneElementOfType(CharacterUpdatedEvent.class)
                .hasAtLeastOneElementOfType(ApplicationResolvedEvent.class);
    }

    @Test
    void rejectDoesNotUseHandlersAndPublishesOnlyProcessEvent() {
        Application application = pendingApplication();
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        Application rejected = service.rejectApplication(APPLICATION_ID, "не подходит");

        assertThat(rejected.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(rejected.getResultMessage()).isEqualTo("не подходит");
        assertThat(rejected.getProcessedAt()).isNotNull();
        verify(eventPublisher).publish(any(ApplicationResolvedEvent.class));
        verify(handlerRegistry, never()).get(any(ApplicationType.class));
    }

    @Test
    void approveFailsWhenApplicationMissing() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveApplication(APPLICATION_ID))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining(String.valueOf(APPLICATION_ID));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    void approveFailsWhenApplicationAlreadyResolved() {
        Application application = pendingApplication();
        application.setStatus(ApplicationStatus.APPROVED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approveApplication(APPLICATION_ID))
                .isInstanceOf(ApplicationAlreadyResolvedException.class)
                .hasMessageContaining("APPROVED");
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
        verify(handlerRegistry, never()).get(any(ApplicationType.class));
        verify(applicationRepository, never()).save(any(Application.class));
    }
}
