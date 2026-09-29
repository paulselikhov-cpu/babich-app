package com.platform.chat.engines.poll.entity;

import com.platform.chat.engines.poll.enums.WeightStrategy;
import com.platform.chat.engines.poll.enums.PollStatus;
import com.platform.chat.engines.poll.enums.PollType;
import com.platform.chat.engines.poll.enums.ScopeType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

/**
 * Голосование — коллективный процесс выбора одного из вариантов голосами участников.
 * 
 */
@Data
@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "poll_type", nullable = false, length = 30)
    private PollType pollType;

    @Column(name = "has_candidates", nullable = false)
    private boolean hasCandidates;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_strategy", nullable = false, length = 30)
    private WeightStrategy weightStrategy;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "quorum_threshold", nullable = false)
    private int quorumThreshold = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PollStatus status = PollStatus.PENDING;

    @Column(name = "parent_poll_id")
    private Long parentPollId;

    @Column(name = "source_application_id")
    private Long sourceApplicationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}