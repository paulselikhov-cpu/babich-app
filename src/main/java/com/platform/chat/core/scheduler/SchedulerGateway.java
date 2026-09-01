package com.platform.chat.core.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Единый планировщик (SchedulerGateway).
 *
 * <p>Единственный {@code @Scheduled}-метод в приложении. Вместо того чтобы
 * каждый сервис плодил свою {@code @Scheduled}-аннотацию, фоновые задачи:
 * <ul>
 *   <li>реализуют {@link TickHandler} — периодическая работа (например,
 *       очистка протухших presence-сессий); Spring собирает
 *       {@code List<TickHandler>} автоматически;</li>
 *   <li>регистрируют отложенную проверку в реестре {@link ScheduledCheck}
 *       (например, «закрыть Poll в 14:00») — gateway находит просроченные
 *       проверки одним индексным запросом и диспетчеризует их
 *       {@link ScheduledCheckHandler} по {@code checkType}.</li>
 * </ul>
 * </p>
 *
 * <p>Период мастер-тика — 15 секунд: он покрывает самую быструю фоновую
 * задачу (presence-cleanup, 15 сек) целым числом тиков и даёт точность
 * доставки отложенных проверок ≤15 сек. Обработчики с большим интервалом
 * (60_000 по умолчанию) срабатывают через каждые 4 тика.</p>
 *
 * <p>{@code fixedDelay}, а не {@code fixedRate}: fixedRate планирует
 * следующий запуск от начала предыдущего и при скачке системных часов
 * (NTP/clock leap) компенсирует «накопившиеся» запуски лавиной вызовов
 * (см. scheduler-clock-leap-fixed-rate-avalanche.md).</p>
 */
@Component
@RequiredArgsConstructor
public class SchedulerGateway {

    private static final Logger log = LoggerFactory.getLogger(SchedulerGateway.class);

    /**
     * Период мастер-тика. 15 секунд — НОД интервалов фоновых задач проекта
     * (presence-cleanup 15_000, выборы/дефолт 60_000), поэтому каждая задача
     * срабатывает с целым числом тиков без «догоняющих» кратных вызовов.
     */
    public static final long MASTER_TICK_MILLIS = 15_000L;

    private final List<TickHandler> tickHandlers;
    private final List<ScheduledCheckHandler> checkHandlers;
    private final ScheduledCheckRepository checkRepository;
    private final ScheduledCheckService checkService;

    /** Время последнего запуска каждого TickHandler (ключ — имя класса). */
    private final Map<String, Long> lastTickMillis = new ConcurrentHashMap<>();

    /**
     * Единственный планируемый метод приложения: мастер-тик.
     */
    @Scheduled(fixedDelay = MASTER_TICK_MILLIS)
    public void onTick() {
        long now = System.currentTimeMillis();
        runDueTickHandlers(now);
        dispatchDueChecks();
    }

    private void runDueTickHandlers(long now) {
        for (TickHandler handler : tickHandlers) {
            String key = handler.getClass().getName();
            long last = lastTickMillis.getOrDefault(key, 0L);
            if (now - last < handler.intervalMillis()) {
                continue;
            }
            try {
                handler.onTick();
                lastTickMillis.put(key, now);
            } catch (Exception e) {
                // lastTickMillis не обновляем — задача будет перепробована на следующем тике
                log.error("Tick handler '{}' failed: {}",
                        handler.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    private void dispatchDueChecks() {
        Instant now = Instant.now();
        List<ScheduledCheck> due = checkRepository
                .findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(ScheduledCheckStatus.PENDING, now);
        if (due.isEmpty()) {
            return;
        }

        Map<String, ScheduledCheckHandler> handlersByType = checkHandlers.stream()
                .collect(Collectors.toMap(ScheduledCheckHandler::checkType, Function.identity()));

        for (ScheduledCheck check : due) {
            ScheduledCheckHandler handler = handlersByType.get(check.getCheckType());
            if (handler == null) {
                // обработчик не зарегистрирован (например, удалённый модуль) —
                // закрываем проверку, чтобы реестр не засорялся и не крутился вечно
                log.warn("No handler for scheduled check type '{}', closing check id={}",
                        check.getCheckType(), check.getId());
                checkService.markDone(check.getId());
                continue;
            }
            try {
                handler.handle(check);
                checkService.markDone(check.getId());
            } catch (Exception e) {
                // проверку не закрываем — будет перепробована на следующем тике
                log.error("Scheduled check handler '{}' failed for check id={}: {}",
                        check.getCheckType(), check.getId(), e.getMessage(), e);
            }
        }
    }
}