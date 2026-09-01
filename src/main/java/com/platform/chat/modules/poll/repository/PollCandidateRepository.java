package com.platform.chat.modules.poll.repository;

import com.platform.chat.modules.poll.entity.PollCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollCandidateRepository extends JpaRepository<PollCandidate, Long> {
    List<PollCandidate> findByPollId(Long pollId);
}
