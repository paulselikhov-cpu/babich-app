package com.platform.chat.modules.poll.entity;

import com.platform.chat.modules.poll.enums.CampaignType;
import com.platform.chat.modules.poll.enums.ScopeType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Кампания / сбор подписей для инициирования ивента.
 * Например, сбор 70% подписей живых участников района для запуска
 * перевыборов губернатора (RECALL_MAYOR).
 * Слой 3 (Процессы).
 */
@Data
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false, length = 30)
    private CampaignType campaignType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "initiated_by_character_id", nullable = false)
    private Long initiatedByCharacterId;

    /** Сколько подписей нужно собрать для успеха. */
    @Column(name = "goal_threshold", nullable = false)
    private int goalThreshold;

    /** Сколько подписей уже собрано. */
    @Column(name = "current_count", nullable = false)
    private int currentCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @PrePersist
    public void prePersist() {
        this.startedAt = Instant.now();
    }
}