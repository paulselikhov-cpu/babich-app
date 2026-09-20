package com.platform.chat.modules.governorElection.service;

import com.platform.chat.core.modules.poll.entity.Poll;
import com.platform.chat.core.modules.poll.service.PollService;
import com.platform.chat.core.modules.poll.dto.DistrictElectionView;
import com.platform.chat.core.modules.poll.entity.PollCandidate;
import com.platform.chat.core.modules.poll.entity.PollVote;
import com.platform.chat.core.modules.poll.enums.PollStatus;
import com.platform.chat.core.modules.poll.enums.PollType;
import com.platform.chat.core.modules.poll.service.PollResultDispatcher;
import com.platform.chat.core.modules.domainEvent.service.DomainEventPublisher;

import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.base.district.service.DistrictSettingsService;

import com.platform.chat.modules.domainEvent.events.ElectionClosedEvent;
import com.platform.chat.modules.domainEvent.events.ElectionStartedEvent;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Слой 2 (Сценарии).
 * ElectionService — оркестратор выборов губернатора.
 * Собирает шаги сценария "Выборы губернатора" в один вызов, понятный клиенту.
 * Не содержит бизнес-логики — делегирует в Слой 3 (PollService).
 * После закрытия голосования результат обрабатывает PollResultDispatcher
 * (Слой 4, выбирает реализацию по PollType) и публикует ElectionClosedEvent —
 * уведомления жителям района и WS-пуш делает независимый подписчик (Слой 4),
 * а не этот фасад напрямую.
 */
@Service
@RequiredArgsConstructor
public class ElectionService {

    private static final Logger log = LoggerFactory.getLogger(ElectionService.class);

    private final PollService pollService;
    private final PollResultDispatcher pollResultDispatcher;
    private final DomainEventPublisher eventPublisher;
    private final ChatUserRepository chatUserRepository;
    private final DistrictSettingsService districtSettingsService;

    /**
     * Шаг 1: Инициировать выборы.
     * Персонаж initiatorCharacterId начинает выборы губернатора в районе.
     * Он автоматически становится первым кандидатом (слот 1).
     * Остальные 4 слота пустые — ждут кандидатов.
     *
     * <p>Публикует ElectionStartedEvent — жителей района уведомляет
     * NotificationEventListener (Слой 4) через Notification + WS-пуш.</p>
     */
    @Transactional
    public Poll initiateElection(Long districtId, Long initiatorCharacterId) {
        log.info("===== Election initiated: districtId={}, initiator={} =====", districtId, initiatorCharacterId);
        Poll poll = pollService.createGovernorElection(districtId, initiatorCharacterId);
        eventPublisher.publish(new ElectionStartedEvent(districtId, poll.getId(), initiatorCharacterId));
        return poll;
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
    private void publishElectionClosed(Poll closed) { //TODO: есть PollClosedEvent - нужен ли нам ElectionClosedEvent?
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

    public Map<Long, Integer> getResults(Long pollId) {
        return pollService.countVotes(pollId);
    }

    /**
     * Собрать состояние выборов района для фронтовой мини-таблицы.
     * Выбирает релевантное голосование: активное (ACTIVE/PENDING), иначе —
     * последнее завершённое (для показа результата). Пусто, если выборов не было.
     *
     * @param districtId  район
     * @param characterId текущий персонаж (внутренний ChatUser.id) или null
     */
    public DistrictElectionView getDistrictOverview(Long districtId, Long characterId) {
        List<Poll> elections = pollService.findByDistrict(districtId).stream()
                .filter(p -> p.getPollType() == PollType.ELECTION)
                .sorted(Comparator.comparing(Poll::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        Poll relevant = elections.stream()
                .filter(p -> p.getStatus() == PollStatus.ACTIVE || p.getStatus() == PollStatus.PENDING)
                .findFirst()
                .orElseGet(() -> elections.stream()
                        .filter(p -> p.getStatus() == PollStatus.CLOSED)
                        .findFirst()
                        .orElse(null));

        if (relevant == null) {
            return DistrictElectionView.EMPTY;
        }

        Map<Long, Integer> votes = pollService.countVotes(relevant.getId());
        List<PollCandidate> candidates = pollService.getCandidates(relevant.getId()).stream()
                .sorted(Comparator.comparingInt(PollCandidate::getSlotNumber))
                .toList();

        List<DistrictElectionView.CandidateView> views = candidates.stream().map(c -> {
            Long charId = c.getCharacterId();
            String name = charId == null ? null
                    : chatUserRepository.findById(charId).map(u -> u.getCharacterName()).orElse(null);
            long count = charId == null ? 0L : (long) votes.getOrDefault(c.getId(), 0);
            return new DistrictElectionView.CandidateView(c.getSlotNumber(), c.getId(), charId, name, count);
        }).toList();

        int totalVoted = pollService.totalVoteCount(relevant.getId());
        int aliveWindowDays = districtSettingsService.getElectionSettings(districtId).aliveWindowDays();
        long alive = chatUserRepository.countByDistrictIdAndLastSeenAfter(
                districtId, LocalDateTime.now().minusDays(aliveWindowDays));
        int quorum = relevant.getQuorumThreshold();
        String winnerName = relevant.getStatus() == PollStatus.CLOSED ? resolveWinnerName(relevant.getId()) : null;

        return new DistrictElectionView(
                true,
                relevant.getId(),
                relevant.getStatus().name(),
                relevant.getEndsAt() != null ? relevant.getEndsAt().toEpochMilli() : null,
                totalVoted,
                (int) alive,
                quorum,
                totalVoted >= quorum,
                characterId != null && pollService.hasVoted(relevant.getId(), characterId),
                characterId != null && candidates.stream().anyMatch(c -> characterId.equals(c.getCharacterId())),
                winnerName,
                views
        );
    }
}