package com.platform.economy.service;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.economy.entity.TransactionLog;
import com.platform.economy.enums.RewardReason;
import com.platform.economy.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Единственная точка входа для всех изменений монет и XP.
 * Все начисления/списания ТОЛЬКО через этот сервис — это гарантирует
 * что каждая операция попадает в TransactionLog для анти-фрод анализа.
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    private final ChatUserRepository chatUserRepository;
    private final TransactionLogRepository transactionLogRepository;

    @Transactional
    public void giveCoins(Long userId, long amount, RewardReason reason, Long relatedEntityId) {
        ChatUser user = getUser(userId);
        user.setCoinBalance(user.getCoinBalance() + amount);
        chatUserRepository.save(user);
        log(userId, "COIN", amount, reason, relatedEntityId);
    }

    @Transactional
    public void takeCoins(Long userId, long amount, RewardReason reason, Long relatedEntityId) {
        ChatUser user = getUser(userId);
        if (user.getCoinBalance() < amount) {
            throw new IllegalStateException(
                    "Недостаточно монет. Нужно: " + amount + ", есть: " + user.getCoinBalance());
        }
        user.setCoinBalance(user.getCoinBalance() - amount);
        chatUserRepository.save(user);
        log(userId, "COIN", -amount, reason, relatedEntityId);
    }

    @Transactional
    public void giveXp(Long userId, long amount, RewardReason reason) {
        ChatUser user = getUser(userId);
        user.setXpUser(user.getXpUser() + amount);
        chatUserRepository.save(user);
        log(userId, "XP_USER", amount, reason, null);
    }

    private ChatUser getUser(Long userId) {
        return chatUserRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + userId));
    }

    private void log(Long userId, String resourceType, long amount,
                     RewardReason reason, Long relatedEntityId) {
        TransactionLog entry = new TransactionLog();
        entry.setUserId(userId);
        entry.setResourceType(resourceType);
        entry.setAmount(amount);
        entry.setReason(reason);
        entry.setRelatedEntityId(relatedEntityId);
        transactionLogRepository.save(entry);
    }
}
