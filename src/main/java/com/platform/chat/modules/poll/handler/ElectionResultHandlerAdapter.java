package com.platform.chat.modules.poll.handler;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.Location;
import com.platform.chat.entity.LocationPost;
import com.platform.chat.enums.LocationPostType;
import com.platform.chat.enums.PublicLocationType;
import com.platform.chat.modules.poll.entity.Poll;
import com.platform.chat.modules.poll.entity.PollCandidate;
import com.platform.chat.modules.poll.entity.PollVote;
import com.platform.chat.modules.poll.enums.PollStatus;
import com.platform.chat.modules.poll.enums.PollType;
import com.platform.chat.modules.poll.repository.PollCandidateRepository;
import com.platform.chat.modules.poll.repository.PollRepository;
import com.platform.chat.modules.poll.repository.PollVoteRepository;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationPostRepository;
import com.platform.chat.repository.location.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Обработчик результатов выборов губернатора (Слой 4, Этап 6 миграции событий).
 * <p>
 * Адаптер над прежней логикой закрытия выборов: подсчитывает голоса по
 * кандидатам и назначает победителя на единоличную должность (GOVERNOR) в
 * системных локациях района. Зарегистрирован как {@link PollResultHandler} для
 * {@link PollType#ELECTION} — {@link PollResultDispatcher} находит его по типу
 * голосования, {@code ElectionFacade} больше не знает про конкретного
 * обработчика.
 */
@Component
@RequiredArgsConstructor
public class ElectionResultHandlerAdapter implements PollResultHandler {

    private static final Set<PollType> SUPPORTED = EnumSet.of(PollType.ELECTION);

    private final PollRepository pollRepository;
    private final PollCandidateRepository pollCandidateRepository;
    private final PollVoteRepository pollVoteRepository;
    private final LocationRepository locationRepository;
    private final LocationPostRepository locationPostRepository;
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

            if (winnerCharacter.isPresent() && "DISTRICT".equals(scopeType)) {
                List<Location> locations = locationRepository.findByDistrictIdAndIsSystemTrue(scopeId);
                for (Location loc : locations) {
                    if (loc.getType() != null) {
                        LocationPostType postType = mapPublicLocationTypeToPost(loc.getType());
                        if (postType != null) {
                            Optional<LocationPost> existing = locationPostRepository
                                    .findByLocationIdAndPost(loc.getId(), postType);
                            if (existing.isEmpty()) {
                                LocationPost locationPost = new LocationPost();
                                locationPost.setLocation(loc);
                                locationPost.setCharacter(winnerCharacter.get());
                                locationPost.setPost(postType);
                                locationPost.setAppointedAt(LocalDateTime.now());
                                locationPostRepository.save(locationPost);
                            }
                        }
                    }
                }
            }
        }

        poll.setStatus(PollStatus.CLOSED);
        pollRepository.save(poll);
    }

    private LocationPostType mapPublicLocationTypeToPost(PublicLocationType type) {
        switch (type) {
            case CITY_HALL: return LocationPostType.GOVERNOR;
            case POLICE_STATION: return LocationPostType.POLICE_OFFICER;
            case BANK: return LocationPostType.BANK_DIRECTOR;
            case REAL_ESTATE_MARKET: return LocationPostType.REAL_ESTATE_DIRECTOR;
            default: return null;
        }
    }
}
