package com.platform.poll;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.core.event.EventType;
import com.platform.chat.core.event.PollClosedEvent;
import com.platform.chat.modules.poll.entity.Poll;
import com.platform.chat.modules.poll.enums.PollType;
import com.platform.chat.modules.poll.service.ElectionFacade;
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
 * Full path: ElectionFacade.closeElection -> PollResultDispatcher
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
    private ElectionFacade electionFacade;
    @Autowired
    private ChatUserRepository chatUserRepository;
    @Autowired
    private PollClosedEventCaptor eventCaptor;

    private Long initiatorId;

    @BeforeEach
    void setUp() {
        eventCaptor.clear();
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
        Poll poll = electionFacade.initiateElection(1L, initiatorId);
        Long pollId = poll.getId();

        // 2. Close election - dispatcher picks ElectionResultHandlerAdapter
        electionFacade.closeElection(pollId);

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
        assertThat(electionFacade.getStatus(pollId).getStatus().name()).isEqualTo("CLOSED");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PollClosedEventCaptor pollClosedEventCaptor() {
            return new PollClosedEventCaptor();
        }
    }
}