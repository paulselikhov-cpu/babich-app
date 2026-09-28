package com.platform.chat.core.modules.poll.service;

import com.platform.chat.core.modules.poll.entity.Poll;
import com.platform.chat.core.modules.poll.enums.PollType;
import com.platform.chat.core.modules.poll.handler.PollResultHandler;
import com.platform.chat.core.modules.domainEvent.service.DomainEventPublisher;
import com.platform.chat.core.modules.domainEvent.events.ElectionClosedEvent;
import com.platform.chat.core.modules.domainEvent.events.PollClosedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Диспетчер обработчиков результатов голосования (Слой 4).
 * <p>
 * Spring собирает все реализации {@link PollResultHandler} в список и передаёт
 * сюда. Диспетчер строит карту {@code PollType → PollResultHandler} и выбирает
 * обработчик по {@link Poll#getPollType()}: так последствия закрытия голосования
 * отделены от сценария ({@code ElectionService} больше не знает про конкретного
 * обработчика) и легко добавляется новый тип голосования — новой реализацией
 * интерфейса, без правки фасада.
 * <p>
 * После обработки результата диспетчер публикует {@link PollClosedEvent} — это
 * общее событие «голосование закрыто и обработано» для любого типа голосования.
 * Для выборов губернатора {@code ElectionService} дополнительно публикует
 * {@link ElectionClosedEvent} с именем победителя — оба
 * события независимы, подписчики выбирают нужное.
 */
@Service
@RequiredArgsConstructor
public class PollResultDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PollResultDispatcher.class);

    private final List<PollResultHandler> handlers;
    private final DomainEventPublisher eventPublisher;

    private final Map<PollType, PollResultHandler> handlerByType = new EnumMap<>(PollType.class);

    @PostConstruct
    void buildHandlerMap() {
        for (PollResultHandler handler : handlers) {
            for (PollType type : handler.supportedTypes()) {
                PollResultHandler previous = handlerByType.put(type, handler);
                if (previous != null) {
                    throw new IllegalStateException(
                            "Дублирующийся PollResultHandler для типа " + type + ": "
                                    + previous.getClass().getSimpleName() + " и "
                                    + handler.getClass().getSimpleName());
                }
            }
        }
        log.info("PollResultDispatcher готов: типов голосования = {}", handlerByType.size());
    }

    /**
     * Обработать результат закрытого голосования и опубликовать {@link PollClosedEvent}.
     * Вызывается из единой точки закрытия голосования ({@code ElectionService.closeElection}).
     */
    @Transactional
    public void dispatch(Poll poll) {
        PollType type = poll.getPollType();
        PollResultHandler handler = handlerByType.get(type);

        if (handler == null) {
            log.warn("Нет PollResultHandler для типа {}, обработка результата пропущена. pollId={}",
                    type, poll.getId());
        } else {
            log.info("PollResultDispatcher: тип={}, обработчик={}, pollId={}",
                    type, handler.getClass().getSimpleName(), poll.getId());
            handler.handle(poll);
        }

        // Событие публикуется при ЛЮБОМ исходе (обработчик найден или нет) —
        // подписчики узнают о закрытии голосования независимо от конкретной
        // реализации обработчика.
        eventPublisher.publish(new PollClosedEvent(
                poll.getScopeId(),
                poll.getId(),
                type.name(),
                poll.getScopeType().name()));
    }
}