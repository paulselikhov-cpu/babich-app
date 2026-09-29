package com.platform.chat.engines.domainEvent.service;

import com.platform.chat.engines.domainEvent.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Единая точка публикации доменных событий.
 * Обёртка над Spring {@link ApplicationEventPublisher}.
 *
 * В MVP работает in-memory (single-instance). При переходе на multi-instance
 * реализация заменяется на Kafka/RabbitMQ без изменения сигнатуры методов.
 */
@Service
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher springEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
    }

    /**
     * Публикует доменное событие. Все подписчики (@TransactionalEventListener)
     * получат его после успешного завершения текущей транзакции (фаза AFTER_COMMIT).
     */
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: type={}, eventId={}, actorId={}, targetId={}",
                event.getType(), event.getEventId(), event.getActorId(), event.getTargetId());
        springEventPublisher.publishEvent(event);
    }
}