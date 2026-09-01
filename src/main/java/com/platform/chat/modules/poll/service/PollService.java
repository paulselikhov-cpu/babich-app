package com.platform.chat.modules.poll.service;

import com.platform.chat.entity.Location;
import com.platform.chat.enums.PublicLocationType;
import com.platform.chat.modules.poll.repository.PollCandidateRepository;
import com.platform.chat.modules.poll.repository.PollRepository;
import com.platform.chat.modules.poll.repository.PollVoteRepository;
import com.platform.chat.modules.poll.scheduler.PollCloseCheckHandler;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.district.DistrictRepository;
import com.platform.chat.repository.location.LocationRepository;
import com.platform.chat.core.scheduler.ScheduledCheckService;
import com.platform.chat.modules.poll.entity.Poll;
import com.platform.chat.modules.poll.entity.PollCandidate;
import com.platform.chat.modules.poll.entity.PollVote;
import com.platform.chat.modules.poll.enums.PollStatus;
import com.platform.chat.modules.poll.enums.PollType;
import com.platform.chat.modules.poll.enums.ScopeType;
import com.platform.chat.modules.poll.enums.WeightStrategy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Слой 3 (Процессы). Сервис голосований — создание, голосование, закрытие.
 * Не содержит логики последствий — это ответственность Слоя 4 (Обработчики результатов).
 */
@Service
@RequiredArgsConstructor
public class PollService {

    private static final Logger log = LoggerFactory.getLogger(PollService.class);

    public static final int MAX_CANDIDATES = 5;
    public static final int DEFAULT_QUORUM = 50; // %

    private final PollRepository pollRepository;
    private final PollCandidateRepository candidateRepository;
    private final PollVoteRepository voteRepository;
    private final DistrictRepository districtRepository;
    private final LocationRepository locationRepository;
    private final ChatUserRepository chatUserRepository;
    private final ScheduledCheckService scheduledCheckService;

    // ===== Создание голосования =====

    /**
     * Создать выборы губернатора (Governor Election) для района.
     * Статус: PENDING. Начинается немедленно, заканчивается через 24 часа.
     */
    @Transactional
    public Poll createGovernorElection(Long districtId, Long initiatorCharacterId) {
        // Проверить, что район существует
        districtRepository.findById(districtId)
                .orElseThrow(() -> new IllegalArgumentException("Район не найден: " + districtId));

        Location leninSquare = locationRepository
                .findByDistrictIdAndIsSystemTrueAndType(districtId, PublicLocationType.LENIN_SQUARE)
                .orElseThrow(() -> new IllegalStateException("Площадь Ленина не найдена в районе: " + districtId));

        // Кворум = 50% от живых участников района
        long aliveCount = chatUserRepository.countByDistrictIdAndLastSeenAfter(
                districtId, LocalDateTime.now().minusDays(2));
        int quorumThreshold = Math.max(1, (int) Math.ceil(aliveCount * 0.5));

        Instant now = Instant.now();
        Poll poll = new Poll();
        poll.setScopeType(ScopeType.DISTRICT);
        poll.setScopeId(districtId);
        poll.setLocationId(leninSquare.getId());
        poll.setPollType(PollType.ELECTION);
        poll.setHasCandidates(true);
        poll.setWeightStrategy(WeightStrategy.ONE_VOTE);
        poll.setStartsAt(now);
        poll.setEndsAt(now.plusSeconds(24 * 60 * 60));
        poll.setQuorumThreshold(quorumThreshold);
        poll.setStatus(PollStatus.ACTIVE);
        poll.setSourceApplicationId(initiatorCharacterId);
        poll = pollRepository.save(poll);

        // Реестр отложенных проверок (Этап 5): закрытие выборов по истечении
        // endsAt идёт через ScheduledCheck, а не через полный скан таблицы polls.
        scheduledCheckService.schedule(
                PollCloseCheckHandler.CHECK_TYPE,
                Poll.class.getSimpleName(),
                poll.getId(),
                poll.getEndsAt());
        log.info("Scheduled check registered for poll close: pollId={}, dueAt={}",
                poll.getId(), poll.getEndsAt());

        // Создать инициатора первым кандидатом (слот 1)
        PollCandidate first = new PollCandidate();
        first.setPollId(poll.getId());
        first.setCharacterId(initiatorCharacterId);
        first.setSlotNumber(1);
        candidateRepository.save(first);

        // Создать пустые слоты 2-5
        for (int i = 2; i <= MAX_CANDIDATES; i++) {
            PollCandidate slot = new PollCandidate();
            slot.setPollId(poll.getId());
            slot.setCharacterId(null);
            slot.setSlotNumber(i);
            candidateRepository.save(slot);
        }

        log.info("Election created: pollId={}, districtId={}, initiator={}", poll.getId(), districtId, initiatorCharacterId);
        return poll;
    }

    // ===== Кандидаты =====

    /**
     * Занять свободный слот кандидата.
     */
    @Transactional
    public PollCandidate occupySlot(Long pollId, Long characterId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено: " + pollId));
        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalStateException("Голосование не активно");
        }
        if (!poll.isHasCandidates()) {
            throw new IllegalStateException("В этом голосовании нет кандидатов");
        }

        // Проверить, не участвует ли уже
        boolean alreadyIn = candidateRepository.findByPollId(pollId).stream()
                .anyMatch(c -> characterId.equals(c.getCharacterId()));
        if (alreadyIn) {
            throw new IllegalStateException("Персонаж уже является кандидатом");
        }

        // Найти первый свободный слот
        PollCandidate freeSlot = candidateRepository.findByPollId(pollId).stream()
                .filter(c -> c.getCharacterId() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет свободных слотов"));

        freeSlot.setCharacterId(characterId);
        candidateRepository.save(freeSlot);
        log.info("Candidate occupied slot: pollId={}, characterId={}, slot={}", pollId, characterId, freeSlot.getSlotNumber());
        return freeSlot;
    }

    // ===== Голосование =====

    /**
     * Проголосовать за кандидата. Вес голоса = 1 (пока OneVote).
     * Повторное голосование тем же персонажем запрещено.
     */
    @Transactional
    public PollVote castVote(Long pollId, Long characterId, Long candidateId) {
        // Проверить poll
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено: " + pollId));
        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalStateException("Голосование не активно");
        }

        // Проверить кандидата
        PollCandidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Кандидат не найден: " + candidateId));
        if (!candidate.getPollId().equals(pollId)) {
            throw new IllegalArgumentException("Кандидат не относится к этому голосованию");
        }
        if (candidate.getCharacterId() == null) {
            throw new IllegalStateException("Нельзя голосовать за пустой слот");
        }
        if (candidate.getCharacterId().equals(characterId)) {
            throw new IllegalStateException("Нельзя голосовать за себя");
        }

        // Проверить повторное голосование
        Optional<PollVote> existing = voteRepository.findByPollIdAndCharacterId(pollId, characterId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Персонаж уже проголосовал в этом голосовании");
        }

        int weight = 1; // WeightStrategy.ONE_VOTE — пока без XP-множителя

        PollVote vote = new PollVote();
        vote.setPollId(pollId);
        vote.setCharacterId(characterId);
        vote.setCandidateId(candidateId);
        vote.setWeight(weight);
        vote = voteRepository.save(vote);

        log.info("Vote cast: pollId={}, characterId={}, candidateId={}, weight={}", pollId, characterId, candidateId, weight);
        return vote;
    }

    // ===== Закрытие и результаты =====

    /**
     * Подсчитать результаты голосования.
     * Возвращает Map: candidateId → голоса.
     */
    public Map<Long, Integer> countVotes(Long pollId) {
        List<Object[]> rows = voteRepository.countVotesPerCandidate(pollId);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    /**
     * Закрыть голосование вручную (администратор/владелец).
     */
    @Transactional
    public Poll closePoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено: " + pollId));
        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalStateException("Голосование не активно");
        }
        poll.setStatus(PollStatus.CLOSED);
        poll = pollRepository.save(poll);

        // Отложенная проверка закрытия больше не нужна — Poll уже закрыт вручную.
        scheduledCheckService.cancelPending(
                PollCloseCheckHandler.CHECK_TYPE,
                Poll.class.getSimpleName(),
                pollId);

        log.info("Poll closed manually: pollId={}", pollId);
        return poll;
    }

    // ===== Получить данные =====

    public Optional<Poll> findById(Long pollId) {
        return pollRepository.findById(pollId);
    }

    public List<PollCandidate> getCandidates(Long pollId) {
        return candidateRepository.findByPollId(pollId);
    }

    public Poll getWithCandidates(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено: " + pollId));
        List<PollCandidate> candidates = candidateRepository.findByPollId(pollId);
        return poll;
    }

    public List<Poll> findByDistrict(Long districtId) {
        return pollRepository.findByScopeTypeAndScopeId(ScopeType.DISTRICT.name(), districtId);
    }

    public boolean hasVoted(Long pollId, Long characterId) {
        return voteRepository.findByPollIdAndCharacterId(pollId, characterId).isPresent();
    }

    public int totalVoteCount(Long pollId) {
        return voteRepository.sumVoteWeightByPollId(pollId);
    }
}