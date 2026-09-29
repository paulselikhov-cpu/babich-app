package com.platform.chat.engines.poll;

/**
 * Типы отложенных проверок ({@code ScheduledCheck.checkType}) для голосований.
 *
 * <p>Константы принадлежат <b>движку голосований</b>, а не фиче выборов:
 * запись в реестр {@code ScheduledCheck} создаёт {@code PollService} (движок),
 * а обработчик ({@code ElectionClose} в {@code chat.features.governorElection})
 * только подписывается на этот тип. Так зависимость идёт строго
 * {@code feature → engine} и не образуется направленного цикла
 * «движок ↔ фича» (раньше константа жила в фиче, и движок тянул фичу).</p>
 */
public final class PollCheckType {

    /** Закрытие голосования по истечении {@code Poll.endsAt}. */
    public static final String POLL_CLOSE = "POLL_CLOSE";

    private PollCheckType() {
    }
}
