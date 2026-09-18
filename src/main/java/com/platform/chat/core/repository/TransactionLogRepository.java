package com.platform.chat.core.repository;

import com.platform.chat.core.entity.TransactionLog;
import com.platform.chat.core.enums.CoinUpdateReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    List<TransactionLog> findByUserIdAndReason(Long userId, CoinUpdateReason reason);
}
