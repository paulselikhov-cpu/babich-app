package com.platform.chat.core.modules.poll.repository;

import com.platform.chat.core.modules.poll.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollId(Long pollId);
    long countByPollId(Long pollId);

    Optional<PollVote> findByPollIdAndCharacterId(Long pollId, Long characterId);

    /** Подсчёт голосов по кандидатам: [candidateId, totalWeight]. */
    @Query("SELECT v.candidateId, SUM(v.weight) FROM PollVote v WHERE v.pollId = :pollId GROUP BY v.candidateId")
    List<Object[]> countVotesPerCandidate(@Param("pollId") Long pollId);

    /** Суммарный вес голосов по голосованию. */
    @Query("SELECT COALESCE(SUM(v.weight), 0) FROM PollVote v WHERE v.pollId = :pollId")
    int sumVoteWeightByPollId(@Param("pollId") Long pollId);
}
