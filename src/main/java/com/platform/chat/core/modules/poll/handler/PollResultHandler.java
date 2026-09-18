package com.platform.chat.core.modules.poll.handler;

import com.platform.chat.core.modules.poll.entity.Poll;
import com.platform.chat.core.modules.poll.enums.PollType;
import com.platform.chat.core.modules.poll.service.PollResultDispatcher;

import java.util.Set;

/**
 * Обработчик результата голосования (Слой 4).
 * <p>
 * Каждая реализация отвечает за свой набор типов голосования
 * ({@link #supportedTypes()}) и выполняет последствия закрытия голосования:
 * подсчёт голосов, назначение должностей, каскад голосований и т.п.
 * Реализации собираются Spring'ом в {@code List<PollResultHandler>} и
 * диспетчеризуются через {@link PollResultDispatcher}.
 */
public interface PollResultHandler {

    /**
     * Типы голосования, которые обрабатывает эта реализация.
     * Диспетчер выбирает обработчик по {@code poll.getPollType()}.
     */
    Set<PollType> supportedTypes();

    /**
     * Обработать результат закрытого голосования.
     * Вызывается только для типа из {@link #supportedTypes()}.
     */
    void handle(Poll poll);
}