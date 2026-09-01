package com.platform.chat.modules.poll.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Кандидат в голосовании (для Poll с hasCandidates=true).
 * Слот может быть свободным (characterId = null) — тогда пользователь может
 * занять его через "добавить свою кандидатуру".
 */
@Data
@Entity
@Table(name = "poll_candidates")
public class PollCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "character_id")
    private Long characterId;

    @Column(name = "slot_number", nullable = false)
    private int slotNumber;

    /** Не маппится в БД — вычисляется в Early объекте запросом count голосов */
    @Transient
    private long voteCount;
}
