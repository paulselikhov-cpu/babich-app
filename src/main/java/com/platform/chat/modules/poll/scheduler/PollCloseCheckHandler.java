package com.platform.poll.scheduler;

import com.platform.core.scheduler.ScheduledCheck;
import com.platform.core.scheduler.ScheduledCheckHandler;
import com.platform.poll.service.ElectionFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Слой 4 (автоматизация). Обработчик отложенных проверок «закрыть Poll».
 *
 * закрытие выборов по истечении {@code endsAt} инициируется явной записью в реестре {@link ScheduledCheck}
 * (checkType = {@value #CHECK_TYPE}), которую регистрирует PollService при
 * создании Poll. Когда {@code dueAt} наступает, {@code SchedulerGateway}
 * находит проверку одним индексным запросом и вызывает {@link #handle}.</p>
 *
 * <p>Делегирует в {@link ElectionFacade#closeElection(Long)} — тот же путь,
 * что и ручное закрытие: closePoll → подсчёт голосов → ElectionResultHandlerAdapter
 * (найден PollResultDispatcher по типу голосования) → публикация
 * ElectionClosedEvent (уведомления жителям района уходят через
 * NotificationEventListener, Слой 4).</p>
 */
@Component
@RequiredArgsConstructor
public class PollCloseCheckHandler implements ScheduledCheckHandler {

    public static final String CHECK_TYPE = "POLL_CLOSE";

    private static final Logger log = LoggerFactory.getLogger(PollCloseCheckHandler.class);

    private final ElectionFacade electionFacade;

    @Override
    public String checkType() {
        return CHECK_TYPE;
    }

    @Override
    public void handle(ScheduledCheck check) {
        log.info("Scheduled check: closing poll entityId={} (check id={})",
                check.getEntityId(), check.getId());
        electionFacade.closeElection(check.getEntityId());
    }
}