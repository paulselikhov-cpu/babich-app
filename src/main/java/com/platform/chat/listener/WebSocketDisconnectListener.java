package com.platform.chat.listener;

import com.platform.chat.service.onlineSessions.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketDisconnectListener {

    private final PresenceService presenceService;

    /** Срабатывает и на явное закрытие вкладки, и на протокольный heartbeat-таймаут STOMP. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presenceService.handleDisconnect(event.getSessionId());
    }
}