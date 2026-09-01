package com.platform.poll.handler;

import com.platform.poll.entity.Poll;
import com.platform.poll.enums.PollType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Обработчик результатов «простых» голосований (Слой 4, Этап 6 миграции событий).
 * <p>
 * Обслуживает типы голосования, у которых пока нет специфических последствий
 * (отзыв, поправка, общественный выбор): результат уже подсчитан
 * {@code ElectionFacade}, специфичных side-effect'ов (назначение должностей,
 * каскад голосований) нет — обработчик фиксирует факт обработки в логе.
 * <p>
 * Это аналог «переименованного» старого PollResultHandler из плана миграции:
 * логика по умолчанию отделена от выборов губернатора, которые теперь
 * обрабатываются {@link ElectionResultHandlerAdapter}.
 */
@Component
public class SimplePollResultHandler implements PollResultHandler {

    private static final Logger log = LoggerFactory.getLogger(SimplePollResultHandler.class);

    private static final Set<PollType> SUPPORTED = EnumSet.of(
            PollType.RECALL,
            PollType.AMENDMENT_VOTE,
            PollType.COMMUNITY_CHOICE
    );

    @Override
    public Set<PollType> supportedTypes() {
        return SUPPORTED;
    }

    @Override
    public void handle(Poll poll) {
        log.info("Simple poll result processed (no side effects yet): pollId={}, pollType={}",
                poll.getId(), poll.getPollType());
    }
}