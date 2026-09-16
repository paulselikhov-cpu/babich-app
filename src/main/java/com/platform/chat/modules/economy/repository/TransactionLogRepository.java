package com.platform.chat.modules.economy.repository;

import com.platform.chat.modules.economy.entity.TransactionLog;
import com.platform.chat.modules.economy.enums.RewardReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    List<TransactionLog> findByUserIdAndReason(Long userId, RewardReason reason);
}
