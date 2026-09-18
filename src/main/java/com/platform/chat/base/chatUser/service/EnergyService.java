package com.platform.chat.base.chatUser.service;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Управление энергией персонажа.
 * Энергия восстанавливается пассивно: 1 единица каждые 6 минут (10/час).
 * Максимум — 100. Хранится в БД (без Redis в MVP).
 */
@Service
@RequiredArgsConstructor
public class EnergyService {

    private static final int MAX_ENERGY = 100;
    /** Минут на восстановление 1 единицы энергии. */
    private static final int MINUTES_PER_ENERGY_UNIT = 6;

    private final ChatUserRepository chatUserRepository;

    /**
     * Возвращает актуальную энергию с учётом пассивного восстановления.
     * Не сохраняет в БД — только вычисляет.
     */
    public int getCurrentEnergy(Long userId) {
        ChatUser user = getUser(userId);
        return computeEnergy(user);
    }

    /**
     * Списывает amount единиц энергии.
     * Сначала применяет пассивное восстановление, затем проверяет достаточность.
     */
    @Transactional
    public void consumeEnergy(Long userId, int amount) {
        ChatUser user = getUser(userId);
        int current = computeEnergy(user);
        if (current < amount) {
            throw new IllegalStateException(
                    "Недостаточно энергии. Нужно: " + amount + ", есть: " + current);
        }
        user.setEnergy(current - amount);
        user.setEnergyLastUpdated(LocalDateTime.now());
        chatUserRepository.save(user);
    }

    /**
     * Мгновенно восстанавливает amount единиц (например, от предмета или события).
     */
    @Transactional
    public void restoreEnergy(Long userId, int amount) {
        ChatUser user = getUser(userId);
        int current = computeEnergy(user);
        int restored = Math.min(current + amount, MAX_ENERGY);
        user.setEnergy(restored);
        user.setEnergyLastUpdated(LocalDateTime.now());
        chatUserRepository.save(user);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private int computeEnergy(ChatUser user) {
        if (user.getEnergyLastUpdated() == null) {
            return user.getEnergy();
        }
        long minutesPassed = ChronoUnit.MINUTES.between(
                user.getEnergyLastUpdated(), LocalDateTime.now());
        int recovered = (int) (minutesPassed / MINUTES_PER_ENERGY_UNIT);
        return Math.min(user.getEnergy() + recovered, MAX_ENERGY);
    }

    private ChatUser getUser(Long userId) {
        return chatUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + userId));
    }
}
