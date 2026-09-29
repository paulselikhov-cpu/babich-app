package com.platform.chat.engines.poll.repository;

import com.platform.chat.engines.poll.entity.Poll;
import com.platform.chat.engines.poll.enums.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByScopeTypeAndScopeId(ScopeType scopeType, Long scopeId);
}
