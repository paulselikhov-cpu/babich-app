package com.platform.chat.modules.poll.repository;

import com.platform.chat.modules.poll.entity.Poll;
import com.platform.chat.modules.poll.enums.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByScopeTypeAndScopeId(ScopeType scopeType, Long scopeId);
}
