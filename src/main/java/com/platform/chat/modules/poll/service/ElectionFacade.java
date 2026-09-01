package com.platform.chat.modules.poll.service;

import com.platform.chat.modules.poll.entity.Poll;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.core.event.DomainEventPublisher;
import com.platform.chat.core.event.ElectionClosedEvent;
import com.platform.chat.modules.poll.entity.PollCandidate;
import com.platform.chat.modules.poll.entity.PollVote;
import com.platform.chat.modules.poll.handler.PollResultDispatcher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Слой 2 (Сценарии / Workflows).
 * ElectionFacade — оркестратор выборов губернатора.
 * Собирает шаги сценария №2 в один вызов, понятный клиенту.
 * Не содержит бизнес-логики — делегирует в Слой 3 (PollService).
 * После закрытия голосования результат обрабатывает PollResultDispatcher
 * (Слой 4, выбирает реализацию по PollType) и публикует ElectionClosedEvent —
 * уведомления жителям района и WS-пуш делает независимый подписчик (Слой 4),
 * а не этот фасад напрямую.
 */
@Service
@RequiredArgsConstructor
public class ElectionFacade {

    private static final Logger log = LoggerFactory.getLogger(ElectionFacade.class);

    private final PollService pollService;
    private final PollResultDispatcher pollResultDispatcher;
    private final DomainEventPublisher eventPublisher;
    private final ChatUserRepository chatUserRepository;

    /**
     * Шаг 1: Инициировать выборы.
     * Персонаж initiatorCharacterId начинает выборы губернатора в районе.
     * Он автоматически становится первым кандидатом (слот 1).
     * Остальные 4 слота пустые — ждут кандидатов.
     */
    public Poll initiateElection(Long districtId, Long initiatorCharacterId) {
        log.info("===== Election initiated: districtId={}, initiator={} =====", districtId, initiatorCharacterId);
        return pollService.createGovernorElection(districtId, initiatorCharacterId);
    }

    /**
     * Шаг 2: Кандидат занимает свободный слот.
     */
    public PollCandidate joinAsCandidate(Long pollId, Long characterId) {
        log.info("Character joins election: pollId={}, characterId={}", pollId, characterId);
        return pollService.occupySlot(pollId, characterId);
    }

    /**
     * Шаг 3: Персонаж голосует за кандидата.
     */
    public PollVote castVote(Long pollId, Long characterId, Long candidateId) {
        log.info("Character votes: pollId={}, characterId={}, candidateId={}", pollId, characterId, candidateId);
        return pollService.castVote(pollId, characterId, candidateId);
    }

    /**
     * Шаг 4: Закрыть выборы и обработать результат.
     * Может быть вызвано вручную (администратором) или по таймеру (когда истёк endsAt).
     *
     * @return Poll со статусом CLOSED
     */
    @Transactional
    public Poll closeElection(Long pollId) {
        log.info("===== Closing election: pollId={} =====", pollId);

        // Закрыть голосование
        Poll closed = pollService.closePoll(pollId);

        // Подсчитать голоса
        Map<Long, Integer> votes = pollService.countVotes(pollId);
        log.info("Vote results for poll {}: {}", pollId, votes);

        // Обработать результат через диспетчер (Слой 4): назначает должности,
        // каскад голосований и публикует PollClosedEvent.
        pollResultDispatcher.dispatch(closed);

        // Опубликовать доменное событие — подписчики (уведомления/WS) реагируют сами
        publishElectionClosed(closed);

        log.info("===== Election closed and governor appointed: pollId={} =====", pollId);
        return closed;
    }

    /**
     * Публикует ElectionClosedEvent после закрытия выборов.
     * Подписчик (NotificationEventListener) разошлёт уведомления жителям района
     * и сделает WS-пуш — фасад больше не знает про Слой 4.
     */
    private void publishElectionClosed(Poll closed) {
        String winnerName = resolveWinnerName(closed.getId());
        eventPublisher.publish(new ElectionClosedEvent(closed.getScopeId(), closed.getId(), winnerName));
    }

    private String resolveWinnerName(Long pollId) {
        PollCandidate winner = pollService.getCandidates(pollId).stream()
                .filter(c -> c.getCharacterId() != null)
                .max((a, b) -> Long.compare(a.getVoteCount(), b.getVoteCount()))
                .orElse(null);
        if (winner == null) {
            return "не определён";
        }
        return chatUserRepository.findById(winner.getCharacterId())
                .map(u -> u.getCharacterName())
                .orElse("неизвестный");
    }

    // ===== Статусы и чтение =====

    public Poll getStatus(Long pollId) {
        return pollService.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено: " + pollId));
    }

    public List<PollCandidate> getCandidates(Long pollId) {
        return pollService.getCandidates(pollId);
    }

    public List<Poll> getElectionsInDistrict(Long districtId) {
        return pollService.findByDistrict(districtId);
    }

    public boolean hasCharacterVoted(Long pollId, Long characterId) {
        return pollService.hasVoted(pollId, characterId);
    }

    public Map<Long, Integer> getResults(Long pollId) {
        return pollService.countVotes(pollId);
    }
}