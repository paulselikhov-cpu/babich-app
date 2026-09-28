package com.platform.economy;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.core.modules.transactionLog.entity.TransactionLog;
import com.platform.chat.core.modules.transactionLog.enums.CoinUpdateReason;
import com.platform.chat.core.modules.transactionLog.repository.TransactionLogRepository;
import com.platform.chat.modules.coin.service.CoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DoD-тесты Этапа 3 (event-driven экономика).
 *
 * Проверяют сквозной путь: CoinService (единственная точка входа изменения
 * монет/XP) → доменное событие (CoinsChangedEvent/XpChangedEvent) →
 * подписчик TransactionLogEventListener → аудит-запись в TransactionLog.
 *
 * <p>Публикация событий в этом проекте идёт через {@code DomainEventPublisher}
 * (обёртка над Spring ApplicationEventPublisher) с доставкой подписчикам
 * {@code @ApplicationModuleListener} через персистентный outbox
 * (event_publication). Spring Modulith 2.x в такой конфигурации не даёт
 * синхронно перехватить событие в тесте через PublishedEvents, а доставка
 * до подписчиков выполняется асинхронно (thread cTaskExecutor-N). Поэтому
 * сквозной путь проверяется по его наблюдаемому эффекту — аудит-записи в
 * {@code TransactionLog}, которую пишет подписчик события на AFTER_COMMIT;
 * появление записи ожидается с таймаутом (см. {@link #awaitAuditLogs}).</p>
 *
 * <p>Четыре сценария прямого вызова {@code CoinService}: {@code giveCoins}
 * (начисление, reason WORK_SHIFT), {@code takeCoins} (списание, reason
 * LOCATION_CREATE + relatedEntityId), {@code takeCoins} при недостатке баланса
 * (IllegalStateException, событие не публикуется — аудита нет) и {@code giveXp}
 * (аудит XP_USER). Баланс/XP проверяются по персонажу (ChatUser.id), аудит —
 * в {@code TransactionLog}.</p>
 */
@SpringBootTest
class RewardServiceAuditIntegrationTest {

    @Autowired
    private CoinService coinService;

    @Autowired
    private ChatUserRepository chatUserRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    /** Стартовый баланс/XP тестового персонажа. */
    private static final long START_BALANCE = 1000L;
    private static final long START_XP = 50L;

    /** Внутренний id персонажа (ChatUser.id). */
    private Long characterId;

    /**
     * Ожидает появления аудит-записи с таймаутом.
     *
     * <p>Событие (CoinsChangedEvent/XpChangedEvent) доставляется подписчику
     * асинхронно через outbox (event_publication), поэтому сразу после вызова
     * CoinService-подписчик запись может ещё не появиться. Хелпер поллит с интервалом
     * 50 мс до 5 с — этого достаточно, чтобы подписчик успел записать аудит.</p>
     */
    private List<TransactionLog> awaitAuditLogs(Long userId, CoinUpdateReason reason) {
        long deadline = System.currentTimeMillis() + 5_000;
        List<TransactionLog> logs = List.of();
        while (System.currentTimeMillis() < deadline) {
            logs = transactionLogRepository.findByUserIdAndReason(userId, reason);
            if (!logs.isEmpty()) {
                return logs;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Прервано ожидание аудит-записи", e);
            }
        }
        return logs;
    }

    @BeforeEach
    void setUp() {
        // id владельца персонажа (User.id) — произвольный, связь с users не
        // смоделирована FK, но уникальность (user_id, district_id) в БД есть:
        // генерим уникальные значения на каждый прогон, т.к. тест не
        // транзакционный и данные коммитятся в dev-БД.
        long unique = System.nanoTime();
        Long userId = 90_000L + unique % 1_000_000L;
        String uniqueName = "AuditTest_" + unique;

        ChatUser user = new ChatUser();
        user.setUserId(userId);
        user.setCharacterName(uniqueName);
        user.setUsername(uniqueName);
        user.setDistrictId(1L);
        user.setCoinBalance(START_BALANCE);
        user.setXpUser(START_XP);
        user.setGameStatus("HOMELESS");
        ChatUser saved = chatUserRepository.save(user);
        characterId = saved.getId();
    }

    @Test
    @DisplayName("giveCoins: начисление монет + доменное событие CoinsChangedEvent + аудит COIN")
    void giveCoins_publishesEvent_andWritesAudit() {
        long amount = 250L;

        coinService.giveCoins(characterId, amount, CoinUpdateReason.WORK_SHIFT, null);

        ChatUser user = chatUserRepository.findById(characterId).orElseThrow();
        assertThat(user.getCoinBalance()).isEqualTo(START_BALANCE + amount);

        // аудит пишет подписчик CoinsChangedEvent (@ApplicationModuleListener) —
        // наличие записи подтверждает сквозной путь: сервис → событие → подписчик → аудит
        List<TransactionLog> logs = awaitAuditLogs(characterId, CoinUpdateReason.WORK_SHIFT);
        assertThat(logs)
                .anySatisfy(log -> {
                    assertThat(log.getResourceType()).isEqualTo("COIN");
                    assertThat(log.getAmount()).isEqualTo(amount);
                    assertThat(log.getReason()).isEqualTo(CoinUpdateReason.WORK_SHIFT);
                });
    }

    @Test
    @DisplayName("takeCoins: списание монет + доменное событие CoinsChangedEvent с отрицательным amount + аудит COIN")
    void takeCoins_publishesEventWithNegativeAmount_andWritesAudit() {
        long amount = 100L;

        coinService.takeCoins(characterId, amount, CoinUpdateReason.LOCATION_CREATE, 777L);

        ChatUser user = chatUserRepository.findById(characterId).orElseThrow();
        assertThat(user.getCoinBalance()).isEqualTo(START_BALANCE - amount);

        // аудит списания: amount отрицательный, связанная сущность сохраняется
        List<TransactionLog> logs = awaitAuditLogs(characterId, CoinUpdateReason.LOCATION_CREATE);
        assertThat(logs)
                .anySatisfy(log -> {
                    assertThat(log.getResourceType()).isEqualTo("COIN");
                    assertThat(log.getAmount()).isEqualTo(-amount);
                    assertThat(log.getReason()).isEqualTo(CoinUpdateReason.LOCATION_CREATE);
                    assertThat(log.getRelatedEntityId()).isEqualTo(777L);
                });
    }

    @Test
    @DisplayName("takeCoins: при недостатке баланса — исключение, баланс и аудит не меняются")
    void takeCoins_withoutEnoughBalance_throws_andWritesNoAudit() {
        long overdraw = START_BALANCE + 1;

        assertThatThrownBy(() -> coinService.takeCoins(
                characterId, overdraw, CoinUpdateReason.LOCATION_CREATE, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Недостаточно монет");

        ChatUser user = chatUserRepository.findById(characterId).orElseThrow();
        assertThat(user.getCoinBalance()).isEqualTo(START_BALANCE);

        // событие не публиковалось — аудит-запись не должна появиться
        assertThat(transactionLogRepository.findByUserIdAndReason(
                characterId, CoinUpdateReason.LOCATION_CREATE)).isEmpty();
    }

    @Test
    @DisplayName("giveXp: начисление XP + доменное событие XpChangedEvent + аудит XP_USER")
    void giveXp_publishesEvent_andWritesAudit() {
        long amount = 40L;

        coinService.giveXp(characterId, amount, CoinUpdateReason.WORK_SHIFT);

        ChatUser user = chatUserRepository.findById(characterId).orElseThrow();
        assertThat(user.getXpUser()).isEqualTo(START_XP + amount);

        // аудит XP_USER пишет подписчик XpChangedEvent — сквозной путь подтверждён
        List<TransactionLog> logs = awaitAuditLogs(characterId, CoinUpdateReason.WORK_SHIFT);
        assertThat(logs)
                .anySatisfy(log -> {
                    assertThat(log.getResourceType()).isEqualTo("XP_USER");
                    assertThat(log.getAmount()).isEqualTo(amount);
                    assertThat(log.getReason()).isEqualTo(CoinUpdateReason.WORK_SHIFT);
                });
    }
}