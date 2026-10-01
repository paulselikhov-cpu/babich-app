package com.platform.poll;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.location.entity.Location;
import com.platform.chat.domain.chatUser.enums.SystemRoleType;
import com.platform.chat.domain.location.enums.SystemLocationCode;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import com.platform.chat.domain.location.repository.LocationUserRepository;
import com.platform.chat.domain.location.repository.LocationRepository;
import com.platform.chat.engines.domainEvent.EventType;
import com.platform.chat.engines.domainEvent.events.PollClosedEvent;
import com.platform.chat.engines.poll.entity.Poll;
import com.platform.chat.engines.poll.enums.PollType;
import com.platform.chat.features.governorElection.service.ElectionService;
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
 * DoD test for Stage 6 (PollResultDispatcher).
 *
 * Full path: ElectionService.closeElection -> PollResultDispatcher
 * (implementation selected by PollType) -> ElectionResultHandlerAdapter ->
 * PollClosedEvent published.
 *
 * Dispatcher and handlers are synchronous part of closeElection, so the event
 * is captured with plain @EventListener (no polling): facade does not publish
 * PollClosedEvent by itself, publication is only possible from a handler
 * selected by the dispatcher via poll.getPollType(). The test fails if the
 * dispatcher picked the wrong implementation or the handler did not publish.
 */
@SpringBootTest
class PollClosedEventIntegrationTest {

    @Autowired
    private ElectionService electionService;
    @Autowired
    private ChatUserRepository chatUserRepository;
    @Autowired
    private PollClosedEventCaptor eventCaptor;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private LocationUserRepository locationUserRepository;

    private Long initiatorId;

    @BeforeEach
    void setUp() {
        eventCaptor.clear();
        clearGovernor(1L);
        ChatUser user = new ChatUser();
        long unique = System.nanoTime();
        user.setUserId(90_000L + unique % 1_000_000L);
        user.setCharacterName("PollClosedTest_" + unique);
        user.setUsername("PollClosedTest_" + unique);
        user.setDistrictId(1L);
        user.setCoinBalance(1000L);
        user.setXpUser(50L);
        user.setGameStatus("HOMELESS");
        initiatorId = chatUserRepository.save(user).getId();
    }

    @Test
    @DisplayName("Scenario: closeElection публикует PollClosedEvent через диспетчер (по PollType)")
    void closeElection_publishesPollClosedEvent_viaDispatcher() {
        // 1. Initiate governor election - PollType GOVERNOR_ELECTION
        Poll poll = electionService.initiateElection(1L, initiatorId);
        Long pollId = poll.getId();

        // 2. Close election - dispatcher picks ElectionResultHandlerAdapter
        electionService.closeElection(pollId);

        // 3. Event published by the handler (facade does not publish PollClosedEvent)
        List<PollClosedEvent> events = eventCaptor.getEvents();
        assertThat(events).hasSize(1);
        PollClosedEvent event = events.get(0);

        // 4. Correct payload: event type and pollId
        assertThat(event.getType()).isEqualTo(EventType.POLL_CLOSED);
        assertThat(event.getPollId()).isEqualTo(pollId);

        // 5. pollType matches the poll created by initiateElection:
        // dispatcher passes the exact type through; handler for ELECTION exists
        assertThat(event.getPollType()).isEqualTo(PollType.ELECTION.name());

        // 6. Poll is closed - result processing ran
        assertThat(electionService.getStatus(pollId).getStatus().name()).isEqualTo("CLOSED");
    }

    /** Убирает действующего губернатора района, чтобы гейт «нет губернатора» не блокировал повторный прогон теста. */
    private void clearGovernor(Long districtId) {
        locationRepository.findByDistrictIdAndCode(districtId, SystemLocationCode.CITY_HALL)
                .ifPresent(cityHall ->
                        locationUserRepository.findByLocationIdAndSystemRole(cityHall.getId(), SystemRoleType.GOVERNOR)
                                .ifPresent(locationUserRepository::delete));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PollClosedEventCaptor pollClosedEventCaptor() {
            return new PollClosedEventCaptor();
        }
    }
}