package com.platform.chat.core.eventListener;

import com.platform.chat.core.repository.TransactionLogRepository;
import com.platform.chat.core.event.childs.CoinsChangedEvent;
import com.platform.chat.core.event.childs.XpChangedEvent;
import com.platform.chat.core.entity.TransactionLog;
import com.platform.chat.core.enums.CoinUpdateReason;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Подписчик на события изменения баланса/XP.
 * Пишет аудит-запись в TransactionLog.
 *
 * <p>Выполняется на фазе AFTER_COMMIT (событие публикуется издателем в той же
 * транзакции, что и бизнес-изменение) в отдельной транзакции REQUIRES_NEW:
 * аудит пишется в БД независимо от успеха исходной транзакции, а ошибка аудита
 * не откатывает бизнес-операцию.</p>
 *
 * <p>Amount приходит из события уже с нужным знаком: начисление — положительное,
 * списание (takeCoins) — отрицательное. Почему аудит не пишется синхронно в
 * RewardService — см. event/event-migration-plan.md, Этап 3.</p>
 *
 * <p>@ApplicationModuleListener — Modulith-обёртка над AFTER_COMMIT
 * (TransactionalEventApplicationListener), но с outbox-гарантиями: событие
 * пишется в {@code event_publication} в транзакции публикатора и может быть
 * доставлено повторно после рестарта, если процесс упал между коммитом и
 * обработкой. REQUIRES_NEW разрешён на @ApplicationModuleListener — аудит
 * фиксируется своей транзакцией независимо от бизнес-операции.</p>
 */
@Component
@RequiredArgsConstructor
public class TransactionLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionLogEventListener.class);

    private final TransactionLogRepository transactionLogRepository;

    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCoinsChanged(CoinsChangedEvent event) {
        write(event.getCharacterId(), "COIN", event.getAmount(),
                event.getReason(), event.getRelatedEntityId());
        log.debug("TransactionLog COIN recorded: userId={}, amount={}, reason={}",
                event.getCharacterId(), event.getAmount(), event.getReason());
    }

    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onXpChanged(XpChangedEvent event) {
        write(event.getCharacterId(), "XP_USER", event.getAmount(),
                event.getReason(), event.getRelatedEntityId());
        log.debug("TransactionLog XP_USER recorded: userId={}, amount={}, reason={}",
                event.getCharacterId(), event.getAmount(), event.getReason());
    }

    private void write(Long userId, String resourceType, long amount,
                       CoinUpdateReason reason, Long relatedEntityId) {
        TransactionLog entry = new TransactionLog();
        entry.setUserId(userId);
        entry.setResourceType(resourceType);
        entry.setAmount(amount);
        entry.setReason(reason);
        entry.setRelatedEntityId(relatedEntityId);
        transactionLogRepository.save(entry);
    }
}