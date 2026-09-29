package com.platform.chat.engines.transactionLog.entity;

import com.platform.chat.engines.transactionLog.enums.CoinUpdateReason;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Лог всех начислений/списаний монет и XP.
 * Пишется исключительно через CoinService — нужен для анти-фрод
 * анализа и постфактум-балансировки экономики (см. babichchat_concept.md, раздел 9).
 */
@Data
@Entity
@Table(name = "transaction_logs")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Тип ресурса, который изменился: COIN или XP_USER (пока только эти
     * два в MVP, XP_GANG добавится с группировками).
     */
    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    /**
     * Величина изменения. Положительная — начисление, отрицательная —
     * списание. Хранится как есть, без отдельного flag "credit/debit".
     */
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CoinUpdateReason reason;

    /**
     * Опциональная ссылка на связанную сущность (например id заявки,
     * id депозита, id попытки кражи) — для трассировки.
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
