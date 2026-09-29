package com.platform.chat.features.governorElection.sheduledCheck;

import com.platform.chat.engines.poll.PollCheckType;
import com.platform.chat.engines.scheduledCheck.entity.ScheduledCheck;
import com.platform.chat.engines.scheduledCheck.models.ScheduledCheckHandler;
import com.platform.chat.features.governorElection.service.ElectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Обработчик отложенных проверок «закрыть Poll».
 *
 * закрытие выборов по истечении {@code endsAt} инициируется явной записью в реестре {@link ScheduledCheck}
 * (checkType = {@value #CHECK_TYPE}), которую регистрирует PollService при
 * создании Poll. Когда {@code dueAt} наступает, {@code SchedulerGateway}
 * находит проверку одним индексным запросом и вызывает {@link #handle}.</p>
 *
 * <p>Делегирует в {@link ElectionService#closeElection(Long)} — тот же путь,
 * что и ручное закрытие: closePoll → подсчёт голосов → ElectionResultHandlerAdapter
 * (найден PollResultDispatcher по типу голосования) → публикация
 * ElectionClosedEvent (уведомления жителям района уходят через
 * NotificationEventListener).</p>
 */
@Component
@RequiredArgsConstructor
public class ElectionClose implements ScheduledCheckHandler {

    /**
     * Тип проверки — константа объявлена в движке голосований
     * ({@link PollCheckType#POLL_CLOSE}), чтобы {@code PollService} не зависел
     * от этого фич-слайса (зависимость строго feature → engine).
     */
    public static final String CHECK_TYPE = PollCheckType.POLL_CLOSE;

    private static final Logger log = LoggerFactory.getLogger(ElectionClose.class);

    private final ElectionService electionService;

    @Override
    public String checkType() {
        return CHECK_TYPE;
    }

    @Override
    public void handle(ScheduledCheck check) {
        log.info("Scheduled check: closing poll entityId={} (check id={})",
                check.getEntityId(), check.getId());
        electionService.closeElection(check.getEntityId());
    }
}