package com.platform.chat.modules.coin.service;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.modules.domainEvent.event.CoinsChangedEvent;
import com.platform.chat.core.modules.domainEvent.base.DomainEventPublisher;
import com.platform.chat.modules.domainEvent.event.XpChangedEvent;
import com.platform.chat.modules.coin.enums.CoinUpdateReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Единственная точка входа для всех изменений монет и XP.
 * Все начисления/списания ТОЛЬКО через этот сервис — это гарантирует
 * что каждая операция публикует доменное событие (CoinsChangedEvent/
 * XpChangedEvent), а аудит-запись в TransactionLog пишет подписчик
 * (TransactionLogEventListener) после коммита транзакции.
 */
@Service
@RequiredArgsConstructor
public class CoinService {

    private final ChatUserRepository chatUserRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public void giveCoins(Long userId, long amount, CoinUpdateReason reason, Long relatedEntityId) {
        ChatUser user = getUser(userId);
        user.setCoinBalance(user.getCoinBalance() + amount);
        chatUserRepository.save(user);
        eventPublisher.publish(
                new CoinsChangedEvent(userId, amount, reason, relatedEntityId));
    }

    @Transactional
    public void takeCoins(Long userId, long amount, CoinUpdateReason reason, Long relatedEntityId) {
        ChatUser user = getUser(userId);
        if (user.getCoinBalance() < amount) {
            throw new IllegalStateException(
                    "Недостаточно монет. Нужно: " + amount + ", есть: " + user.getCoinBalance());
        }
        user.setCoinBalance(user.getCoinBalance() - amount);
        chatUserRepository.save(user);
        eventPublisher.publish(
                new CoinsChangedEvent(userId, -amount, reason, relatedEntityId));
    }

    @Transactional
    public void giveXp(Long userId, long amount, CoinUpdateReason reason) {
        ChatUser user = getUser(userId);
        user.setXpUser(user.getXpUser() + amount);
        chatUserRepository.save(user);
        eventPublisher.publish(new XpChangedEvent(userId, amount, reason, null));
    }

    private ChatUser getUser(Long userId) {
        return chatUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + userId));
    }
}