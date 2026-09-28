package com.platform.chat.core.modules.webSocket.eventListener;

import com.platform.chat.core.modules.chatRoom.service.OnlineSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketDisconnectListener {

    private final OnlineSessionService onlineSessionService;

    /** Срабатывает и на явное закрытие вкладки, и на протокольный heartbeat-таймаут STOMP. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        onlineSessionService.handleDisconnect(event.getSessionId());
    }
}