package com.platform.chat.modules.poll.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Подпись (голос) за кампанию — персонаж подтверждает, что поддерживает ивент.
 * Слой 3 (Процессы).
 */
@Data
@Entity
@Table(name = "campaign_contributions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"campaign_id", "character_id"})
        })
public class CampaignContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @PrePersist
    public void prePersist() {
        this.signedAt = Instant.now();
    }
}