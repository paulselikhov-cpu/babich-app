package com.platform.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Настройки района (редактируются админом через панель настроек).
 * <p>
 * Значения являются «активными» — не имеют отдельного уровня дефолтов в конфиге:
 * если админ ещё ничего не менял, поля содержат значения по умолчанию,
 * которые проставляются при создании строки.
 * </p>
 * <p>Группы: election_* — выборы губернатора, recall_* — обжалование/переизбрание
 * (запланировано). Recall-поля уже доступны админке, начнут использоваться
 * бэкендом, когда появится PollType.RECALL.</p>
 */
@Data
@Entity
@Table(name = "district_settings")
public class DistrictSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false, unique = true)
    private District district;

    // === Выборы губернатора ===
    @Column(name = "election_duration_hours", nullable = false)
    private int electionDurationHours = 24;

    @Column(name = "election_alive_window_days", nullable = false)
    private int electionAliveWindowDays = 2;

    @Column(name = "election_quorum_percent", nullable = false)
    private int electionQuorumPercent = 50;

    @Column(name = "election_max_candidates", nullable = false)
    private int electionMaxCandidates = 5;

    // === Обжалование губернатора / переизбрание (запланировано) ===
    @Column(name = "recall_duration_hours", nullable = false)
    private int recallDurationHours = 24;

    @Column(name = "recall_alive_window_days", nullable = false)
    private int recallAliveWindowDays = 2;

    @Column(name = "recall_quorum_percent", nullable = false)
    private int recallQuorumPercent = 50;
}
