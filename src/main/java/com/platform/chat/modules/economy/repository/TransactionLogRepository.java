package com.platform.economy.repository;

import com.platform.economy.entity.TransactionLog;
import com.platform.economy.enums.RewardReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TransactionLog> findByUserIdAndReason(Long userId, RewardReason reason);
}
