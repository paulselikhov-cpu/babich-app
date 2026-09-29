package com.platform.poll;

import com.platform.chat.engines.domainEvent.events.PollClosedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test captor for {@link PollClosedEvent}.
 * <p>
 * Not a {@code @Component}: registered in test context via
 * {@code @TestConfiguration} to avoid leaking into shared ApplicationContext.
 */
public class PollClosedEventCaptor {

    private final CopyOnWriteArrayList<PollClosedEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void onPollClosed(PollClosedEvent event) {
        events.add(event);
    }

    public List<PollClosedEvent> getEvents() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}