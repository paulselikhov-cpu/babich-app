package com.platform.chat.features.governorElection.handler;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.enums.SystemRoleType;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import com.platform.chat.domain.location.entity.LocationUser;
import com.platform.chat.domain.location.enums.LocationUserRole;
import com.platform.chat.domain.location.enums.SystemLocationCode;
import com.platform.chat.domain.location.repository.LocationUserRepository;
import com.platform.chat.domain.location.repository.LocationRepository;

import com.platform.chat.engines.poll.entity.Poll;
import com.platform.chat.engines.poll.entity.PollCandidate;
import com.platform.chat.engines.poll.entity.PollVote;
import com.platform.chat.engines.poll.enums.PollStatus;
import com.platform.chat.engines.poll.enums.PollType;
import com.platform.chat.engines.poll.handler.PollResultHandler;
import com.platform.chat.engines.poll.repository.PollCandidateRepository;
import com.platform.chat.engines.poll.repository.PollRepository;
import com.platform.chat.engines.poll.repository.PollVoteRepository;
import com.platform.chat.engines.poll.service.PollResultDispatcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Обработчик результатов выборов губернатора.
 * <p>
 * Подсчитывает голоса по кандидатам и назначает победителя на единоличную
 * должность (GOVERNOR) в мэрии района. Зарегистрирован как
 * {@link PollResultHandler} для
 * {@link PollType#ELECTION} — {@link PollResultDispatcher} находит его по типу
 * голосования, {@code ElectionService} больше не знает про конкретного
 * обработчика.
 */
@Component
@RequiredArgsConstructor
public class ElectionResult implements PollResultHandler {

    private static final Set<PollType> SUPPORTED = EnumSet.of(PollType.ELECTION);

    private final PollRepository pollRepository;
    private final PollCandidateRepository pollCandidateRepository;
    private final PollVoteRepository pollVoteRepository;
    private final LocationRepository locationRepository;
    private final LocationUserRepository locationUserRepository;
    private final ChatUserRepository chatUserRepository;

    @Override
    public Set<PollType> supportedTypes() {
        return SUPPORTED;
    }

    @Override
    @Transactional
    public void handle(Poll poll) {
        List<PollCandidate> candidates = pollCandidateRepository.findByPollId(poll.getId());
        List<PollVote> votes = pollVoteRepository.findByPollId(poll.getId());

        // Подсчёт голосов
        for (PollCandidate candidate : candidates) {
            long count = votes.stream()
                    .filter(v -> v.getCandidateId().equals(candidate.getId()))
                    .count();
            candidate.setVoteCount(count);
        }
        pollCandidateRepository.saveAll(candidates);

        // Победитель — кандидат с максимальным числом голосов
        PollCandidate winner = candidates.stream()
                .max((a, b) -> Long.compare(a.getVoteCount(), b.getVoteCount()))
                .orElse(null);

        if (winner != null && winner.getCharacterId() != null) {
            String scopeType = poll.getScopeType().name();
            Long scopeId = poll.getScopeId();

            Optional<ChatUser> winnerCharacter = chatUserRepository.findById(winner.getCharacterId());

            // Выборы губернатора назначают победителя ТОЛЬКО на должность GOVERNOR в мэрии района.
            if (winnerCharacter.isPresent() && "DISTRICT".equals(scopeType)) {
                ChatUser newMayor = winnerCharacter.get();
                ChatUser finalNewMayor = newMayor;
                locationRepository
                        .findByDistrictIdAndCode(scopeId, SystemLocationCode.CITY_HALL)
                        .ifPresent(cityHall -> {
                            // 1. снять действующего губернатора (в системной локации membership == должность)
                            locationUserRepository
                                    .findByLocationIdAndSystemRole(cityHall.getId(), SystemRoleType.GOVERNOR)
                                    .ifPresent(locationUserRepository::delete);
                            // 2. назначить нового мэра: GOVERNOR + MODERATOR
                            LocationUser member = locationUserRepository
                                    .findByLocationIdAndCharacterId(cityHall.getId(), finalNewMayor.getId())
                                    .orElseGet(LocationUser::new);
                            member.setLocation(cityHall);
                            member.setCharacter(finalNewMayor);
                            member.setSystemRole(SystemRoleType.GOVERNOR);
                            member.setAppointedAt(LocalDateTime.now());
                            member.setRole(LocationUserRole.MODERATOR);
                            if (member.getIsRegistered() == null) member.setIsRegistered(true);
                            locationUserRepository.save(member);
                        });
            }
        }

        poll.setStatus(PollStatus.CLOSED);
        pollRepository.save(poll);
    }
}
