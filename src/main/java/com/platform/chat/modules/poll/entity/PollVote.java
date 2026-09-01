package com.platform.poll.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

/**
 * Голос участника в голосовании.
 */
@Data
@Entity
@Table(name = "poll_votes")
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "weight", nullable = false)
    private int weight = 1;

    @Column(name = "voted_at", nullable = false)
    private Instant votedAt;

    @PrePersist
    public void prePersist() {
        this.votedAt = Instant.now();
    }
}