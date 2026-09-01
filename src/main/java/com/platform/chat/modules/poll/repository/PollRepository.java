package com.platform.poll.repository;

import com.platform.poll.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByScopeTypeAndScopeId(String scopeType, Long scopeId);
}
