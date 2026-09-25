package com.platform.civic.service;

import com.platform.chat.modules.domainEvent.events.CharacterUpdatedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test captor for {@link CharacterUpdatedEvent}.
 * <p>
 * Not a {@code @Component}: registered in test context via
 * {@code @TestConfiguration} to avoid leaking into shared ApplicationContext
 * (по образцу PollClosedEventCaptor).
 */
public class CharacterUpdatedEventCaptor {

    private final CopyOnWriteArrayList<CharacterUpdatedEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void onCharacterUpdated(CharacterUpdatedEvent event) {
        events.add(event);
    }

    public List<CharacterUpdatedEvent> getEvents() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}
